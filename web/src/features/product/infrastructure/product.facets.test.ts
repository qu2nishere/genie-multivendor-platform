// Facets của API 5.3 → bộ lọc web: thuộc tính ngành (D9) và cờ "Shop có voucher" (D7) — plan 2026-11-24 H11.
import { describe, expect, it, vi } from 'vitest';

const request = vi.fn();
vi.mock('@/shared/api/http', () => ({ request: (...args: unknown[]) => request(...args) }));

const { toApiQuery, getFacets } = await import('./product.api');
const { fromFacetsDto } = await import('./product.mapper');
const { facetsOf, mockAttributeOf, queryProducts } = await import('./product.mock');

const FACETS = {
  brands: [{ id: 3, name: 'Sony', count: 2 }],
  locations: [{ province_id: 202, name: 'TP. HCM', count: 9 }],
  attribute: {
    code: 'warranty',
    name: 'Bảo hành',
    values: [
      { value: '12 tháng', count: 4 },
      { value: '24 tháng', count: 1 },
    ],
  },
  has_shop_voucher_supported: true,
};

describe('fromFacetsDto', () => {
  it('giữ mã thuộc tính, tên, giá trị và cờ hỗ trợ lọc voucher', () => {
    const f = fromFacetsDto(FACETS, 12);
    expect(f.brands).toEqual(['Sony']);
    expect(f.locations).toEqual(['TP. HCM']);
    expect(f.attribute).toEqual({
      code: 'warranty',
      name: 'Bảo hành',
      values: ['12 tháng', '24 tháng'],
    });
    expect(f.voucherFilterSupported).toBe(true);
    expect(f.total).toBe(12);
  });

  it('không có facet → không thuộc tính, ẩn ô "Shop có voucher"', () => {
    const f = fromFacetsDto(null, 0);
    expect(f.attribute).toBeNull();
    expect(f.voucherFilterSupported).toBe(false);
    expect(
      fromFacetsDto({ ...FACETS, attribute: null, has_shop_voucher_supported: false }, 3),
    ).toMatchObject({
      attribute: null,
      voucherFilterSupported: false,
    });
  });
});

describe('toApiQuery', () => {
  it('lọc thuộc tính gửi attr.<mã> lấy từ facets của phạm vi, kèm has_shop_voucher', async () => {
    request.mockReset();
    request.mockResolvedValue({
      data: [],
      summary: { facets: FACETS },
      pagination: { total_items: 0 },
    });

    const q = await toApiQuery({ categoryId: 7, attributes: ['12 tháng'], hasVoucher: true });

    expect(q['attr.warranty']).toEqual(['12 tháng']);
    expect(q.has_shop_voucher).toBe(true);
    expect(q.category_id).toBe(7);
    // Lượt hỏi facets cùng phạm vi (danh mục) để biết mã thuộc tính
    expect(request).toHaveBeenCalledWith(
      '/public/products',
      expect.objectContaining({
        query: expect.objectContaining({ category_id: 7, include_facets: true }),
      }),
    );
  });

  it('không chọn voucher → không gửi has_shop_voucher; không có thuộc tính trong facets → bỏ lọc thuộc tính', async () => {
    request.mockReset();
    request.mockResolvedValue({
      data: [],
      summary: { facets: { ...FACETS, attribute: null } },
      pagination: {},
    });

    const q = await toApiQuery({ categoryId: 7, attributes: ['12 tháng'] });

    expect(q.has_shop_voucher).toBeUndefined();
    expect(Object.keys(q).some((k) => k.startsWith('attr.'))).toBe(false);
  });
});

describe('mock khớp API', () => {
  it('facets mock có thuộc tính của danh mục gốc và luôn hỗ trợ lọc voucher', () => {
    const f = facetsOf(queryProducts({ categoryId: 1 }), 1);
    expect(f.attribute).toEqual(mockAttributeOf(1));
    expect(f.attribute?.name).toBe('Bảo hành');
    expect(f.attribute?.code).toMatch(/^[a-z_]+$/);
    expect(f.voucherFilterSupported).toBe(true);
    expect(facetsOf(queryProducts({})).attribute).toBeNull();
  });

  it('getFacets (mock) — có danh mục thì có thuộc tính, không có thì không', async () => {
    expect((await getFacets({ categoryId: 1 })).attribute?.values).toContain('12 tháng');
    expect((await getFacets({ q: 'tai nghe' })).attribute).toBeNull();
  });
});
