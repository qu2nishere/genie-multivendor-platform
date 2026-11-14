import { describe, expect, it } from 'vitest';
import { flashIssues, genCode, maxFlashPrice, normalizeCode, voucherActions } from './promo.rules';
import { voucherSchema } from './promo.schema';

describe('promo.rules', () => {
  it('sinh mã 8 ký tự hợp lệ, chuẩn hóa mã nhập', () => {
    const c = genCode(() => 0.5);
    expect(c).toMatch(/^[A-Z0-9]{8}$/);
    expect(normalizeCode('summer-2026!')).toBe('SUMMER2026');
  });
  it('giá Flash tối đa 85%, kiểm suất', () => {
    expect(maxFlashPrice(389000)).toBe(330650);
    const p = { maxFlashPrice: 330650, stock: 38 };
    expect(flashIssues(p, 299000, 30)).toEqual([]);
    expect(flashIssues(p, 340000, 39)).toEqual(['price', 'stock']);
    expect(flashIssues(p, null, 0)).toEqual(['price', 'stock']);
  });
  it('thao tác theo trạng thái', () => {
    expect(voucherActions('ACTIVE')).toEqual(['EDIT', 'DUPLICATE', 'DISABLE']);
    expect(voucherActions('ENDED')).toEqual(['DUPLICATE']);
  });
});

describe('voucherSchema', () => {
  const ok = {
    code: 'SUMMER2026',
    discountType: 'PERCENTAGE' as const,
    discountValue: 15,
    maxDiscount: 50000,
    minOrderValue: 200000,
    usageLimit: 500,
    usagePerUser: 1,
    budget: null,
    validFrom: '2026-06-10',
    validTo: '2026-06-30',
    scope: 'ALL' as const,
    categoryIds: [],
    productIds: [],
    excludedCategoryIds: [],
  };
  const msgs = (v: object) => {
    const r = voucherSchema.safeParse({ ...ok, ...v });
    return r.success ? [] : r.error.issues.map((i) => i.message);
  };
  it('hợp lệ', () => expect(msgs({})).toEqual([]));
  it('báo lỗi', () => {
    expect(msgs({ code: 'abc' })).toContain('seller-promotions.form.err.code');
    expect(msgs({ discountValue: 120 })).toContain('seller-promotions.form.err.percent');
    expect(msgs({ discountType: 'FREESHIP', discountValue: null })).toEqual([]);
    expect(msgs({ validTo: '2026-06-01' })).toContain('seller-promotions.form.err.end');
    expect(msgs({ scope: 'PRODUCT' })).toContain('seller-promotions.form.err.product');
    expect(msgs({ usageLimit: null })).toContain('seller-promotions.form.err.total');
  });
});
