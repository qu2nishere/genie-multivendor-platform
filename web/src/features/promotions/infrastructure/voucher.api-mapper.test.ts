// $Voucher (API) → Voucher (UI) — plan 2026-11-26 voucher V08.04
import { describe, expect, it } from 'vitest';
import type { ApiVoucherDto } from './voucher.api-dto';
import { amountLabelOf, fromApiVoucher, kindOf, shortMoney } from './voucher.api-mapper';

const dto = (over: Partial<ApiVoucherDto>): ApiVoucherDto => ({
  id: 7,
  code: 'GENIE50',
  title: 'Siêu Sale',
  type: 'PLATFORM',
  discount_type: 'PERCENTAGE',
  discount_value: 15,
  max_discount: 50000,
  min_order_value: 200000,
  shop: null,
  scope: 'ALL',
  included_categories: [],
  excluded_categories: [],
  valid_from: '2026-06-01T00:00:00+07:00',
  valid_to: '2026-06-20T23:59:59+07:00',
  state: 'AVAILABLE',
  usage: { used: 3, limit: 100 },
  funded_by: 'PLATFORM',
  description: null,
  is_saved: null,
  used_order_code: null,
  ...over,
});

describe('voucher api mapper', () => {
  it('voucher sàn %: nhãn, ngày giờ VN, trạng thái', () => {
    const v = fromApiVoucher(dto({}));
    expect(v).toMatchObject({
      id: '7',
      kind: 'platform',
      type: 'PERCENT',
      amountLabel: '-15%',
      shopId: null,
      scopeLabel: 'Toàn sàn',
      sponsorLabel: 'Genie tài trợ',
      validFrom: '2026-06-01',
      expiresAt: '2026-06-20',
      status: 'active',
      saved: false,
      categories: null,
      usageLimit: 100,
    });
    expect(v.conditionLabel).toMatch(/^Đơn từ 200\.000/);
    expect(fromApiVoucher(dto({ min_order_value: 0 })).conditionLabel).toBe('Mọi đơn hàng');
    expect(fromApiVoucher(dto({ state: 'UPCOMING' })).status).toBe('upcoming');
  });

  it('voucher shop: shop_id là vendor_profiles.id, tên shop, ngành áp dụng / loại trừ', () => {
    const v = fromApiVoucher(
      dto({
        type: 'SHOP',
        discount_type: 'FIXED_AMOUNT',
        discount_value: 30000,
        max_discount: null,
        shop: { id: 15, name: 'TechZone' },
        funded_by: 'VENDOR',
        excluded_categories: ['Điện thoại'],
        is_saved: true,
      }),
    );
    expect(v).toMatchObject({
      kind: 'shop',
      type: 'FIXED',
      amountLabel: '-30k',
      shopId: 15,
      scopeLabel: 'Shop TechZone',
      sponsorLabel: 'TechZone',
      excluded: ['Điện thoại'],
      saved: true,
    });
  });

  it('freeship (kể cả voucher shop freeship) gom vào nhóm Freeship', () => {
    expect(kindOf({ type: 'SHOP', discount_type: 'FREESHIP' })).toBe('freeship');
    expect(kindOf({ type: 'FREESHIP', discount_type: 'FREESHIP' })).toBe('freeship');
    const f = fromApiVoucher(
      dto({
        type: 'FREESHIP',
        discount_type: 'FREESHIP',
        discount_value: null,
        max_discount: 30000,
      }),
    );
    expect(f.type).toBe('FREESHIP');
    expect(f.value).toBe(30000);
    expect(amountLabelOf(dto({ discount_type: 'FREESHIP', max_discount: null }))).toBe('Freeship');
  });

  it('đã dùng: mã đơn; số tiền rút gọn', () => {
    const u = fromApiVoucher(dto({ state: 'USED', used_order_code: 'TR-20260608-000044' }));
    expect(u.status).toBe('used');
    expect(u.usedOrderCode).toBe('TR-20260608-000044');
    expect(shortMoney(1_500_000)).toBe('1,5tr');
    expect(shortMoney(30000)).toBe('30k');
    expect(amountLabelOf(dto({ discount_value: 7.5 }))).toBe('-7,5%');
  });
});
