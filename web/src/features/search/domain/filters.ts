// Bộ lọc & sắp xếp danh sách sản phẩm (prototype Category.jsx, Search.jsx) — plan G08.17.
// Trạng thái lọc sống trong query string: trang SSR đọc URL, mọi lựa chọn là liên kết / form GET thật
// (tắt JS vẫn lọc được — R16). Hàm thuần, có test.

export type SortId = 'rel' | 'hot' | 'new' | 'price';
export type SortDir = 'asc' | 'desc';
export type SearchScope = 'all' | 'shop';

/** Thứ tự nút sắp xếp: danh mục (Liên quan · Bán chạy · Mới nhất · Giá), tìm kiếm (… Mới nhất · Bán chạy …). */
export const CATEGORY_SORTS: readonly SortId[] = ['rel', 'hot', 'new', 'price'];
export const SEARCH_SORTS: readonly SortId[] = ['rel', 'new', 'hot', 'price'];

export interface PriceBand {
  id: string;
  min: number;
  /** null = không giới hạn trên. */
  max: number | null;
}

/** Khoảng giá trang danh mục (prototype Category.jsx BANDS). */
export const CATEGORY_BANDS: readonly PriceBand[] = [
  { id: 'b1', min: 0, max: 100_000 },
  { id: 'b2', min: 100_000, max: 500_000 },
  { id: 'b3', min: 500_000, max: 1_000_000 },
  { id: 'b4', min: 1_000_000, max: null },
];

/** Khoảng giá trang tìm kiếm (prototype Search.jsx PRICE_BANDS). */
export const SEARCH_BANDS: readonly PriceBand[] = [
  { id: 'b1', min: 0, max: 100_000 },
  { id: 'b2', min: 100_000, max: 300_000 },
  { id: 'b3', min: 300_000, max: 700_000 },
  { id: 'b4', min: 700_000, max: 5_000_000 },
  { id: 'b5', min: 5_000_000, max: null },
];

/** Mức "từ N sao trở lên". */
export const RATINGS: readonly number[] = [5, 4, 3];

export interface FilterState {
  q: string;
  scope: SearchScope;
  shop: number | null;
  /** Danh mục con (slug) trên trang danh mục. */
  sub: string | null;
  /** Danh mục gốc (slug) trên trang tìm kiếm. */
  cats: string[];
  brands: string[];
  places: string[];
  attrs: string[];
  band: string | null;
  /** Khoảng giá tự nhập (ưu tiên hơn band). */
  min: number | null;
  max: number | null;
  authentic: boolean;
  freeship: boolean;
  voucher: boolean;
  rating: number;
  sort: SortId;
  dir: SortDir;
  page: number;
}

export const EMPTY_FILTERS: FilterState = {
  q: '',
  scope: 'all',
  shop: null,
  sub: null,
  cats: [],
  brands: [],
  places: [],
  attrs: [],
  band: null,
  min: null,
  max: null,
  authentic: false,
  freeship: false,
  voucher: false,
  rating: 0,
  sort: 'rel',
  dir: 'asc',
  page: 1,
};

const SORT_IDS: readonly SortId[] = ['rel', 'hot', 'new', 'price'];

const posInt = (v: string | null): number | null => {
  if (v == null || !/^\d+$/.test(v.trim())) return null;
  const n = Number(v);
  return Number.isSafeInteger(n) ? n : null;
};

const uniq = (xs: string[]) => Array.from(new Set(xs.map((x) => x.trim()).filter(Boolean)));

/** URL → trạng thái. Giá trị lạ bị bỏ qua (không ném lỗi), trang tối thiểu 1. */
export function parseFilters(params: URLSearchParams): FilterState {
  const sort = params.get('sort') as SortId | null;
  const rating = posInt(params.get('rating')) ?? 0;
  const shop = posInt(params.get('shop'));
  return {
    q: (params.get('q') ?? '').trim().slice(0, 100),
    scope: params.get('scope') === 'shop' && shop ? 'shop' : 'all',
    shop,
    sub: params.get('sub')?.trim() || null,
    cats: uniq(params.getAll('cat')),
    brands: uniq(params.getAll('brand')),
    places: uniq(params.getAll('place')),
    attrs: uniq(params.getAll('attr')),
    band: params.get('band')?.trim() || null,
    min: posInt(params.get('min')),
    max: posInt(params.get('max')),
    authentic: params.get('authentic') === '1',
    freeship: params.get('freeship') === '1',
    voucher: params.get('voucher') === '1',
    rating: RATINGS.includes(rating) ? rating : 0,
    sort: sort && SORT_IDS.includes(sort) ? sort : 'rel',
    dir: params.get('dir') === 'desc' ? 'desc' : 'asc',
    page: Math.max(1, posInt(params.get('page')) ?? 1),
  };
}

/** Trạng thái → URL; bỏ giá trị mặc định để URL gọn và canonical ổn định. */
export function serializeFilters(s: FilterState): URLSearchParams {
  const p = new URLSearchParams();
  if (s.q) p.set('q', s.q);
  if (s.scope === 'shop' && s.shop) p.set('scope', 'shop');
  if (s.shop) p.set('shop', String(s.shop));
  if (s.sub) p.set('sub', s.sub);
  s.cats.forEach((v) => p.append('cat', v));
  s.brands.forEach((v) => p.append('brand', v));
  s.places.forEach((v) => p.append('place', v));
  s.attrs.forEach((v) => p.append('attr', v));
  if (s.band && s.min == null && s.max == null) p.set('band', s.band);
  if (s.min != null) p.set('min', String(s.min));
  if (s.max != null) p.set('max', String(s.max));
  if (s.authentic) p.set('authentic', '1');
  if (s.freeship) p.set('freeship', '1');
  if (s.voucher) p.set('voucher', '1');
  if (s.rating) p.set('rating', String(s.rating));
  if (s.sort !== 'rel') p.set('sort', s.sort);
  if (s.sort === 'price' && s.dir === 'desc') p.set('dir', 'desc');
  if (s.page > 1) p.set('page', String(s.page));
  return p;
}

/**
 * Liên kết tới trạng thái mới. Đổi bất kỳ thứ gì ngoài `page` thì quay về trang 1
 * (prototype: setPage(1) khi lọc / sắp xếp đổi).
 */
export function filtersHref(
  path: string,
  s: FilterState,
  patch: Partial<FilterState> = {},
): string {
  const next: FilterState = { ...s, ...patch };
  if (!('page' in patch)) next.page = 1;
  const qs = serializeFilters(next).toString();
  return qs ? `${path}?${qs}` : path;
}

/** Khoảng giá đang áp dụng: ô tự nhập thắng khoảng chọn sẵn (prototype lo/hi). */
export function priceRange(
  s: Pick<FilterState, 'band' | 'min' | 'max'>,
  bands: readonly PriceBand[],
): { min?: number; max?: number } {
  const b = bands.find((x) => x.id === s.band);
  const min = s.min ?? b?.min;
  const max = s.max ?? b?.max ?? undefined;
  return {
    ...(min ? { min } : {}),
    ...(max != null ? { max } : {}),
  };
}

/** Sắp xếp gửi backend. */
export function sortParam(
  s: Pick<FilterState, 'sort' | 'dir'>,
): 'rel' | 'hot' | 'new' | 'price_asc' | 'price_desc' {
  if (s.sort === 'price') return s.dir === 'desc' ? 'price_desc' : 'price_asc';
  return s.sort;
}

/** Bấm nút sắp xếp: bấm lại "Giá" đang chọn thì đảo chiều (prototype setAsc(!asc)). */
export function nextSort(
  s: Pick<FilterState, 'sort' | 'dir'>,
  id: SortId,
): { sort: SortId; dir: SortDir } {
  if (id === 'price' && s.sort === 'price')
    return { sort: 'price', dir: s.dir === 'asc' ? 'desc' : 'asc' };
  return { sort: id, dir: 'asc' };
}

export type ChipKind =
  | 'sub'
  | 'cat'
  | 'brand'
  | 'place'
  | 'attr'
  | 'price'
  | 'authentic'
  | 'freeship'
  | 'voucher'
  | 'rating';

export interface FilterChip {
  kind: ChipKind;
  /** Giá trị gốc (slug / tên / số sao); UI tự dịch thành nhãn. */
  value: string;
}

/** Thẻ "đang lọc" theo thứ tự prototype (danh mục → thương hiệu → giá → dịch vụ → sao). */
export function activeChips(s: FilterState): FilterChip[] {
  const out: FilterChip[] = [];
  if (s.sub) out.push({ kind: 'sub', value: s.sub });
  s.cats.forEach((v) => out.push({ kind: 'cat', value: v }));
  s.brands.forEach((v) => out.push({ kind: 'brand', value: v }));
  s.places.forEach((v) => out.push({ kind: 'place', value: v }));
  s.attrs.forEach((v) => out.push({ kind: 'attr', value: v }));
  if (s.band || s.min != null || s.max != null)
    out.push({ kind: 'price', value: s.band ?? 'custom' });
  if (s.authentic) out.push({ kind: 'authentic', value: '1' });
  if (s.freeship) out.push({ kind: 'freeship', value: '1' });
  if (s.voucher) out.push({ kind: 'voucher', value: '1' });
  if (s.rating) out.push({ kind: 'rating', value: String(s.rating) });
  return out;
}

/** Bỏ một thẻ lọc. */
export function withoutChip(s: FilterState, c: FilterChip): Partial<FilterState> {
  const drop = (xs: string[]) => xs.filter((x) => x !== c.value);
  switch (c.kind) {
    case 'sub':
      return { sub: null };
    case 'cat':
      // bỏ danh mục thì thuộc tính ngành của nó không còn ý nghĩa
      return { cats: drop(s.cats), attrs: [] };
    case 'brand':
      return { brands: drop(s.brands) };
    case 'place':
      return { places: drop(s.places) };
    case 'attr':
      return { attrs: drop(s.attrs) };
    case 'price':
      return { band: null, min: null, max: null };
    case 'authentic':
      return { authentic: false };
    case 'freeship':
      return { freeship: false };
    case 'voucher':
      return { voucher: false };
    case 'rating':
      return { rating: 0 };
  }
}

/** "Xoá tất cả": giữ từ khóa, phạm vi, shop và cách sắp xếp. */
export function clearedFilters(s: FilterState): FilterState {
  return { ...EMPTY_FILTERS, q: s.q, scope: s.scope, shop: s.shop, sort: s.sort, dir: s.dir };
}

/** Bật / tắt một giá trị trong nhóm chọn nhiều. */
export const toggleValue = (xs: string[], v: string): string[] =>
  xs.includes(v) ? xs.filter((x) => x !== v) : [...xs, v];
