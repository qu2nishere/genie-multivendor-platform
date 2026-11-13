// Nhãn hiển thị cho bộ lọc (dùng chung cho island và .astro).
import { formatVND } from '@/shared/lib/format';
import type { TFunction } from '@/shared/lib/i18n';
import type { Locale } from '@/shared/lib/locale';
import type { FilterChip, FilterState, PriceBand, SortId } from '../domain/filters';

/** "Dưới 100.000₫" · "100.000₫ – 500.000₫" · "Trên 1.000.000₫". */
export function bandLabel(b: PriceBand, t: TFunction, locale: Locale): string {
  const money = (n: number) => formatVND(n, { locale });
  if (!b.min && b.max != null) return t('search.band.under', { max: money(b.max) });
  if (b.max == null) return t('search.band.over', { min: money(b.min) });
  return `${money(b.min)} – ${money(b.max)}`;
}

/** Khoảng giá tự nhập. */
export function rangeLabel(
  s: Pick<FilterState, 'min' | 'max'>,
  t: TFunction,
  locale: Locale,
): string {
  const money = (n: number) => formatVND(n, { locale });
  if (s.min != null && s.max != null) return `${money(s.min)} – ${money(s.max)}`;
  if (s.min != null) return t('search.band.from', { min: money(s.min) });
  return t('search.band.to', { max: money(s.max ?? 0) });
}

export const sortLabel = (id: SortId, t: TFunction): string => t(`search.sort.${id}`);

export interface ChipNames {
  /** slug → tên (danh mục gốc, danh mục con). */
  categories: Record<string, string>;
  bands: readonly PriceBand[];
}

/** Nhãn thẻ "đang lọc" (prototype chips: tên danh mục, khoảng giá, "Shop Chính Hãng", "4 sao trở lên"…). */
export function chipLabel(
  c: FilterChip,
  s: FilterState,
  names: ChipNames,
  t: TFunction,
  locale: Locale,
): string {
  switch (c.kind) {
    case 'sub':
    case 'cat':
      return names.categories[c.value] ?? c.value;
    case 'price': {
      if (s.min != null || s.max != null) return rangeLabel(s, t, locale);
      const b = names.bands.find((x) => x.id === c.value);
      return b ? bandLabel(b, t, locale) : c.value;
    }
    case 'authentic':
      return t('search.filter.authentic_shop');
    case 'freeship':
      return t('search.filter.freeship');
    case 'voucher':
      return t('search.filter.voucher');
    case 'rating':
      return t('search.filter.rating_up', { n: c.value });
    default:
      return c.value;
  }
}
