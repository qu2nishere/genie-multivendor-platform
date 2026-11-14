// Thêm / Sửa danh mục (prototype Catalog.jsx CatModal) — plan G12.22: tên, slug (tự sinh + nút Auto),
// danh mục cha (gốc / cấp 2 — cây 3 cấp, chỉ khi thêm), biểu tượng, hoa hồng mặc định, hiển thị;
// Xóa chỉ khi danh mục rỗng (hỏi lại). react-hook-form + zod.
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { slugify } from '@/shared/lib/slug';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Button,
  ConfirmDialog,
  Icon,
  Input,
  Modal,
  Select,
  Switch,
  toast,
  toIconName,
} from '@/shared/ui';
import { useCreateCategory, useDeleteCategory, useUpdateCategory } from '../application/useCatalog';
import { CAT_ICONS, canDeleteRoot, parentOptions } from '../domain/catalog.rules';
import { categorySchema, type CategoryValues } from '../domain/catalog.schema';
import type { RootCategory } from '../domain/catalog.types';

const labelCls = 'block text-sm font-semibold text-strong mb-1.5';

export function CatModal({
  open,
  editing,
  roots,
  onClose,
  onDeleted,
}: {
  open: boolean;
  /** null = thêm mới. */
  editing: RootCategory | null;
  roots: RootCategory[];
  onClose: () => void;
  onDeleted?: () => void;
}) {
  const { t } = useT();
  const create = useCreateCategory();
  const update = useUpdateCategory();
  const del = useDeleteCategory();
  const [confirmDel, setConfirmDel] = useState(false);
  const blank: CategoryValues = {
    name: '',
    slug: '',
    parentId: null,
    icon: 'package',
    commissionRate: 5,
    active: true,
  };
  const form = useForm<CategoryValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: blank,
  });
  useEffect(() => {
    if (!open) return;
    form.reset(
      editing
        ? {
            name: editing.name,
            slug: editing.slug,
            parentId: null,
            icon: editing.icon,
            commissionRate: editing.commissionRate,
            active: editing.active,
          }
        : blank,
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editing]);
  const v = form.watch();
  const err = form.formState.errors;
  const pending = create.isPending || update.isPending;

  const submit = form.handleSubmit((values) => {
    const done = {
      onSuccess: () => {
        toast.success(t('admin-catalog.cat.saved'));
        onClose();
      },
      onError: (e: unknown) => toast.error(errorMessage(e, t)),
    };
    if (editing) {
      const { parentId: _p, ...patch } = values;
      update.mutate({ id: editing.id, patch }, done);
    } else create.mutate(values, done);
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      width={520}
      dismissible={!form.formState.isDirty}
      title={
        <span className="inline-flex items-center gap-2">
          <Icon name="boxes" size={18} className="text-blue-600" />
          {editing
            ? t('admin-catalog.cat.edit_title', { name: editing.name })
            : t('admin-catalog.cat.add_title')}
        </span>
      }
      footer={
        <>
          {editing && (
            <Button
              variant="ghost"
              iconLeft="trash-2"
              className="mr-auto !text-red-600"
              disabled={!canDeleteRoot(editing)}
              title={canDeleteRoot(editing) ? undefined : t('admin-catalog.cat.delete_hint')}
              onClick={() => setConfirmDel(true)}
            >
              {t('admin-catalog.cat.delete')}
            </Button>
          )}
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="check" loading={pending} onClick={() => void submit()}>
            {t('admin-catalog.cat.save')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-[15px]" onSubmit={(e) => void submit(e)} noValidate>
        <Input
          label={t('admin-catalog.cat.name')}
          placeholder={t('admin-catalog.cat.name_placeholder')}
          required
          maxLength={100}
          error={err.name?.message ? t(err.name.message) : undefined}
          {...form.register('name', {
            onChange: (e: { target: { value: string } }) => {
              if (!editing) form.setValue('slug', slugify(e.target.value));
            },
          })}
        />
        <div>
          <span className={labelCls}>{t('admin-catalog.cat.slug')}</span>
          <div className="flex gap-2">
            <Input
              aria-label={t('admin-catalog.cat.slug')}
              wrapperClassName="flex-1"
              error={err.slug?.message ? t(err.slug.message) : undefined}
              {...form.register('slug')}
            />
            <Button
              variant="secondary"
              iconLeft="refresh-cw"
              onClick={() => form.setValue('slug', slugify(v.name), { shouldValidate: true })}
            >
              Auto
            </Button>
          </div>
        </div>
        {!editing && (
          <Select<number>
            label={t('admin-catalog.cat.parent')}
            value={v.parentId ?? 0}
            onChange={(id) => form.setValue('parentId', id || null)}
            options={[
              { value: 0, label: t('admin-catalog.cat.root') },
              ...parentOptions(roots).map((o) => ({ value: o.id, label: o.label })),
            ]}
          />
        )}
        <fieldset className="border-0 p-0 m-0">
          <legend className={labelCls}>{t('admin-catalog.cat.icon')}</legend>
          <div className="flex flex-wrap gap-2">
            {CAT_ICONS.map((ic) => {
              const on = v.icon === ic;
              return (
                <button
                  key={ic}
                  type="button"
                  aria-label={ic}
                  aria-pressed={on}
                  onClick={() => form.setValue('icon', ic, { shouldDirty: true })}
                  className={cn(
                    'w-[42px] h-[42px] rounded-md border-[1.5px] border-solid cursor-pointer grid place-items-center focus-visible:outline-none focus-visible:shadow-focus',
                    on
                      ? 'border-blue-500 bg-blue-50 text-blue-600'
                      : 'border-line bg-surface-card text-muted',
                  )}
                >
                  <Icon name={toIconName(ic, 'package')} size={19} />
                </button>
              );
            })}
          </div>
        </fieldset>
        <div className="flex gap-[18px] items-end flex-wrap">
          <Input
            label={t('admin-catalog.cat.rate')}
            wrapperClassName="w-[160px]"
            inputMode="decimal"
            iconRight="percent"
            error={err.commissionRate?.message ? t(err.commissionRate.message) : undefined}
            {...form.register('commissionRate', { valueAsNumber: true })}
          />
          <label className="flex items-center gap-2.5 mb-[11px] text-sm text-body">
            {t('admin-catalog.cat.visible')}
            <Switch
              checked={v.active}
              onChange={() => form.setValue('active', !v.active, { shouldDirty: true })}
            />
          </label>
        </div>
      </form>
      <ConfirmDialog
        open={confirmDel}
        tone="danger"
        title={t('admin-catalog.cat.delete_title')}
        message={t('admin-catalog.cat.delete_desc')}
        confirmLabel={t('admin-catalog.cat.delete_confirm')}
        loading={del.isPending}
        onCancel={() => setConfirmDel(false)}
        onConfirm={() =>
          editing &&
          del.mutate(editing.id, {
            onSuccess: () => {
              toast.success(t('admin-catalog.cat.deleted'));
              setConfirmDel(false);
              onClose();
              onDeleted?.();
            },
            onError: (e) => toast.error(errorMessage(e, t)),
          })
        }
      />
    </Modal>
  );
}
