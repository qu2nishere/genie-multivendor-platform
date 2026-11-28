// Khối Phân loại & Giá (G11.28: tối đa 2 nhóm, tên nhóm chọn / tự đặt, giá trị dạng thẻ, ảnh theo phân loại,
// áp dụng nhanh, bảng giá / tồn / SKU theo tổ hợp, kiểm giá > 0, ≤ 50 phân loại) và Vận chuyển (G11.29: cân
// nặng, kích thước, phí ship, đặt trước 7–30 ngày) — prototype VariantSection, GroupNameCombo, TagInput,
// ShippingInfo.
import { useEffect, useId, useRef, useState } from 'react';
import { useWatch } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { useShopSettings } from '@/features/seller-settings';
import { errorMessage } from '@/shared/api/errors';
import { UploadError, uploadFile } from '@/shared/api/upload';
import { useClickOutside } from '@/shared/hooks/useClickOutside';
import { useT } from '@/shared/hooks/useT';
import { cn } from '@/shared/lib/cn';
import { Badge, Button, Checkbox, Icon, Input, Radio, Select, Switch, toast } from '@/shared/ui';
import {
  applyBulk,
  GROUP_NAMES,
  MAX_GROUPS,
  MAX_VARIANTS,
  PRE_ORDER_MAX,
  PRE_ORDER_MIN,
  invalidPrice,
  syncVariants,
} from '../../domain/product.rules';
import type { OptionGroup, VariantRow } from '../../domain/product.types';
import {
  errCls,
  groupDigits,
  hintCls,
  labCls,
  onGroupedChange,
  SectionCard,
  toInt,
  type SectionProps,
} from './editorKit';

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
    <div ref={box} className="relative w-[260px] max-sm:w-full">
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
        className="w-full h-10 border-[1.5px] border-solid border-line rounded-md px-3 text-sm text-strong bg-surface-card outline-none focus:border-primary disabled:bg-surface-sunken"
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

/**
 * Ô giá trị của một nhóm: mỗi giá trị là một thẻ; nhóm được chọn làm "ảnh theo phân loại" thì thẻ có thêm ô ảnh nhỏ
 * ngay bên trái (bấm để tải ảnh) — ảnh gắn thẳng vào giá trị, không tách khung riêng.
 */
/** Ô nhập giá trị + nút Thêm — nằm cạnh tên nhóm ở đầu thẻ (Enter / dấu phẩy cũng thêm). */
function ValueAdder({
  existing,
  onAdd,
  label,
}: {
  existing: string[];
  onAdd: (v: string) => void;
  label: string;
}) {
  const { t } = useT();
  const [v, setV] = useState('');
  const add = () => {
    const x = v.trim();
    if (x && !existing.includes(x)) onAdd(x);
    setV('');
  };
  return (
    <span className="inline-flex items-center gap-1.5 max-sm:w-full">
      <input
        value={v}
        aria-label={label}
        placeholder={t('seller-products.variants.value_placeholder')}
        onChange={(e) => setV(e.target.value)}
        onKeyDown={(e) => {
          // Telex / VNI đang ghép chữ: Enter thuộc về bộ gõ, chưa phải "thêm giá trị"
          if (e.nativeEvent.isComposing) return;
          if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            add();
          }
        }}
        onBlur={add}
        maxLength={30}
        className="w-[200px] h-10 border-[1.5px] border-solid border-line rounded-md px-3 text-sm text-strong outline-none bg-surface-card focus:border-primary max-sm:flex-1"
      />
      <Button
        variant="secondary"
        iconLeft="plus"
        disabled={!v.trim()}
        onMouseDown={(e) => e.preventDefault()}
        onClick={add}
      >
        {t('seller-products.variants.add_value')}
      </Button>
    </span>
  );
}

function ValueTags({
  values,
  withImages,
  uploading,
  onRemove,
  onPickImage,
  disabled,
}: {
  values: OptionGroup['values'];
  withImages: boolean;
  uploading: string | null;
  onRemove: (v: string) => void;
  onPickImage: (value: string, f: File | undefined) => void;
  disabled: boolean;
}) {
  const { t } = useT();
  if (values.length === 0) {
    return <p className="m-0 text-sm text-subtle">{t('seller-products.variants.no_values')}</p>;
  }
  return (
    <div className="flex flex-wrap gap-2 items-center">
      {values.map((o) => (
        <span
          key={o.value}
          className={cn(
            'inline-flex items-center gap-2 bg-surface-card border border-solid border-line rounded-md text-[13px] font-semibold text-strong',
            withImages ? 'h-11 pl-1 pr-2' : 'h-9 pl-3 pr-2',
          )}
        >
          {withImages && (
            <label
              className={cn(
                'w-9 h-9 rounded-sm overflow-hidden grid place-items-center flex-none',
                o.imageUrl ? '' : 'border-[1.5px] border-dashed border-line-strong text-subtle',
                !disabled && 'cursor-pointer hover:border-primary hover:text-brand-600',
              )}
              title={t('seller-products.variants.image_for', { value: o.value })}
            >
              <input
                type="file"
                className="sr-only"
                disabled={disabled || uploading !== null}
                accept="image/jpeg,image/png,image/webp"
                aria-label={t('seller-products.variants.image_for', { value: o.value })}
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  e.target.value = '';
                  onPickImage(o.value, f);
                }}
              />
              {o.imageUrl ? (
                <img
                  src={o.imageUrl}
                  alt=""
                  width={36}
                  height={36}
                  className="w-9 h-9 object-cover"
                />
              ) : (
                <Icon name={uploading === o.value ? 'clock' : 'image-plus'} size={16} />
              )}
            </label>
          )}
          {o.value}
          {!disabled && (
            <button
              type="button"
              aria-label={t('seller-products.variants.remove_value', { value: o.value })}
              onClick={() => onRemove(o.value)}
              className="border-0 bg-transparent cursor-pointer grid place-items-center w-6 h-6 rounded-full text-subtle hover:text-red-600 hover:bg-red-50 p-0"
            >
              <Icon name="x" size={14} />
            </button>
          )}
        </span>
      ))}
    </div>
  );
}

export function VariantSection({ form, readOnly, issues }: SectionProps) {
  const { t } = useT();
  const groups = useWatch({ control: form.control, name: 'groups' });
  const variants = useWatch({ control: form.control, name: 'variants' });
  const [bulk, setBulk] = useState<{ price: number | null; stock: number | null }>({
    price: null,
    stock: null,
  });
  // lọc dòng "Áp dụng hàng loạt" theo giá trị từng nhóm ('' = mọi giá trị) — giá theo Dung lượng, màu nào cũng như nhau
  const [bulkMatch, setBulkMatch] = useState<string[]>([]);
  const [uploading, setUploading] = useState<string | null>(null);

  const setGroups = (next: OptionGroup[]) => {
    form.setValue('groups', next, { shouldDirty: true });
    form.setValue('variants', syncVariants(next, form.getValues('variants')), {
      shouldDirty: true,
    });
    // nhóm ảnh chính (ảnh của SKU — giỏ, đơn hàng) = nhóm đầu tiên đang dùng ảnh
    const primary =
      next.find((g) => g.withImages && g.name.trim() && g.values.length)?.name ?? null;
    if (primary !== form.getValues('variantImageGroup'))
      form.setValue('variantImageGroup', primary, { shouldDirty: true });
  };
  // Bật / tắt ảnh cho một nhóm (chọn được nhiều nhóm); tắt thì bỏ ảnh đã gắn của nhóm đó
  const toggleImages = (i: number, on: boolean) =>
    setGroups(
      groups.map((g, k) =>
        k !== i
          ? g
          : {
              ...g,
              withImages: on,
              values: on
                ? g.values
                : g.values.map((v) => ({ ...v, imageKey: null, imageUrl: null })),
            },
      ),
    );
  const setGroup = (i: number, patch: Partial<OptionGroup>) =>
    setGroups(groups.map((g, k) => (k === i ? { ...g, ...patch } : g)));
  const setVariants = (next: VariantRow[]) =>
    form.setValue('variants', next, { shouldDirty: true });
  const setCell = (i: number, patch: Partial<VariantRow>) =>
    setVariants(variants.map((v, k) => (k === i ? { ...v, ...patch } : v)));

  const active = groups.filter((g) => g.values.length > 0);
  const over = variants.length > MAX_VARIANTS;
  const priceBad = issues.includes('price');
  // các nhóm đang dùng ảnh, theo thứ tự nhóm (ảnh nhỏ ở bảng giá lấy nhóm đầu tiên có ảnh)
  const imageGroups = groups.filter((g) => g.withImages && g.name.trim() && g.values.length);

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

  // bộ lọc của dòng Áp dụng hàng loạt: vừa giới hạn phạm vi áp dụng, vừa lọc các dòng đang hiện
  const match = active.map((g, k) => {
    const m = bulkMatch[k] ?? '';
    return m && g.values.some((x) => x.value === m) ? m : null;
  });
  const filtering = match.some((m) => m !== null);
  // giữ chỉ số gốc (setCell sửa theo vị trí trong toàn bộ danh sách)
  const shownRows = variants
    .map((v, i) => ({ v, i }))
    .filter(({ v }) => match.every((m, k) => m === null || v.options[k] === m));
  const ROWS_BEFORE_SCROLL = 10;
  // Bộ cột dùng chung cho khung lọc và bảng giá (table-fixed): cột nhóm chia đều phần còn lại
  const cols = (
    <colgroup>
      {active.map((_, k) => (
        <col key={k} />
      ))}
      <col className="w-[240px]" />
      <col className="w-[190px]" />
      <col className="w-[150px]" />
    </colgroup>
  );
  // bề rộng thanh cuộn dọc của bảng giá → khung lọc chừa đúng chừng đó để các cột thẳng nhau
  const scrollBox = useRef<HTMLDivElement>(null);
  const [gutter, setGutter] = useState(0);
  useEffect(() => {
    const el = scrollBox.current;
    if (!el) return undefined;
    const measure = () => setGutter(Math.max(0, el.offsetWidth - el.clientWidth - 2));
    measure();
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, [shownRows.length, active.length]);

  const runBulk = () => {
    const r = applyBulk(variants, match, bulk);
    setVariants(r.variants);
    toast.success(t('seller-products.variants.bulk_done', { count: r.count }));
    setBulk({ price: null, stock: null });
  };

  return (
    <SectionCard
      id="sec-variants"
      icon="tag"
      title={t('seller-products.variants.title')}
      error={priceBad || over}
      right={
        <span className="ml-auto">
          <Badge variant="neutral">
            {t('seller-products.variants.max_groups', { n: MAX_GROUPS })}
          </Badge>
        </span>
      }
    >
      {groups.map((g, i) => {
        const taken = groups.filter((_, k) => k !== i).map((x) => x.name);
        const quick = GROUP_NAMES.filter((n) => !taken.includes(n)).slice(0, 5);
        const hasImages = !!g.withImages && !!g.name.trim() && g.values.length > 0;
        return (
          <div
            key={i}
            className="border border-solid border-line-subtle rounded-md mb-3 overflow-hidden"
          >
            {/* Đầu thẻ: số nhóm + tên nhóm + xóa */}
            <div className="flex items-center gap-3 flex-wrap bg-surface-sunken px-4 py-3">
              <span className="w-6 h-6 rounded-full bg-primary text-primary-on text-xs font-bold grid place-items-center flex-none">
                {i + 1}
              </span>
              <span className="text-[13px] font-semibold text-strong whitespace-nowrap">
                {t('seller-products.variants.group_label')}
              </span>
              <GroupNameCombo
                value={g.name}
                disabled={readOnly}
                label={t('seller-products.variants.group_name', { n: i + 1 })}
                onChange={(name) => setGroup(i, { name })}
              />
              {!readOnly && (
                <ValueAdder
                  existing={g.values.map((x) => x.value)}
                  label={t('seller-products.variants.values_of', { name: g.name || i + 1 })}
                  onAdd={(value) =>
                    setGroup(i, {
                      values: [...g.values, { value, imageKey: null, imageUrl: null }],
                    })
                  }
                />
              )}
              {!readOnly && (
                <button
                  type="button"
                  aria-label={t('seller-products.variants.remove_group_n', { n: i + 1 })}
                  title={t('seller-products.variants.remove_group')}
                  onClick={() => setGroups(groups.filter((_, k) => k !== i))}
                  className="ml-auto w-9 h-9 rounded-md border-0 bg-transparent cursor-pointer grid place-items-center text-subtle hover:text-red-600 hover:bg-red-50"
                >
                  <Icon name="trash-2" size={17} />
                </button>
              )}
            </div>

            <div className="px-4 py-3.5">
              {!readOnly && !g.name.trim() && (
                <div className="flex items-center gap-1.5 flex-wrap mb-3">
                  <span className="text-xs text-muted">
                    {t('seller-products.variants.suggest')}
                  </span>
                  {quick.map((n) => (
                    <button
                      key={n}
                      type="button"
                      onClick={() => setGroup(i, { name: n })}
                      className="h-7 px-2.5 rounded-full border border-solid border-line bg-surface-card text-xs font-semibold text-body cursor-pointer hover:border-primary hover:text-brand-600"
                    >
                      {n}
                    </button>
                  ))}
                </div>
              )}
              <ValueTags
                values={g.values}
                withImages={hasImages}
                uploading={uploading}
                disabled={readOnly}
                onRemove={(value) =>
                  setGroup(i, { values: g.values.filter((x) => x.value !== value) })
                }
                onPickImage={(value, f) => void pickValueImage(i, value, f)}
              />
              <div className="flex items-center gap-3 flex-wrap mt-2">
                {!readOnly && (
                  <p className="m-0 text-xs text-muted">
                    {t('seller-products.variants.value_hint')}
                  </p>
                )}
                {/* Bật ảnh cho nhóm này — tick được ở nhiều nhóm (vd Màu sắc và Dung lượng) */}
                {g.values.length > 0 && !!g.name.trim() && (
                  <span className="ml-auto">
                    <Checkbox
                      checked={!!g.withImages}
                      disabled={readOnly}
                      onChange={(e) => toggleImages(i, e.target.checked)}
                      className="text-xs"
                    >
                      {t('seller-products.variants.images_here')}
                    </Checkbox>
                  </span>
                )}
              </div>
            </div>
          </div>
        );
      })}

      {!readOnly && groups.length < MAX_GROUPS && (
        <button
          type="button"
          onClick={() =>
            setGroups([...groups, { name: '', values: [], withImages: groups.length === 0 }])
          }
          className="flex items-center gap-3 w-full mb-4 px-4 py-3 border-[1.5px] border-dashed border-line-strong rounded-md bg-transparent cursor-pointer text-left hover:border-primary hover:bg-primary-tint"
        >
          <span className="w-8 h-8 rounded-full bg-primary-tint text-brand-600 grid place-items-center flex-none">
            <Icon name="plus" size={17} />
          </span>
          <span>
            <span className="block text-sm font-semibold text-strong">
              {groups.length === 0
                ? t('seller-products.variants.add_group_cta')
                : t('seller-products.variants.add_group_second')}
            </span>
            <span className="block text-xs text-muted mt-0.5">
              {groups.length === 0
                ? t('seller-products.variants.add_group_hint', { n: MAX_GROUPS })
                : t('seller-products.variants.add_group_second_hint')}
            </span>
          </span>
        </button>
      )}

      {over && (
        <p className={errCls} role="alert">
          <Icon name="circle-alert" size={13} />
          {t('seller-products.variants.over', { max: MAX_VARIANTS, count: variants.length })}
        </p>
      )}

      {active.length === 0 ? (
        <div className="mt-1.5">
          <p className="m-0 mb-3 text-sm text-muted">
            {groups.length === 0
              ? t('seller-products.variants.single_hint')
              : t('seller-products.variants.need_values')}
          </p>
          <div className="flex gap-3.5 max-sm:flex-col">
            <div className="flex-1">
              <Input
                label={t('seller-products.variants.price')}
                inputMode="numeric"
                iconRight="banknote"
                placeholder="0"
                disabled={readOnly}
                value={groupDigits(variants[0]?.price)}
                error={
                  priceBad && !(variants[0]?.price && variants[0].price > 0)
                    ? t('seller-products.err.price')
                    : undefined
                }
                onChange={(e) => onGroupedChange(e, (n) => setCell(0, { price: n }))}
              />
            </div>
            <div className="flex-1">
              <Input
                label={t('seller-products.variants.stock')}
                inputMode="numeric"
                placeholder="0"
                disabled={readOnly}
                value={groupDigits(variants[0]?.stock)}
                onChange={(e) => onGroupedChange(e, (n) => setCell(0, { stock: n }))}
              />
            </div>
          </div>
        </div>
      ) : (
        <>
          <p className="m-0 mb-2 text-sm font-semibold text-strong">
            {t('seller-products.variants.table_title', { count: variants.length })}
          </p>
          {!readOnly && active.length > 1 && (
            <p className={cn(hintCls, 'flex gap-[7px] mt-0 mb-2.5')}>
              <Icon name="info" size={14} className="flex-none mt-px" />
              {t('seller-products.variants.price_by_group_hint', {
                a: active[0].name || 1,
                b: active[1].name || 2,
              })}
            </p>
          )}
          {/* Lọc & áp dụng hàng loạt: bảng 1 dòng NGOÀI khung cuộn (menu chọn không bị che), dùng chung bộ cột với
              bảng giá bên dưới → ô Màu sắc / Dung lượng / Giá / Tồn kho thẳng cột; chừa bề rộng thanh cuộn của bảng dưới */}
          {!readOnly && variants.length > 1 && (
            <div
              className="bg-primary-tint rounded-md border border-solid border-transparent mb-2.5"
              style={{ paddingRight: gutter }}
            >
              <div className="px-3.5 pt-2.5 text-xs font-semibold text-brand-700">
                {t('seller-products.variants.bulk')}
              </div>
              <table className="w-full table-fixed border-collapse min-w-[560px] max-sm:min-w-0">
                {cols}
                <tbody>
                  <tr>
                    {active.map((g, k) => (
                      <td key={k} className="px-3.5 py-2.5 align-top">
                        <Select
                          aria-label={t('seller-products.variants.bulk_filter', {
                            name: g.name || k + 1,
                          })}
                          value={bulkMatch[k] ?? ''}
                          onChange={(v) =>
                            setBulkMatch((m) => {
                              const next = [...m];
                              next[k] = v;
                              return next;
                            })
                          }
                          options={[
                            {
                              value: '',
                              label: t('seller-products.variants.bulk_all', {
                                name: g.name || k + 1,
                              }),
                            },
                            ...g.values.map((x) => ({ value: x.value, label: x.value })),
                          ]}
                        />
                      </td>
                    ))}
                    <td className="px-3.5 py-2.5 align-top">
                      <Input
                        aria-label={t('seller-products.variants.bulk_price')}
                        inputMode="numeric"
                        placeholder={t('seller-products.variants.price_short')}
                        value={groupDigits(bulk.price)}
                        onChange={(e) =>
                          onGroupedChange(e, (n) => setBulk((b) => ({ ...b, price: n })))
                        }
                      />
                    </td>
                    <td className="px-3.5 py-2.5 align-top">
                      <Input
                        aria-label={t('seller-products.variants.bulk_stock')}
                        inputMode="numeric"
                        placeholder={t('seller-products.variants.stock')}
                        value={groupDigits(bulk.stock)}
                        onChange={(e) =>
                          onGroupedChange(e, (n) => setBulk((b) => ({ ...b, stock: n })))
                        }
                      />
                    </td>
                    <td className="px-3.5 py-2.5 align-top">
                      <Button
                        block
                        disabled={bulk.price === null && bulk.stock === null}
                        onClick={runBulk}
                      >
                        {t('seller-products.variants.apply')}
                      </Button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          )}
          {filtering && (
            <p className="m-0 mb-2 text-xs text-muted flex items-center gap-2 flex-wrap">
              {t('seller-products.variants.filtered', {
                shown: shownRows.length,
                total: variants.length,
              })}
              <button
                type="button"
                onClick={() => setBulkMatch([])}
                className="border-0 bg-transparent p-0 cursor-pointer text-xs font-semibold text-brand-600 hover:underline"
              >
                {t('seller-products.variants.filter_clear')}
              </button>
            </p>
          )}
          {/* Hơn 10 dòng: khung cuộn dọc, hàng tiêu đề dính trên cùng */}
          <div
            ref={scrollBox}
            className={cn(
              'relative overflow-x-auto border border-solid border-line-subtle rounded-md',
              shownRows.length > ROWS_BEFORE_SCROLL && 'max-h-[660px] overflow-y-auto',
            )}
          >
            <table className="w-full table-fixed border-collapse min-w-[560px]">
              {cols}
              <thead className="sticky top-0 z-[1] bg-surface-card">
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
                {shownRows.map(({ v, i }) => {
                  const bad = invalidPrice(v) || (priceBad && v.price === null);
                  const name = v.options.join(' / ');
                  const img =
                    imageGroups
                      .map((g) => g.values.find((x) => v.options.includes(x.value))?.imageUrl)
                      .find(Boolean) ?? null;
                  return (
                    <tr
                      key={v.options.join('|')}
                      className="border-0 border-t border-solid border-line-subtle text-[13px]"
                    >
                      {v.options.map((o, k) => (
                        <td key={k} className="px-3.5 py-2 text-body">
                          <span className="inline-flex items-center gap-2">
                            {k === 0 && img && (
                              <img
                                src={img}
                                alt=""
                                width={28}
                                height={28}
                                className="w-7 h-7 rounded-sm object-cover"
                              />
                            )}
                            {o}
                          </span>
                        </td>
                      ))}
                      <td className="px-3.5 py-2">
                        <Input
                          aria-label={t('seller-products.variants.price_of', { name })}
                          inputMode="numeric"
                          placeholder="0"
                          disabled={readOnly}
                          invalid={bad}
                          value={groupDigits(v.price)}
                          onChange={(e) => onGroupedChange(e, (n) => setCell(i, { price: n }))}
                        />
                        {bad && (
                          <div className="text-[11px] text-red-600 mt-[3px]">
                            {t('seller-products.err.price_short')}
                          </div>
                        )}
                      </td>
                      <td className="px-3.5 py-2">
                        <Input
                          aria-label={t('seller-products.variants.stock_of', { name })}
                          inputMode="numeric"
                          placeholder="0"
                          disabled={readOnly}
                          value={groupDigits(v.stock)}
                          onChange={(e) => onGroupedChange(e, (n) => setCell(i, { stock: n }))}
                        />
                      </td>
                      <td className="px-3.5 py-2">
                        {v.sku ? (
                          <span className="code text-xs text-body">{v.sku}</span>
                        ) : (
                          <span className="text-xs text-subtle italic">
                            {t('seller-products.variants.sku_auto')}
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {shownRows.length === 0 && (
                  <tr>
                    <td
                      colSpan={active.length + 3}
                      className="px-3.5 py-6 text-center text-sm text-muted"
                    >
                      {t('seller-products.variants.filter_empty')}
                    </td>
                  </tr>
                )}
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
  const pre = useWatch({ control: form.control, name: 'isPreOrder' });
  const pickup = useShopSettings().data?.pickup;
  const fromText = [pickup?.ward?.name, pickup?.province?.name].filter(Boolean).join(', ');
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
          {/* Địa chỉ lấy hàng thật của shop (Cài đặt › Địa chỉ lấy hàng — API 10.11) */}
          <div className="flex items-center gap-2 h-11 px-3 rounded-md bg-surface-sunken text-sm text-body">
            <Icon name="map-pin" size={16} className="text-subtle" />
            {fromText || (
              <Link to="/settings" className="text-brand-600 font-semibold">
                {t('seller-products.shipping.from_none')}
              </Link>
            )}
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
            <Radio
              name="payer"
              checked={payer === 'SELLER'}
              disabled={readOnly}
              onChange={() => form.setValue('shippingPayer', 'SELLER', { shouldDirty: true })}
              className="text-sm"
            >
              {t('seller-products.shipping.seller')}
            </Radio>
          </div>
          {/* Mức đơn được freeship đặt ở cấp shop (plan C1 D23) */}
          <p className={hintCls}>{t('seller-products.shipping.threshold_hint')}</p>
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
