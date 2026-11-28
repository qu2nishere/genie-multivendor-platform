import { describe, expect, it } from 'vitest';
import {
  flashIssues,
  genCode,
  keepOriginalIso,
  lockedKeys,
  maxFlashPrice,
  normalizeCode,
  voucherActions,
  voucherFieldOf,
  voucherStartIso,
} from './promo.rules';
import { voucherSchema, voucherSchemaFor } from './promo.schema';

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
    title: '',
    fundedBy: 'PLATFORM' as const,
    platformSharePercent: null,
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

describe('voucherSchemaFor platform (voucher sàn G12.33)', () => {
  const ok = {
    code: 'GENIE50',
    discountType: 'PERCENTAGE' as const,
    discountValue: 50,
    maxDiscount: 100000,
    minOrderValue: 500000,
    usageLimit: 10000,
    usagePerUser: 1,
    budget: 500000000,
    validFrom: '2026-06-15',
    validTo: '2026-06-30',
    scope: 'ALL' as const,
    categoryIds: [],
    productIds: [],
    excludedCategoryIds: [1],
    title: 'Siêu Sale Giữa Tháng',
    fundedBy: 'PLATFORM' as const,
    platformSharePercent: null,
  };
  const schema = voucherSchemaFor('platform', { isNew: true, today: '2026-06-10' });
  const msgs = (v: object) => {
    const r = schema.safeParse({ ...ok, ...v });
    return r.success ? [] : r.error.issues.map((i) => i.message);
  };
  it('hợp lệ', () => expect(msgs({})).toEqual([]));
  it('bắt buộc tên, chặn phạm vi sản phẩm, đồng tài trợ cần tỷ lệ, không bắt đầu ở quá khứ', () => {
    expect(msgs({ title: ' ' })).toEqual(['seller-promotions.form.err.title']);
    expect(msgs({ scope: 'PRODUCT', productIds: [1] })).toEqual([
      'seller-promotions.form.err.scope_platform',
    ]);
    expect(msgs({ fundedBy: 'CO_SPONSOR' })).toEqual(['seller-promotions.form.err.share']);
    expect(msgs({ fundedBy: 'CO_SPONSOR', platformSharePercent: 50 })).toEqual([]);
    expect(msgs({ validFrom: '2026-06-01' })).toEqual(['seller-promotions.form.err.past']);
  });
  it('sửa voucher cũ được giữ ngày bắt đầu đã qua', () => {
    const edit = voucherSchemaFor('platform', { isNew: false, today: '2026-06-20' });
    expect(edit.safeParse(ok).success).toBe(true);
  });
});

describe('voucher — nối API thật (plan 2026-11-26 voucher V06)', () => {
  it('đổi tên trường lỗi / khóa của API sang ô form', () => {
    expect(voucherFieldOf('min_order_value')).toBe('minOrderValue');
    expect(voucherFieldOf('platform_share_percent')).toBe('platformSharePercent');
    expect(voucherFieldOf('status')).toBeNull();
    expect([...lockedKeys(['code', 'discount_value', 'scope', 'status'])]).toEqual([
      'code',
      'discountValue',
      'scope',
    ]);
  });

  it('bắt đầu hôm nay gửi thời điểm hiện tại, ngày khác gửi 00:00 giờ VN', () => {
    const now = new Date('2026-06-08T03:15:00Z');
    expect(voucherStartIso('2026-06-08', '2026-06-08', now)).toBe('2026-06-08T03:15:00.000Z');
    expect(voucherStartIso('2026-06-10', '2026-06-08', now)).toBe('2026-06-10T00:00:00+07:00');
    expect(voucherStartIso('', '2026-06-08', now)).toBe('');
  });
});

describe('sửa voucher đang chạy — giữ mốc giờ gốc (lỗi chạy thật V09.04)', () => {
  const dayOf = (iso: string) => iso.slice(0, 10);
  it('ngày không đổi → gửi lại ISO gốc; đổi ngày → giá trị mới', () => {
    const iso = '2026-11-28T05:32:54.045Z';
    expect(keepOriginalIso('2026-11-28', iso, dayOf, 'NEW')).toBe(iso);
    expect(keepOriginalIso('2026-12-01', iso, dayOf, 'NEW')).toBe('NEW');
    expect(keepOriginalIso('2026-11-28', undefined, dayOf, 'NEW')).toBe('NEW');
  });
});
