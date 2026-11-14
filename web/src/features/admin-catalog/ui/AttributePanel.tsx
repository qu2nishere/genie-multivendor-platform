// Bộ thuộc tính theo ngành (prototype Catalog.jsx AttributePanel; UC-CAT-15) — plan G12.23, route
// /catalog/:id/attributes: bảng Mã / Tên / Kiểu / Giá trị–Đơn vị / Bắt buộc / Lọc được / Trục biến thể /
// Trạng thái; Thêm / Sửa (zod: mã ^[a-z][a-z0-9_]*$, ENUM cần giá trị), Ngừng dùng / Dùng lại.
// Thêm thuộc tính bắt buộc → thông báo shop có 14 ngày bổ sung cho sản phẩm cũ (API grace_period_days).
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Badge,
  Button,
  Checkbox,
  EmptyState,
  Icon,
  Input,
  Modal,
  Select,
  Skeleton,
  toast,
} from '@/shared/ui';
import {
  useAttributes,
  useCategoryTree,
  useSaveAttribute,
  useToggleAttribute,
} from '../application/useCatalog';
import { ATTR_TYPES, duplicateCode, needsValues, parseValues } from '../domain/catalog.rules';
import { attributeSchema, type AttributeValues } from '../domain/catalog.schema';
import type { CategoryAttribute } from '../domain/catalog.types';

const th = 'px-4 py-2.5 whitespace-nowrap font-semibold text-xs text-muted text-left bg-gray-25';
const td = 'px-4 py-3 whitespace-nowrap';

const blank: AttributeValues = {
  code: '',
  name: '',
  dataType: 'TEXT',
  valuesText: '',
  unit: '',
  required: false,
  filterable: false,
  variantAxis: false,
};

function AttrModal({
  categoryId,
  open,
  editing,
  rows,
  onClose,
}: {
  categoryId: number;
  open: boolean;
  editing: CategoryAttribute | null;
  rows: CategoryAttribute[];
  onClose: () => void;
}) {
  const { t } = useT();
  const save = useSaveAttribute();
  const form = useForm<AttributeValues>({
    resolver: zodResolver(attributeSchema),
    defaultValues: blank,
  });
  useEffect(() => {
    if (!open) return;
    form.reset(
      editing
        ? {
            code: editing.code,
            name: editing.name,
            dataType: editing.dataType,
            valuesText: editing.values.join(', '),
            unit: editing.unit ?? '',
            required: editing.required,
            filterable: editing.filterable,
            variantAxis: editing.variantAxis,
          }
        : blank,
    );
  }, [open, editing, form]);
  const v = form.watch();
  const err = form.formState.errors;
  const msg = (k?: string) => (k ? t(k) : undefined);
  const submit = form.handleSubmit((x) => {
    if (duplicateCode(rows, x.code, editing?.code ?? null)) {
      form.setError('code', { message: 'admin-catalog.attr.err.duplicate' });
      return;
    }
    const attr: CategoryAttribute = {
      code: x.code,
      name: x.name,
      dataType: x.dataType,
      values: needsValues(x.dataType) ? parseValues(x.valuesText) : [],
      unit: x.dataType === 'NUMBER' && x.unit ? x.unit : null,
      required: x.required,
      filterable: x.filterable,
      variantAxis: x.variantAxis,
      active: editing?.active ?? true,
    };
    save.mutate(
      { categoryId, attr, editing: editing?.code ?? null },
      {
        onSuccess: (r) => {
          toast.success(
            r.gracePeriodDays
              ? t('admin-catalog.attr.saved_grace', { days: r.gracePeriodDays })
              : t('admin-catalog.attr.saved'),
          );
          onClose();
        },
        onError: (e) => toast.error(errorMessage(e, t)),
      },
    );
  });
  const flags: ['required' | 'filterable' | 'variantAxis', string][] = [
    ['required', t('admin-catalog.attr.required')],
    ['filterable', t('admin-catalog.attr.filterable_long')],
    ['variantAxis', t('admin-catalog.attr.axis')],
  ];
  return (
    <Modal
      open={open}
      onClose={onClose}
      width={520}
      dismissible={!form.formState.isDirty}
      title={editing ? t('admin-catalog.attr.edit_title') : t('admin-catalog.attr.add_title')}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="check" loading={save.isPending} onClick={() => void submit()}>
            {t('admin-catalog.attr.save')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-3.5" onSubmit={(e) => void submit(e)} noValidate>
        <div className="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
          <Input
            label={t('admin-catalog.attr.code')}
            placeholder="warranty"
            readOnly={!!editing}
            error={msg(err.code?.message)}
            {...form.register('code')}
          />
          <Input
            label={t('admin-catalog.attr.name')}
            placeholder={t('admin-catalog.attr.name_placeholder')}
            error={msg(err.name?.message)}
            {...form.register('name')}
          />
        </div>
        <div className="grid grid-cols-2 gap-3 max-sm:grid-cols-1">
          <Select
            label={t('admin-catalog.attr.type')}
            value={v.dataType}
            onChange={(x) => form.setValue('dataType', x, { shouldDirty: true })}
            options={ATTR_TYPES.map((x) => ({ value: x, label: x }))}
          />
          {v.dataType === 'NUMBER' && (
            <Input
              label={t('admin-catalog.attr.unit')}
              placeholder={t('admin-catalog.attr.unit_placeholder')}
              {...form.register('unit')}
            />
          )}
        </div>
        {needsValues(v.dataType) && (
          <Input
            label={t('admin-catalog.attr.values')}
            placeholder={t('admin-catalog.attr.values_placeholder')}
            error={msg(err.valuesText?.message)}
            {...form.register('valuesText')}
          />
        )}
        <div className="flex gap-[18px] flex-wrap">
          {flags.map(([k, label]) => (
            <Checkbox
              key={k}
              checked={v[k]}
              onChange={() => form.setValue(k, !v[k], { shouldDirty: true })}
              className="text-sm"
            >
              {label}
            </Checkbox>
          ))}
        </div>
      </form>
    </Modal>
  );
}

export default function AttributePanel() {
  const { t } = useT();
  const { id } = useParams();
  const categoryId = Number(id);
  const tree = useCategoryTree();
  const q = useAttributes(categoryId);
  const toggle = useToggleAttribute();
  const [modal, setModal] = useState<{ editing: CategoryAttribute | null } | null>(null);
  const cat = tree.data?.roots.find((r) => r.id === categoryId);
  const back = (
    <Link
      to={`/catalog?id=${categoryId}`}
      className="inline-flex items-center gap-1.5 text-sm text-muted no-underline hover:underline mb-3"
    >
      <Icon name="chevron-left" size={16} />
      {t('admin-catalog.back')}
    </Link>
  );
  if (tree.data && !cat)
    return (
      <div>
        {back}
        <EmptyState icon="boxes" title={t('admin-catalog.not_found')} />
      </div>
    );
  const rows = q.data ?? [];
  const yes = (b: boolean, label: string) =>
    b ? (
      <Icon name="check" size={15} className="text-mint-600" aria-label={label} />
    ) : (
      <span className="text-disabled" aria-hidden="true">
        —
      </span>
    );
  return (
    <div className="max-w-[1180px]">
      {back}
      <div className="flex items-center gap-3 mb-3.5 flex-wrap">
        <div>
          <h2 className="m-0 text-xl font-bold text-strong">
            {t('admin-catalog.attr.title', { name: cat?.name ?? '' })}
          </h2>
          <p className="m-0 mt-[3px] text-sm text-muted">{t('admin-catalog.attr.desc')}</p>
        </div>
        <Button iconLeft="plus" className="ml-auto" onClick={() => setModal({ editing: null })}>
          {t('admin-catalog.attr.add')}
        </Button>
      </div>
      <div className="bg-surface-card rounded-lg shadow-sm overflow-x-auto">
        {q.isPending ? (
          <Skeleton lines={4} className="m-5" />
        ) : (
          <table className="w-full min-w-[820px] border-collapse">
            <thead>
              <tr>
                <th className={th}>{t('admin-catalog.attr.code')}</th>
                <th className={th}>{t('admin-catalog.attr.name')}</th>
                <th className={th}>{t('admin-catalog.attr.type')}</th>
                <th className={th}>{t('admin-catalog.attr.values_unit')}</th>
                <th className={th}>{t('admin-catalog.attr.required')}</th>
                <th className={th}>{t('admin-catalog.attr.filterable')}</th>
                <th className={th}>{t('admin-catalog.attr.axis')}</th>
                <th className={th}>{t('admin-catalog.attr.status')}</th>
                <th className={cn(th, 'text-right')}>{t('admin-catalog.attr.action')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr>
                  <td colSpan={9} className="p-9 text-center text-base text-muted">
                    {t('admin-catalog.attr.empty')}
                  </td>
                </tr>
              )}
              {rows.map((r) => (
                <tr
                  key={r.code}
                  className={cn(
                    'border-0 border-t border-solid border-line-subtle text-[13px]',
                    r.active ? 'text-body' : 'text-subtle',
                  )}
                >
                  <td className={cn(td, 'font-mono')}>{r.code}</td>
                  <td className={td}>{r.name}</td>
                  <td className={td}>
                    <Badge variant="neutral">{r.dataType}</Badge>
                  </td>
                  <td className={cn(td, 'max-w-[200px] overflow-hidden text-ellipsis')}>
                    {r.values.join(', ') || r.unit || '—'}
                  </td>
                  <td className={td}>{yes(r.required, t('admin-catalog.attr.required'))}</td>
                  <td className={td}>{yes(r.filterable, t('admin-catalog.attr.filterable'))}</td>
                  <td className={td}>{yes(r.variantAxis, t('admin-catalog.attr.axis'))}</td>
                  <td className={td}>
                    <Badge variant={r.active ? 'success' : 'neutral'}>
                      {t(r.active ? 'admin-catalog.attr.active' : 'admin-catalog.attr.inactive')}
                    </Badge>
                  </td>
                  <td className={cn(td, 'text-right')}>
                    <div className="flex gap-1 justify-end">
                      <Button
                        size="sm"
                        variant="ghost"
                        iconLeft="pencil"
                        onClick={() => setModal({ editing: r })}
                      >
                        {t('admin-catalog.attr.edit')}
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() =>
                          toggle.mutate(
                            { categoryId, code: r.code, active: !r.active },
                            { onError: (e) => toast.error(errorMessage(e, t)) },
                          )
                        }
                      >
                        {t(
                          r.active
                            ? 'admin-catalog.attr.deactivate'
                            : 'admin-catalog.attr.activate',
                        )}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      <AttrModal
        categoryId={categoryId}
        open={!!modal}
        editing={modal?.editing ?? null}
        rows={rows}
        onClose={() => setModal(null)}
      />
    </div>
  );
}
