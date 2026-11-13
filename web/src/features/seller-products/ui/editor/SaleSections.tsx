// Khối Phân loại & Giá (G11.28: tối đa 2 nhóm, tên nhóm chọn / tự đặt, giá trị dạng thẻ, ảnh theo phân loại,
// áp dụng nhanh, bảng giá / tồn / SKU theo tổ hợp, kiểm giá > 0, ≤ 50 phân loại) và Vận chuyển (G11.29: cân
// nặng, kích thước, phí ship, đặt trước 7–30 ngày) — prototype VariantSection, GroupNameCombo, TagInput,
// ShippingInfo.
import { useId, useRef, useState } from 'react';
import { useWatch } from 'react-hook-form';
import { errorMessage } from '@/shared/api/errors';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { useClickOutside } from '@/shared/hooks/useClickOutside';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Badge, Button, Icon, Input, Radio, Select, Switch, toast } from '@/shared/ui';
import {
  GROUP_NAMES,
  MAX_GROUPS,
  MAX_VARIANTS,
  PRE_ORDER_MAX,
  PRE_ORDER_MIN,
  invalidPrice,
  skuPreview,
  syncVariants,
} from '../../domain/product.rules';
import type { OptionGroup, VariantRow } from '../../domain/product.types';
import { errCls, hintCls, labCls, SectionCard, toInt, type SectionProps } from './editorKit';

function GroupNameCombo({
  value,
  onChange,
  disabled,
  label,
}: {
  value: string;
  onChange: (v: string) => void;
  disabled: boolean;
  label: string;
}) {
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const box = useRef<HTMLDivElement>(null);
  const listId = useId();
  useClickOutside(box, () => setOpen(false), open);
  const matches = GROUP_NAMES.filter((b) => b.toLowerCase().includes(value.trim().toLowerCase()));
  const exact = GROUP_NAMES.some((b) => b.toLowerCase() === value.trim().toLowerCase());
  return (
    <div ref={box} className="relative w-[200px] max-sm:w-full">
      <input
        value={value}
        disabled={disabled}
        aria-label={label}
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => e.key === 'Escape' && setOpen(false)}
        placeholder={t('seller-products.variants.group_placeholder')}
        className="w-full h-10 border-[1.5px] border-solid border-line rounded-md px-3 text-sm text-strong bg-surface-card outline-none focus:border-primary"
      />
      {open && !disabled && (
        <ul
          id={listId}
          role="listbox"
          className="absolute top-[calc(100%+4px)] left-0 right-0 m-0 p-0 list-none bg-surface-card border border-solid border-line rounded-md shadow-lg z-dropdown max-h-[220px] overflow-y-auto"
        >
          {matches.map((b) => (
            <li key={b} role="option" aria-selected={value === b}>
              <button
                type="button"
                onClick={() => {
                  onChange(b);
                  setOpen(false);
                }}
                className="flex items-center w-full border-0 bg-transparent px-3 py-2 cursor-pointer text-left text-sm text-body hover:bg-gray-50"
              >
                <span className="flex-1">{b}</span>
                {value === b && <Icon name="check" size={14} className="text-brand-600" />}
              </button>
            </li>
          ))}
          {value.trim() && !exact && (
            <li role="option" aria-selected>
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="flex items-center gap-1.5 w-full border-0 border-t border-solid border-line-subtle bg-transparent px-3 py-2 cursor-pointer text-left text-[13px] font-semibold text-brand-600"
              >
                <Icon name="pencil" size={13} />
                {t('seller-products.variants.custom_name', { name: value.trim() })}
              </button>
            </li>
          )}
        </ul>
      )}
    </div>
  );
}

function TagInput({
  values,
  onAdd,
  onRemove,
  disabled,
  label,
}: {
  values: string[];
  onAdd: (v: string) => void;
  onRemove: (v: string) => void;
  disabled: boolean;
  label: string;
}) {
  const { t } = useT();
  const [v, setV] = useState('');
  const add = () => {
    const x = v.trim();
    if (x && !values.includes(x)) onAdd(x);
    setV('');
  };
  return (
    <div className="flex flex-wrap gap-2 items-center">
      {values.map((val) => (
        <span
          key={val}
          className="inline-flex items-center gap-1.5 bg-gray-100 rounded-full px-2.5 py-1 text-[13px] font-semibold text-body"
        >
          {val}
          {!disabled && (
            <button
              type="button"
              aria-label={t('seller-products.variants.remove_value', { value: val })}
              onClick={() => onRemove(val)}
              className="border-0 bg-transparent cursor-pointer grid place-items-center text-subtle hover:text-red-600 p-0"
            >
              <Icon name="x" size={13} />
            </button>
          )}
        </span>
      ))}
      {!disabled && (
        <input
          value={v}
          aria-label={label}
          placeholder={t('seller-products.variants.value_placeholder')}
          onChange={(e) => setV(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ',') {
              e.preventDefault();
              add();
            }
          }}
          onBlur={add}
          maxLength={30}
          className="min-w-[110px] h-[30px] border-[1.5px] border-dashed border-line-strong rounded-full px-3 text-[13px] outline-none bg-transparent focus:border-primary"
        />
      )}
    </div>
  );
}

export function VariantSection({ form, readOnly, issues }: SectionProps) {
  const { t } = useT();
  const groups = useWatch({ control: form.control, name: 'groups' });
  const variants = useWatch({ control: form.control, name: 'variants' });
  const imgGroup = useWatch({ control: form.control, name: 'variantImageGroup' });
  const [bulk, setBulk] = useState<{ price: number | null; stock: number | null }>({
    price: null,
    stock: null,
  });
  const [uploading, setUploading] = useState<string | null>(null);

  const setGroups = (next: OptionGroup[]) => {
    form.setValue('groups', next, { shouldDirty: true });
    form.setValue('variants', syncVariants(next, form.getValues('variants')), {
      shouldDirty: true,
    });
    const names = next.filter((g) => g.values.length).map((g) => g.name);
    if (imgGroup && !names.includes(imgGroup)) form.setValue('variantImageGroup', names[0] ?? null);
    if (!imgGroup && names[0]) form.setValue('variantImageGroup', names[0]);
  };
  const setGroup = (i: number, patch: Partial<OptionGroup>) =>
    setGroups(groups.map((g, k) => (k === i ? { ...g, ...patch } : g)));
  const setVariants = (next: VariantRow[]) =>
    form.setValue('variants', next, { shouldDirty: true });
  const setCell = (i: number, patch: Partial<VariantRow>) =>
    setVariants(variants.map((v, k) => (k === i ? { ...v, ...patch } : v)));

  const active = groups.filter((g) => g.values.length > 0);
  const over = variants.length > MAX_VARIANTS;
  const priceBad = issues.includes('price');
  const imgG = active.find((g) => g.name === imgGroup) ?? active[0];

  const pickValueImage = async (gi: number, value: string, f: File | undefined) => {
    if (!f) return;
    setUploading(value);
    try {
      const u = await uploadFile(f, 'VARIANT_IMAGE');
      setGroups(
        groups.map((g, k) =>
          k === gi
            ? {
                ...g,
                values: g.values.map((x) =>
                  x.value === value ? { ...x, imageKey: u.objectKey, imageUrl: u.previewUrl } : x,
                ),
              }
            : g,
        ),
      );
    } catch (e) {
      toast.error(e instanceof UploadError ? t('seller-products.images.err') : errorMessage(e, t));
    } finally {
      setUploading(null);
    }
  };

  return (
    <SectionCard
      id="sec-variants"
      icon="tag"
      title={t('seller-products.variants.title')}
      error={priceBad || over}
      right={
        <span className="ml-auto flex items-center gap-2.5">
          <Badge variant="neutral">
            {t('seller-products.variants.max_groups', { n: MAX_GROUPS })}
          </Badge>
          {!readOnly && groups.length < MAX_GROUPS && (
            <Button
              variant="ghost"
              size="sm"
              iconLeft="plus"
              onClick={() => setGroups([...groups, { name: '', values: [] }])}
            >
              {t('seller-products.variants.add_group')}
            </Button>
          )}
        </span>
      }
    >
      {groups.map((g, i) => (
        <div
          key={i}
          className="border border-solid border-line-subtle rounded-md px-[15px] py-[13px] mb-2.5"
        >
          <div className="flex items-center gap-2.5 mb-2.5 flex-wrap">
            <span className="text-xs font-semibold text-muted">
              {t('seller-products.variants.group_n', { n: i + 1 })}
            </span>
            <GroupNameCombo
              value={g.name}
              disabled={readOnly}
              label={t('seller-products.variants.group_name', { n: i + 1 })}
              onChange={(name) => setGroup(i, { name })}
            />
            {!readOnly && (
              <Button
                variant="ghost"
                size="sm"
                iconLeft="trash-2"
                className="ml-auto !text-red-600"
                onClick={() => setGroups(groups.filter((_, k) => k !== i))}
              >
                {t('seller-products.variants.remove_group')}
              </Button>
            )}
          </div>
          <TagInput
            values={g.values.map((v) => v.value)}
            disabled={readOnly}
            label={t('seller-products.variants.values_of', { name: g.name || i + 1 })}
            onAdd={(value) =>
              setGroup(i, { values: [...g.values, { value, imageKey: null, imageUrl: null }] })
            }
            onRemove={(value) => setGroup(i, { values: g.values.filter((x) => x.value !== value) })}
          />
        </div>
      ))}

      {imgG && (
        <div className="border border-solid border-line-subtle rounded-md px-[15px] py-[13px] mb-2.5">
          <div className="flex items-center gap-2.5 mb-2.5 flex-wrap">
            <span className="text-xs font-semibold text-muted">
              {t('seller-products.variants.images_by')}
            </span>
            <div className="w-[180px]">
              <Select
                aria-label={t('seller-products.variants.images_by')}
                value={imgG.name}
                disabled={readOnly}
                onChange={(v) => form.setValue('variantImageGroup', v, { shouldDirty: true })}
                options={active.map((g, k) => ({
                  value: g.name,
                  label: g.name || t('seller-products.variants.group_n', { n: k + 1 }),
                }))}
              />
            </div>
          </div>
          <div className="flex flex-wrap gap-2.5">
            {imgG.values.map((v) => {
              const gi = groups.indexOf(imgG);
              return (
                <label
                  key={v.value}
                  className={cn(
                    'flex items-center gap-2 text-sm text-body',
                    !readOnly && 'cursor-pointer',
                  )}
                >
                  <input
                    type="file"
                    className="sr-only"
                    disabled={readOnly || uploading !== null}
                    accept="image/jpeg,image/png,image/webp"
                    aria-label={t('seller-products.variants.image_for', { value: v.value })}
                    onChange={(e) => {
                      const f = e.target.files?.[0];
                      e.target.value = '';
                      void pickValueImage(gi, v.value, f);
                    }}
                  />
                  {v.imageUrl ? (
                    <img
                      src={v.imageUrl}
                      alt=""
                      width={36}
                      height={36}
                      className="w-9 h-9 rounded-sm object-cover"
                    />
                  ) : (
                    <span className="w-9 h-9 rounded-sm border-[1.5px] border-dashed border-line-strong grid place-items-center text-subtle">
                      <Icon name={uploading === v.value ? 'clock' : 'image-plus'} size={16} />
                    </span>
                  )}
                  {v.value}
                </label>
              );
            })}
          </div>
        </div>
      )}

      {over && (
        <p className={errCls} role="alert">
          <Icon name="circle-alert" size={13} />
          {t('seller-products.variants.over', { max: MAX_VARIANTS, count: variants.length })}
        </p>
      )}

      {active.length === 0 ? (
        <div className="mt-1.5">
          <p className="m-0 mb-3 text-sm text-muted">{t('seller-products.variants.single_hint')}</p>
          <div className="flex gap-3.5 max-sm:flex-col">
            <div className="flex-1">
              <Input
                label={t('seller-products.variants.price')}
                inputMode="numeric"
                iconRight="banknote"
                placeholder="0"
                disabled={readOnly}
                value={variants[0]?.price ?? ''}
                error={
                  priceBad && !(variants[0]?.price && variants[0].price > 0)
                    ? t('seller-products.err.price')
                    : undefined
                }
                onChange={(e) => setCell(0, { price: toInt(e.target.value) })}
              />
            </div>
            <div className="flex-1">
              <Input
                label={t('seller-products.variants.stock')}
                inputMode="numeric"
                placeholder="0"
                disabled={readOnly}
                value={variants[0]?.stock ?? ''}
                onChange={(e) => setCell(0, { stock: toInt(e.target.value) })}
              />
            </div>
          </div>
        </div>
      ) : (
        <>
          {!readOnly && (
            <div className="flex items-end gap-2.5 flex-wrap bg-surface-sunken rounded-md px-3.5 py-3 mt-1.5 mb-3.5">
              <span className="text-xs font-semibold text-muted mb-[11px]">
                {t('seller-products.variants.bulk')}
              </span>
              <div className="w-[150px]">
                <Input
                  label={t('seller-products.variants.price_short')}
                  inputMode="numeric"
                  iconRight="banknote"
                  value={bulk.price ?? ''}
                  onChange={(e) => setBulk((b) => ({ ...b, price: toInt(e.target.value) }))}
                />
              </div>
              <div className="w-[110px]">
                <Input
                  label={t('seller-products.variants.stock')}
                  inputMode="numeric"
                  value={bulk.stock ?? ''}
                  onChange={(e) => setBulk((b) => ({ ...b, stock: toInt(e.target.value) }))}
                />
              </div>
              <Button
                variant="secondary"
                disabled={bulk.price === null && bulk.stock === null}
                onClick={() =>
                  setVariants(
                    variants.map((v) => ({
                      ...v,
                      price: bulk.price ?? v.price,
                      stock: bulk.stock ?? v.stock,
                    })),
                  )
                }
              >
                {t('seller-products.variants.apply')}
              </Button>
            </div>
          )}
          <div className="overflow-x-auto border border-solid border-line-subtle rounded-md">
            <table className="w-full border-collapse min-w-[560px]">
              <thead>
                <tr className="text-xs font-semibold text-muted text-left bg-gray-25">
                  {active.map((g, i) => (
                    <th key={i} scope="col" className="px-3.5 py-2.5">
                      {g.name || t('seller-products.variants.group_n', { n: i + 1 })}
                    </th>
                  ))}
                  <th scope="col" className="px-3.5 py-2.5">
                    {t('seller-products.variants.price')}
                  </th>
                  <th scope="col" className="px-3.5 py-2.5">
                    {t('seller-products.variants.stock')}
                  </th>
                  <th scope="col" className="px-3.5 py-2.5">
                    SKU
                  </th>
                </tr>
              </thead>
              <tbody>
                {variants.map((v, i) => {
                  const bad = invalidPrice(v) || (priceBad && v.price === null);
                  const name = v.options.join(' / ');
                  return (
                    <tr
                      key={v.options.join('|')}
                      className="border-0 border-t border-solid border-line-subtle text-[13px]"
                    >
                      {v.options.map((o, k) => (
                        <td key={k} className="px-3.5 py-2 text-body">
                          {o}
                        </td>
                      ))}
                      <td className="px-3.5 py-2 w-[150px]">
                        <Input
                          aria-label={t('seller-products.variants.price_of', { name })}
                          inputMode="numeric"
                          placeholder="0"
                          disabled={readOnly}
                          invalid={bad}
                          value={v.price ?? ''}
                          onChange={(e) => setCell(i, { price: toInt(e.target.value) })}
                        />
                        {bad && (
                          <div className="text-[11px] text-red-600 mt-[3px]">
                            {t('seller-products.err.price_short')}
                          </div>
                        )}
                      </td>
                      <td className="px-3.5 py-2 w-[110px]">
                        <Input
                          aria-label={t('seller-products.variants.stock_of', { name })}
                          inputMode="numeric"
                          placeholder="0"
                          disabled={readOnly}
                          value={v.stock ?? ''}
                          onChange={(e) => setCell(i, { stock: toInt(e.target.value) })}
                        />
                      </td>
                      <td className="px-3.5 py-2 w-[150px]">
                        <span className="code text-xs text-muted">
                          {v.sku ?? skuPreview(v.options)}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}
    </SectionCard>
  );
}

export function ShippingInfo({ form, readOnly, issues }: SectionProps) {
  const { t } = useT();
  const payer = useWatch({ control: form.control, name: 'shippingPayer' });
  const dims = useWatch({ control: form.control, name: 'dims' });
  const weight = useWatch({ control: form.control, name: 'weightGrams' });
  const threshold = useWatch({ control: form.control, name: 'freeShippingThreshold' });
  const pre = useWatch({ control: form.control, name: 'isPreOrder' });
  const days = useWatch({ control: form.control, name: 'preOrderDays' });
  const dayBad = pre && (days === null || days < PRE_ORDER_MIN || days > PRE_ORDER_MAX);
  const weightBad = issues.includes('weight');
  const dimCls =
    'w-full h-11 border-[1.5px] border-solid border-line rounded-md pl-[30px] pr-3 text-base text-strong bg-surface-card outline-none focus:border-primary disabled:bg-surface-sunken';
  return (
    <SectionCard
      id="sec-shipping"
      icon="package"
      title={t('seller-products.shipping.title')}
      error={weightBad || (issues.includes('pre_order') && dayBad)}
    >
      <div className="flex gap-5 flex-wrap mb-3.5">
        <div className="w-[200px]">
          <Input
            label={t('seller-products.shipping.weight')}
            required
            inputMode="numeric"
            disabled={readOnly}
            value={weight ?? ''}
            error={weightBad ? t('seller-products.err.weight') : undefined}
            onChange={(e) =>
              form.setValue('weightGrams', toInt(e.target.value), { shouldDirty: true })
            }
          />
        </div>
        <fieldset className="border-0 p-0 m-0">
          <legend className={labCls}>{t('seller-products.shipping.dims')}</legend>
          <div className="flex gap-2">
            {(['length', 'width', 'height'] as const).map((k) => (
              <div key={k} className="relative w-[92px]">
                <span
                  className="absolute left-[11px] top-[13px] text-[13px] font-semibold text-subtle"
                  aria-hidden="true"
                >
                  {t(`seller-products.shipping.dim_short.${k}`)}:
                </span>
                <input
                  aria-label={t(`seller-products.shipping.dim.${k}`)}
                  inputMode="numeric"
                  disabled={readOnly}
                  value={dims[k] ?? ''}
                  onChange={(e) =>
                    form.setValue(
                      'dims',
                      { ...dims, [k]: toInt(e.target.value) },
                      { shouldDirty: true },
                    )
                  }
                  className={dimCls}
                />
              </div>
            ))}
          </div>
        </fieldset>
      </div>
      <p className={cn(hintCls, 'flex gap-[7px] mt-0 mb-4')}>
        <Icon name="info" size={14} className="flex-none mt-px" />
        {t('seller-products.shipping.hint')}
      </p>
      <div className="grid grid-cols-2 gap-[18px] max-md:grid-cols-1">
        <div>
          <span className={labCls}>{t('seller-products.shipping.from')}</span>
          <div className="flex items-center gap-2 h-11 px-3 rounded-md bg-surface-sunken text-sm text-body">
            <Icon name="map-pin" size={16} className="text-subtle" />
            {t('seller-products.shipping.from_value')}
          </div>
          <p className={hintCls}>{t('seller-products.shipping.from_hint')}</p>
        </div>
        <fieldset className="border-0 p-0 m-0">
          <legend className={labCls}>{t('seller-products.shipping.payer')}</legend>
          <div className="flex flex-col gap-1">
            <Radio
              name="payer"
              checked={payer === 'BUYER'}
              disabled={readOnly}
              onChange={() => form.setValue('shippingPayer', 'BUYER', { shouldDirty: true })}
              className="text-sm"
            >
              {t('seller-products.shipping.buyer')}
            </Radio>
            <div className="flex items-center gap-2 flex-wrap">
              <Radio
                name="payer"
                checked={payer === 'SELLER'}
                disabled={readOnly}
                onChange={() => form.setValue('shippingPayer', 'SELLER', { shouldDirty: true })}
                className="text-sm"
              >
                {t('seller-products.shipping.seller')}
              </Radio>
              <input
                aria-label={t('seller-products.shipping.threshold')}
                inputMode="numeric"
                disabled={readOnly || payer !== 'SELLER'}
                value={threshold ?? ''}
                onChange={(e) =>
                  form.setValue('freeShippingThreshold', toInt(e.target.value), {
                    shouldDirty: true,
                  })
                }
                className="w-[110px] h-9 border-[1.5px] border-solid border-line rounded-md px-2.5 text-sm outline-none disabled:opacity-50"
              />
              <span className="text-muted text-sm">₫</span>
            </div>
          </div>
        </fieldset>
      </div>
      <div className="flex items-center gap-3 mt-4 pt-3.5 border-0 border-t border-solid border-line-subtle flex-wrap">
        <Switch
          checked={pre}
          disabled={readOnly}
          onChange={(e) => form.setValue('isPreOrder', e.target.checked, { shouldDirty: true })}
        >
          <span className="text-sm text-body">{t('seller-products.shipping.pre_order')}</span>
        </Switch>
        {pre && (
          <>
            <input
              aria-label={t('seller-products.shipping.pre_days')}
              inputMode="numeric"
              disabled={readOnly}
              value={days ?? ''}
              onChange={(e) =>
                form.setValue('preOrderDays', toInt(e.target.value), { shouldDirty: true })
              }
              className={cn(
                'w-[70px] h-9 border-[1.5px] border-solid rounded-md px-2.5 text-sm outline-none',
                dayBad ? 'border-red-500' : 'border-line',
              )}
            />
            <span className="text-xs text-muted">
              {t('seller-products.shipping.pre_days_hint', {
                min: PRE_ORDER_MIN,
                max: PRE_ORDER_MAX,
              })}
            </span>
          </>
        )}
      </div>
    </SectionCard>
  );
}
