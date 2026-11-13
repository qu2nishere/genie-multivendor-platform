import { describe, expect, it } from 'vitest';
import {
  activeChips,
  CATEGORY_BANDS,
  clearedFilters,
  EMPTY_FILTERS,
  filtersHref,
  nextSort,
  parseFilters,
  priceRange,
  SEARCH_BANDS,
  serializeFilters,
  sortParam,
  toggleValue,
  withoutChip,
} from './filters';

const parse = (qs: string) => parseFilters(new URLSearchParams(qs));

describe('parseFilters / serializeFilters', () => {
  it('URL rỗng → mặc định', () => {
    expect(parse('')).toEqual(EMPTY_FILTERS);
    expect(serializeFilters(EMPTY_FILTERS).toString()).toBe('');
  });

  it('đọc và ghi lại đủ tham số (round-trip)', () => {
    const qs =
      'q=tai+nghe&cat=dien-tu&brand=Sony&brand=JBL&place=TP.+HCM&attr=12+th%C3%A1ng&min=100000&max=500000&authentic=1&freeship=1&voucher=1&rating=4&sort=price&dir=desc&page=3';
    const s = parse(qs);
    expect(s).toMatchObject({
      q: 'tai nghe',
      cats: ['dien-tu'],
      brands: ['Sony', 'JBL'],
      places: ['TP. HCM'],
      attrs: ['12 tháng'],
      min: 100000,
      max: 500000,
      authentic: true,
      freeship: true,
      voucher: true,
      rating: 4,
      sort: 'price',
      dir: 'desc',
      page: 3,
    });
    expect(parse(serializeFilters(s).toString())).toEqual(s);
  });

  it('bỏ qua giá trị lạ', () => {
    const s = parse('sort=abc&rating=2&page=-4&min=1e9&scope=shop&brand=&brand=Sony&brand=Sony');
    expect(s.sort).toBe('rel');
    expect(s.rating).toBe(0);
    expect(s.page).toBe(1);
    expect(s.min).toBeNull();
    // scope shop cần shop id
    expect(s.scope).toBe('all');
    expect(s.brands).toEqual(['Sony']);
  });

  it('khoảng tự nhập thì không ghi band', () => {
    const s = { ...EMPTY_FILTERS, band: 'b2', min: 1000 };
    expect(serializeFilters(s).has('band')).toBe(false);
  });

  it('chiều giá chỉ ghi khi sắp theo giá', () => {
    expect(serializeFilters({ ...EMPTY_FILTERS, dir: 'desc' }).has('dir')).toBe(false);
  });
});

describe('filtersHref', () => {
  const s = { ...EMPTY_FILTERS, q: 'ao', page: 4 };
  it('đổi lọc thì về trang 1', () => {
    expect(filtersHref('/search', s, { freeship: true })).toBe('/search?q=ao&freeship=1');
  });
  it('đổi trang giữ lọc', () => {
    expect(filtersHref('/search', s, { page: 5 })).toBe('/search?q=ao&page=5');
  });
  it('không còn tham số → đường dẫn trần', () => {
    expect(filtersHref('/c/dien-tu', EMPTY_FILTERS)).toBe('/c/dien-tu');
  });
});

describe('priceRange', () => {
  it('theo khoảng chọn sẵn', () => {
    expect(priceRange({ band: 'b2', min: null, max: null }, CATEGORY_BANDS)).toEqual({
      min: 100000,
      max: 500000,
    });
    expect(priceRange({ band: 'b5', min: null, max: null }, SEARCH_BANDS)).toEqual({
      min: 5000000,
    });
    expect(priceRange({ band: 'b1', min: null, max: null }, SEARCH_BANDS)).toEqual({ max: 100000 });
  });
  it('ô tự nhập thắng', () => {
    expect(priceRange({ band: 'b2', min: 50, max: null }, CATEGORY_BANDS)).toEqual({
      min: 50,
      max: 500000,
    });
  });
  it('không lọc giá', () => {
    expect(priceRange({ band: null, min: null, max: null }, CATEGORY_BANDS)).toEqual({});
  });
});

describe('sắp xếp', () => {
  it('sortParam', () => {
    expect(sortParam({ sort: 'price', dir: 'asc' })).toBe('price_asc');
    expect(sortParam({ sort: 'price', dir: 'desc' })).toBe('price_desc');
    expect(sortParam({ sort: 'hot', dir: 'desc' })).toBe('hot');
  });
  it('bấm lại Giá thì đảo chiều', () => {
    expect(nextSort({ sort: 'price', dir: 'asc' }, 'price')).toEqual({
      sort: 'price',
      dir: 'desc',
    });
    expect(nextSort({ sort: 'hot', dir: 'asc' }, 'price')).toEqual({ sort: 'price', dir: 'asc' });
    expect(nextSort({ sort: 'price', dir: 'desc' }, 'new')).toEqual({ sort: 'new', dir: 'asc' });
  });
});

describe('thẻ đang lọc', () => {
  const s = parse(
    'q=x&sub=tai-nghe&cat=dien-tu&brand=Sony&band=b1&freeship=1&rating=5&attr=12+th%C3%A1ng',
  );
  it('liệt kê theo thứ tự', () => {
    expect(activeChips(s).map((c) => c.kind)).toEqual([
      'sub',
      'cat',
      'brand',
      'attr',
      'price',
      'freeship',
      'rating',
    ]);
  });
  it('bỏ từng thẻ', () => {
    expect(withoutChip(s, { kind: 'brand', value: 'Sony' })).toEqual({ brands: [] });
    expect(withoutChip(s, { kind: 'price', value: 'b1' })).toEqual({
      band: null,
      min: null,
      max: null,
    });
    expect(withoutChip(s, { kind: 'cat', value: 'dien-tu' })).toEqual({ cats: [], attrs: [] });
  });
  it('xoá tất cả giữ từ khóa và sắp xếp', () => {
    const c = clearedFilters({ ...s, sort: 'hot' });
    expect(activeChips(c)).toEqual([]);
    expect(c.q).toBe('x');
    expect(c.sort).toBe('hot');
  });
  it('toggleValue', () => {
    expect(toggleValue(['a'], 'b')).toEqual(['a', 'b']);
    expect(toggleValue(['a', 'b'], 'a')).toEqual(['b']);
  });
});
