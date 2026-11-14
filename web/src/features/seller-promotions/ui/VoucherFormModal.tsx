// "Tạo / Chỉnh sửa / Nhân bản voucher" (prototype VendorPages.jsx VoucherFormModal) — plan G11.40:
// react-hook-form + zod; mã (Tự tạo), loại Giảm % / Giảm ₫ / Freeship, giá trị + giảm tối đa, đơn tối thiểu,
// số lượng, giới hạn / người, ngân sách, thời gian, phạm vi (tất cả / danh mục / sản phẩm + ngoại trừ).
import { useForm, useWatch } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { zodResolver } from '@/shared/lib/zodResolver';
import {
  Button,
  Checkbox,
  Icon,
  Input,
  Modal,
  Radio,
  Select,
  Skeleton,
  toast,
  type IconName,
} from '@/shared/ui';
import { useSaveVoucher, useVoucher, useVoucherTargets } from '../application/usePromotions';
import { genCode, normalizeCode } from '../domain/promo.rules';
import { voucherSchema, type VoucherValues } from '../domain/promo.schema';
import type { DiscountType, VoucherInput } from '../domain/promo.types';

const KINDS: [DiscountType, IconName][] = [
  ['PERCENTAGE', 'percent'],
  ['FIXED', 'banknote'],
  ['FREESHIP', 'truck'],
];
const EMPTY: VoucherInput = {
  code: '',
  discountType: 'PERCENTAGE',
  discountValue: null,
  maxDiscount: null,
  minOrderValue: null,
  usageLimit: null,
  usagePerUser: 1,
  budget: null,
  validFrom: '',
  validTo: '',
  scope: 'ALL',
  categoryIds: [],
  productIds: [],
  excludedCategoryIds: [],
};
const int = (s: string): number | null => {
  const d = s.replace(/\D/g, '');
  return d ? Number(d) : null;
};
const dateCls =
  'flex-1 min-w-0 h-11 border-[1.5px] border-solid rounded-md px-3 text-base text-strong bg-surface-card outline-none focus:border-primary';
const labCls = 'block font-semibold text-sm text-strong';

export interface VoucherFormTarget {
  /** null = tạo mới. */
  id: number | null;
  /** Nhân bản: mở dữ liệu của voucher `id` nhưng lưu thành voucher mới, mã để trống. */
  duplicate?: boolean;
}

function Form({
  target,
  initial,
  onClose,
}: {
  target: VoucherFormTarget;
  initial: VoucherInput;
  onClose: () => void;
}) {
  const { t } = useT();
  const save = useSaveVoucher();
  const targets = useVoucherTargets();
  const editing = target.id !== null && !target.duplicate;
  const form = useForm<VoucherValues>({
    resolver: zodResolver(voucherSchema),
    defaultValues: target.duplicate ? { ...initial, code: '' } : initial,
  });
  const { errors } = form.formState;
  const v = useWatch({ control: form.control });
  const err = (m?: string) => (m ? t(m) : undefined);
  const set = <K extends keyof VoucherValues>(k: K, val: VoucherValues[K]) =>
    // Kiểu đường dẫn của rhf không suy được cho khóa tổng quát K — giá trị đã đúng kiểu VoucherValues[K]
    form.setValue(k, val as never, {
      shouldDirty: true,
      shouldValidate: form.formState.isSubmitted,
    });

  const submit = form.handleSubmit((vals) =>
    save.mutate(
      { id: editing ? target.id : null, input: vals },
      {
        onSuccess: (r) => {
          toast.success(
            t(editing ? 'seller-promotions.form.saved' : 'seller-promotions.form.created', {
              code: r.code,
            }),
          );
          onClose();
        },
        onError: (e) => {
          if (isApiError(e) && e.code === 'GNE-VCH-4092')
            form.setError('code', { message: 'errors.GNE-VCH-4092' });
          else toast.error(errorMessage(e, t));
        },
      },
    ),
  );

  const toggle = (k: 'categoryIds' | 'productIds', id: number) => {
    const cur = form.getValues(k);
    set(k, cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]);
  };

  return (
    <Modal
      open
      onClose={onClose}
      width={580}
      title={
        <span className="inline-flex items-center gap-2">
          <Icon name="ticket" size={18} className="text-primary" />
          {editing
            ? t('seller-promotions.form.title_edit', { code: initial.code })
            : t('seller-promotions.form.title_new')}
        </span>
      }
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="check" loading={save.isPending} onClick={() => void submit()}>
            {editing ? t('seller-promotions.form.save') : t('seller-promotions.form.create')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-[15px]" onSubmit={(e) => void submit(e)} noValidate>
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <label htmlFor="v-code" className={labCls}>
              {t('seller-promotions.form.code')}
            </label>
            {!editing && (
              <button
                type="button"
                onClick={() => set('code', genCode())}
                className="ml-auto inline-flex items-center gap-1 border-0 bg-transparent text-brand-600 font-semibold text-xs cursor-pointer"
              >
                <Icon name="refresh-cw" size={13} />
                {t('seller-promotions.form.gen')}
              </button>
            )}
          </div>
          <Input
            id="v-code"
            value={v.code ?? ''}
            disabled={editing}
            onChange={(e) => set('code', normalizeCode(e.target.value))}
            placeholder="VD: SUMMER2026"
            error={err(errors.code?.message)}
            hint={errors.code ? undefined : t('seller-promotions.form.code_hint')}
          />
        </div>
        <fieldset className="border-0 p-0 m-0">
          <legend className={labCls}>{t('seller-promotions.form.kind')}</legend>
          <div className="flex gap-2 mt-2 max-sm:flex-col">
            {KINDS.map(([id, icon]) => {
              const on = v.discountType === id;
              return (
                <button
                  key={id}
                  type="button"
                  aria-pressed={on}
                  onClick={() => set('discountType', id)}
                  className={cn(
                    'flex-1 inline-flex items-center justify-center gap-1.5 h-10 rounded-md border-[1.5px] border-solid font-semibold text-[13px] cursor-pointer',
                    on
                      ? 'border-primary bg-primary text-white'
                      : 'border-line bg-surface-card text-body',
                  )}
                >
                  <Icon name={icon} size={15} />
                  {t(`seller-promotions.kind.${id}`)}
                </button>
              );
            })}
          </div>
        </fieldset>
        {v.discountType === 'FREESHIP' ? (
          <div className="flex gap-[9px] items-center bg-mint-50 rounded-md px-[13px] py-[11px] text-sm text-body">
            <Icon name="truck" size={16} className="text-mint-600" />
            {t('seller-promotions.form.freeship_note')}
          </div>
        ) : (
          <div className="flex gap-3.5 max-sm:flex-col">
            <div className="flex-1">
              <Input
                label={t('seller-promotions.form.value')}
                inputMode="numeric"
                iconRight={v.discountType === 'PERCENTAGE' ? 'percent' : 'banknote'}
                value={v.discountValue ?? ''}
                onChange={(e) => set('discountValue', int(e.target.value))}
                error={err(errors.discountValue?.message)}
              />
            </div>
            {v.discountType === 'PERCENTAGE' && (
              <div className="flex-1">
                <Input
                  label={t('seller-promotions.form.cap')}
                  inputMode="numeric"
                  iconRight="banknote"
                  placeholder="50000"
                  value={v.maxDiscount ?? ''}
                  onChange={(e) => set('maxDiscount', int(e.target.value))}
                />
              </div>
            )}
          </div>
        )}
        <div className="flex gap-3.5 max-sm:flex-col">
          <div className="flex-1">
            <Input
              label={t('seller-promotions.form.min')}
              inputMode="numeric"
              iconRight="banknote"
              placeholder="200000"
              value={v.minOrderValue ?? ''}
              onChange={(e) => set('minOrderValue', int(e.target.value))}
            />
          </div>
          <div className="flex-1">
            <Input
              label={t('seller-promotions.form.total')}
              inputMode="numeric"
              placeholder="500"
              value={v.usageLimit ?? ''}
              onChange={(e) => set('usageLimit', int(e.target.value))}
              error={err(errors.usageLimit?.message)}
            />
          </div>
        </div>
        <div className="flex gap-3.5 max-sm:flex-col">
          <div className="w-[150px] max-sm:w-full">
            <Input
              label={t('seller-promotions.form.per_user')}
              inputMode="numeric"
              value={v.usagePerUser ?? ''}
              onChange={(e) => set('usagePerUser', int(e.target.value))}
            />
          </div>
          <div className="flex-1">
            <Input
              label={t('seller-promotions.form.budget')}
              inputMode="numeric"
              iconRight="banknote"
              placeholder="5000000"
              value={v.budget ?? ''}
              onChange={(e) => set('budget', int(e.target.value))}
            />
          </div>
        </div>
        <fieldset className="border-0 p-0 m-0">
          <legend className={labCls}>{t('seller-promotions.form.period')}</legend>
          <div className="flex gap-2.5 items-center mt-1.5">
            <input
              type="date"
              aria-label={t('seller-promotions.form.from')}
              value={v.validFrom ?? ''}
              onChange={(e) => set('validFrom', e.target.value)}
              className={cn(dateCls, errors.validFrom ? 'border-red-500' : 'border-line')}
            />
            <Icon name="arrow-right" size={16} className="text-subtle flex-none" />
            <input
              type="date"
              aria-label={t('seller-promotions.form.to')}
              value={v.validTo ?? ''}
              min={v.validFrom || undefined}
              onChange={(e) => set('validTo', e.target.value)}
              className={cn(dateCls, errors.validTo ? 'border-red-500' : 'border-line')}
            />
          </div>
          {(errors.validFrom || errors.validTo) && (
            <p className="m-0 mt-1.5 flex items-center gap-1 text-xs text-red-600" role="alert">
              <Icon name="circle-alert" size={13} />
              {err(errors.validTo?.message ?? errors.validFrom?.message)}
            </p>
          )}
        </fieldset>
        <fieldset className="border-0 p-0 m-0">
          <legend className={labCls}>{t('seller-promotions.form.scope')}</legend>
          {!targets.data ? (
            <Skeleton lines={3} className="mt-2" />
          ) : (
            <div className="flex flex-col gap-1 mt-2">
              <Radio name="vscope" checked={v.scope === 'ALL'} onChange={() => set('scope', 'ALL')}>
                {t('seller-promotions.form.scope_all')}
              </Radio>
              <Radio
                name="vscope"
                checked={v.scope === 'CATEGORY'}
                onChange={() => set('scope', 'CATEGORY')}
              >
                {t('seller-promotions.form.scope_cat')}
              </Radio>
              {v.scope === 'CATEGORY' && (
                <div className="flex flex-col gap-0.5 px-3 py-2 border border-solid border-line-subtle rounded-md">
                  {targets.data.categories.map((c) => (
                    <Checkbox
                      key={c.id}
                      checked={(v.categoryIds ?? []).includes(c.id)}
                      onChange={() => toggle('categoryIds', c.id)}
                      className="text-sm"
                    >
                      {c.name}
                    </Checkbox>
                  ))}
                </div>
              )}
              <Radio
                name="vscope"
                checked={v.scope === 'PRODUCT'}
                onChange={() => set('scope', 'PRODUCT')}
              >
                {t('seller-promotions.form.scope_product')}
              </Radio>
              {v.scope === 'PRODUCT' && (
                <div className="flex flex-col gap-0.5 px-3 py-2 border border-solid border-line-subtle rounded-md">
                  {targets.data.products.map((p) => (
                    <Checkbox
                      key={p.id}
                      checked={(v.productIds ?? []).includes(p.id)}
                      onChange={() => toggle('productIds', p.id)}
                      className="text-sm"
                    >
                      {p.name}
                    </Checkbox>
                  ))}
                </div>
              )}
              {(errors.categoryIds || errors.productIds) && (
                <p className="m-0 text-xs text-red-600" role="alert">
                  {err(errors.categoryIds?.message ?? errors.productIds?.message)}
                </p>
              )}
              {v.scope !== 'PRODUCT' && (
                <Select
                  label={t('seller-promotions.form.exclude')}
                  className="mt-2"
                  value={v.excludedCategoryIds?.[0] ?? 0}
                  onChange={(id) => set('excludedCategoryIds', id ? [id] : [])}
                  options={[
                    { value: 0, label: t('seller-promotions.form.exclude_none') },
                    ...targets.data.categories.map((c) => ({ value: c.id, label: c.name })),
                  ]}
                />
              )}
            </div>
          )}
        </fieldset>
      </form>
    </Modal>
  );
}

export function VoucherFormModal({
  target,
  onClose,
}: {
  target: VoucherFormTarget;
  onClose: () => void;
}) {
  const q = useVoucher(target.id);
  if (target.id !== null && !q.data) return null;
  return <Form target={target} initial={q.data ?? EMPTY} onClose={onClose} />;
}
