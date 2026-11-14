// "Tạo / Chỉnh sửa / Nhân bản voucher" (prototype VendorPages.jsx VoucherFormModal) — plan G11.40:
// react-hook-form + zod; mã (Tự tạo), loại Giảm % / Giảm ₫ / Freeship, giá trị + giảm tối đa, đơn tối thiểu,
// số lượng, giới hạn / người, ngân sách, thời gian, phạm vi (tất cả / danh mục / sản phẩm + ngoại trừ).
// `VoucherEditor` là phần form dùng chung: Seller (voucher shop) và Admin (voucher sàn — plan G12.33,
// prototype admin/Vouchers.jsx PlatformVoucherModal: thêm tên chương trình, nhà tài trợ, tỷ lệ sàn
// tài trợ; phạm vi chỉ Toàn sàn / Danh mục). Nơi gọi cấp dữ liệu và hàm lưu.
import { useMemo } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { isoDateVN } from '@/shared/lib/format';
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
import { voucherSchemaFor, type VoucherFormMode, type VoucherValues } from '../domain/promo.schema';
import type { DiscountType, VoucherInput } from '../domain/promo.types';

const KINDS: [DiscountType, IconName][] = [
  ['PERCENTAGE', 'percent'],
  ['FIXED', 'banknote'],
  ['FREESHIP', 'truck'],
];

/** Giá trị trống của form (voucher shop bỏ qua 3 trường cuối). */
export const EMPTY_VOUCHER: VoucherValues = {
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
  title: '',
  fundedBy: 'PLATFORM',
  platformSharePercent: null,
};

const int = (s: string): number | null => {
  const d = s.replace(/\D/g, '');
  return d ? Number(d) : null;
};
const dateCls =
  'flex-1 min-w-0 h-11 border-[1.5px] border-solid rounded-md px-3 text-base text-strong bg-surface-card outline-none focus:border-primary';
const labCls = 'block font-semibold text-sm text-strong';

export interface VoucherEditorProps {
  mode: VoucherFormMode;
  /** true = sửa (mã khóa lại); false = tạo mới / nhân bản. */
  editing: boolean;
  initial: VoucherValues;
  /** Danh mục chọn được cho phạm vi / ngoại trừ; undefined = đang tải. */
  categories: { id: number; name: string }[] | undefined;
  /** Sản phẩm của shop (chỉ chế độ shop). */
  products?: { id: number; name: string }[];
  saving: boolean;
  /** Lưu; ném ApiError GNE-VCH-4092 thì form báo trùng mã dưới ô. Trả mã đã lưu để báo thành công. */
  onSave: (v: VoucherValues) => Promise<{ code: string }>;
  onClose: () => void;
}

export function VoucherEditor({
  mode,
  editing,
  initial,
  categories,
  products = [],
  saving,
  onSave,
  onClose,
}: VoucherEditorProps) {
  const { t } = useT();
  const platform = mode === 'platform';
  const schema = useMemo(
    () => voucherSchemaFor(mode, { isNew: !editing, today: isoDateVN() }),
    [mode, editing],
  );
  const form = useForm<VoucherValues>({ resolver: zodResolver(schema), defaultValues: initial });
  const { errors } = form.formState;
  const v = useWatch({ control: form.control });
  const err = (m?: string) => (m ? t(m) : undefined);
  const set = <K extends keyof VoucherValues>(k: K, val: VoucherValues[K]) =>
    // Kiểu đường dẫn của rhf không suy được cho khóa tổng quát K — giá trị đã đúng kiểu VoucherValues[K]
    form.setValue(k, val as never, {
      shouldDirty: true,
      shouldValidate: form.formState.isSubmitted,
    });

  const submit = form.handleSubmit(async (vals) => {
    try {
      const r = await onSave(vals);
      toast.success(
        t(editing ? 'seller-promotions.form.saved' : 'seller-promotions.form.created', {
          code: r.code,
        }),
      );
      onClose();
    } catch (e) {
      if (isApiError(e) && e.code === 'GNE-VCH-4092')
        form.setError('code', { message: 'errors.GNE-VCH-4092' });
      else toast.error(errorMessage(e, t));
    }
  });

  const toggle = (k: 'categoryIds' | 'productIds', id: number) => {
    const cur = form.getValues(k);
    set(k, cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id]);
  };

  const titleKey = editing
    ? 'seller-promotions.form.title_edit'
    : platform
      ? 'seller-promotions.form.title_new_platform'
      : 'seller-promotions.form.title_new';

  return (
    <Modal
      open
      onClose={onClose}
      width={580}
      dismissible={!form.formState.isDirty}
      title={
        <span className="inline-flex items-center gap-2">
          <Icon name="ticket" size={18} className="text-primary" />
          {t(titleKey, { code: initial.code })}
        </span>
      }
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            {t('common.action.cancel')}
          </Button>
          <Button iconLeft="check" loading={saving} onClick={() => void submit()}>
            {editing ? t('seller-promotions.form.save') : t('seller-promotions.form.create')}
          </Button>
        </>
      }
    >
      <form className="flex flex-col gap-[15px]" onSubmit={(e) => void submit(e)} noValidate>
        {platform && (
          <Input
            label={t('seller-promotions.form.title')}
            placeholder={t('seller-promotions.form.title_placeholder')}
            required
            maxLength={100}
            value={v.title ?? ''}
            onChange={(e) => set('title', e.target.value)}
            error={err(errors.title?.message)}
          />
        )}
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
            placeholder={platform ? 'VD: GENIE50' : 'VD: SUMMER2026'}
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
                  placeholder={platform ? '100000' : '50000'}
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
              placeholder={platform ? '500000' : '200000'}
              value={v.minOrderValue ?? ''}
              onChange={(e) => set('minOrderValue', int(e.target.value))}
            />
          </div>
          <div className="flex-1">
            <Input
              label={t('seller-promotions.form.total')}
              inputMode="numeric"
              placeholder={platform ? '10000' : '500'}
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
              placeholder={platform ? '500000000' : '5000000'}
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
        {platform && (
          <div className="flex gap-3.5 items-end max-sm:flex-col max-sm:items-stretch">
            <Select<'PLATFORM' | 'CO_SPONSOR'>
              label={t('seller-promotions.form.sponsor')}
              className="flex-1"
              value={v.fundedBy ?? 'PLATFORM'}
              onChange={(x) => set('fundedBy', x)}
              options={[
                { value: 'PLATFORM', label: t('seller-promotions.form.sponsor_platform') },
                { value: 'CO_SPONSOR', label: t('seller-promotions.form.sponsor_co') },
              ]}
            />
            {v.fundedBy === 'CO_SPONSOR' && (
              <Input
                label={t('seller-promotions.form.share')}
                wrapperClassName="flex-1"
                inputMode="numeric"
                iconRight="percent"
                placeholder="50"
                value={v.platformSharePercent ?? ''}
                onChange={(e) => set('platformSharePercent', int(e.target.value.slice(0, 3)))}
                error={err(errors.platformSharePercent?.message)}
              />
            )}
          </div>
        )}
        <fieldset className="border-0 p-0 m-0">
          <legend className={labCls}>{t('seller-promotions.form.scope')}</legend>
          {!categories ? (
            <Skeleton lines={3} className="mt-2" />
          ) : (
            <div className="flex flex-col gap-1 mt-2">
              <Radio name="vscope" checked={v.scope === 'ALL'} onChange={() => set('scope', 'ALL')}>
                {t(
                  platform
                    ? 'seller-promotions.form.scope_platform'
                    : 'seller-promotions.form.scope_all',
                )}
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
                  {categories.map((c) => (
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
              {!platform && (
                <Radio
                  name="vscope"
                  checked={v.scope === 'PRODUCT'}
                  onChange={() => set('scope', 'PRODUCT')}
                >
                  {t('seller-promotions.form.scope_product')}
                </Radio>
              )}
              {!platform && v.scope === 'PRODUCT' && (
                <div className="flex flex-col gap-0.5 px-3 py-2 border border-solid border-line-subtle rounded-md">
                  {products.map((p) => (
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
              {(errors.categoryIds || errors.productIds || errors.scope) && (
                <p className="m-0 text-xs text-red-600" role="alert">
                  {err(
                    errors.categoryIds?.message ??
                      errors.productIds?.message ??
                      errors.scope?.message,
                  )}
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
                    ...categories.map((c) => ({ value: c.id, label: c.name })),
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

export interface VoucherFormTarget {
  /** null = tạo mới. */
  id: number | null;
  /** Nhân bản: mở dữ liệu của voucher `id` nhưng lưu thành voucher mới, mã để trống. */
  duplicate?: boolean;
}

const toValues = (v: VoucherInput): VoucherValues => ({
  ...EMPTY_VOUCHER,
  ...v,
});

/** Form voucher shop (Seller Center) — dữ liệu và lưu qua hook của seller-promotions. */
export function VoucherFormModal({
  target,
  onClose,
}: {
  target: VoucherFormTarget;
  onClose: () => void;
}) {
  const q = useVoucher(target.id);
  const targets = useVoucherTargets();
  const save = useSaveVoucher();
  if (target.id !== null && !q.data) return null;
  const editing = target.id !== null && !target.duplicate;
  const base = q.data ? toValues(q.data) : EMPTY_VOUCHER;
  return (
    <VoucherEditor
      mode="shop"
      editing={editing}
      initial={target.duplicate ? { ...base, code: '' } : base}
      categories={targets.data?.categories}
      products={targets.data?.products}
      saving={save.isPending}
      onSave={(vals) => {
        const { title: _t, fundedBy: _f, platformSharePercent: _p, ...input } = vals;
        return save.mutateAsync({ id: editing ? target.id : null, input });
      }}
      onClose={onClose}
    />
  );
}
