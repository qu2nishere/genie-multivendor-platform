import { describe, expect, it } from 'vitest';
import {
  bannerStateOf,
  campaignAction,
  canDeleteBanner,
  ctr,
  flashDiscount,
  nextOrder,
  parseEmails,
  parseLink,
  plainText,
  previewPath,
  readRate,
  sitePath,
  usagePercent,
  voucherActions,
} from './marketing.rules';
import { flashSchema, notificationSchema } from './marketing.schema';

describe('voucher sàn', () => {
  it('thao tác theo trạng thái', () => {
    expect(voucherActions({ status: 'ACTIVE', usage: { used: 3420, limit: 10000 } })).toEqual([
      'duplicate',
      'edit',
      'disable',
    ]);
    expect(voucherActions({ status: 'SCHEDULED', usage: { used: 0, limit: 5000 } })).toEqual([
      'duplicate',
      'edit',
      'cancel',
    ]);
    expect(voucherActions({ status: 'ENDED', usage: { used: 15000, limit: 15000 } })).toEqual([
      'duplicate',
    ]);
    expect(voucherActions({ status: 'ENDED', usage: { used: 0, limit: 10 } })).toEqual([
      'duplicate',
      'delete',
    ]);
    expect(usagePercent({ used: 3420, limit: 10000 })).toBe(34);
    expect(usagePercent({ used: 20, limit: 10 })).toBe(100);
  });
});

describe('flash sale', () => {
  const item = { referencePrice: 389000, flashPrice: 299000 };
  it('% giảm và luật 15%', () => {
    expect(flashDiscount(item)).toBe(23);
    expect(flashDiscount({ referencePrice: 89000, flashPrice: 82000 })).toBe(8);
  });
  it('lên lịch cần ≥ 1 sản phẩm đã duyệt; đang chạy → kết thúc sớm', () => {
    const it0 = {
      id: 1,
      productId: 1,
      productName: '',
      shopName: '',
      ...item,
      flashStock: 1,
      meetsDiscountRule: true,
    };
    expect(campaignAction({ status: 'DRAFT', items: [{ ...it0, status: 'PENDING' }] })).toEqual({
      kind: 'schedule',
      enabled: false,
    });
    expect(campaignAction({ status: 'DRAFT', items: [{ ...it0, status: 'APPROVED' }] })).toEqual({
      kind: 'schedule',
      enabled: true,
    });
    expect(campaignAction({ status: 'ACTIVE', items: [] })).toEqual({ kind: 'end' });
    expect(campaignAction({ status: 'ENDED', items: [] })).toBeNull();
  });
  it('form: giờ kết thúc sau bắt đầu, hạn đăng ký trước ngày mở', () => {
    const ok = {
      name: 'FLASH SALE 12.12',
      startsAt: '2026-12-12T00:00',
      endsAt: '2026-12-12T02:00',
      maxItemsPerShop: 20,
      registrationDeadline: '2026-12-10',
    };
    expect(flashSchema.safeParse(ok).success).toBe(true);
    expect(flashSchema.safeParse({ ...ok, endsAt: '2026-12-11T23:00' }).success).toBe(false);
    expect(flashSchema.safeParse({ ...ok, registrationDeadline: '2026-12-13' }).success).toBe(
      false,
    );
  });
});

describe('banner', () => {
  it('đọc liên kết đích theo route mới', () => {
    expect(parseLink('/c/dien-tu')).toEqual({ type: 'category', value: 'dien-tu' });
    expect(parseLink('/p/tai-nghe-anc-pro-5-1')).toEqual({ type: 'product', value: '1' });
    expect(parseLink('/shop/5')).toEqual({ type: 'shop', value: '5' });
    expect(parseLink('/#flash-sale')).toEqual({ type: 'flash', value: '' });
    expect(parseLink('/discover')).toEqual({ type: 'discover', value: '' });
    expect(parseLink('/campaign/sieu-sale-12-12')).toEqual({
      type: 'campaign',
      value: '/campaign/sieu-sale-12-12',
    });
  });
  it('chỉ nhận đường dẫn trong miền sàn', () => {
    expect(sitePath('/campaign/x?a=1#b')).toBe('/campaign/x?a=1#b');
    expect(sitePath('https://genie.vn/c/dien-tu')).toBe('/c/dien-tu');
    expect(sitePath('//evil.com/x')).toBeNull();
    expect(sitePath('/\\evil.com')).toBeNull();
    expect(sitePath('javascript:alert(1)')).toBeNull();
    expect(sitePath('https://evil.com')).toBeNull();
    expect(sitePath('  ')).toBeNull();
  });
  it('thứ tự kế tiếp theo ô (danh mục riêng với Đầu trang danh mục)', () => {
    const rows = [
      { id: 'a', position: 'HOME_TOP_CAROUSEL' as const, category: null, sortOrder: 5 },
      {
        id: 'b',
        position: 'CATEGORY_TOP' as const,
        category: { id: 1, name: 'Điện tử', slug: 'dien-tu' },
        sortOrder: 2,
      },
      {
        id: 'c',
        position: 'CATEGORY_TOP' as const,
        category: { id: 3, name: 'Làm đẹp', slug: 'lam-dep' },
        sortOrder: 7,
      },
    ];
    expect(nextOrder(rows, { position: 'HOME_TOP_CAROUSEL', categoryId: null })).toBe(6);
    expect(nextOrder(rows, { position: 'CATEGORY_TOP', categoryId: 1 })).toBe(3);
    expect(nextOrder(rows, { position: 'POPUP', categoryId: null })).toBe(1);
  });
  it('trạng thái, CTR, xóa, trang xem', () => {
    const b = { active: true, startsAt: '2026-11-01', endsAt: '2026-12-12' };
    expect(bannerStateOf(b, '2026-11-14')).toBe('LIVE');
    expect(bannerStateOf(b, '2026-10-01')).toBe('SCHEDULED');
    expect(bannerStateOf(b, '2026-12-13')).toBe('ENDED');
    expect(bannerStateOf({ ...b, active: false }, '2026-11-14')).toBe('OFF');
    expect(ctr({ impressions: 12800, clicks: 940 })).toBeCloseTo(0.0734, 3);
    expect(ctr({ impressions: 0, clicks: 0 })).toBeNull();
    expect(canDeleteBanner({ state: 'LIVE' })).toBe(false);
    expect(canDeleteBanner({ state: 'OFF' })).toBe(true);
    expect(
      previewPath({ position: 'CATEGORY_TOP', category: { id: 1, name: '', slug: 'dien-tu' } }),
    ).toBe('/c/dien-tu');
  });
});

describe('thông báo sàn', () => {
  it('tách email, chữ thuần, tỷ lệ đọc', () => {
    expect(parseEmails('A@x.vn, b@y.vn ,,a@x.vn; sai')).toEqual({
      emails: ['a@x.vn', 'b@y.vn'],
      invalid: ['sai'],
    });
    expect(plainText('<p>Xin&nbsp;<b>chào</b></p>')).toBe('Xin chào');
    expect(readRate({ readCount: 48210, reachCount: 120400 })).toBe(40);
  });
  it('form: nội dung không rỗng, người nhận cụ thể cần email', () => {
    const ok = {
      target: 'ALL' as const,
      emailsText: '',
      title: 'Bảo trì',
      contentHtml: '<p>x</p>',
      sendAt: '',
    };
    expect(notificationSchema.safeParse(ok).success).toBe(true);
    expect(notificationSchema.safeParse({ ...ok, contentHtml: '<p> </p>' }).success).toBe(false);
    expect(notificationSchema.safeParse({ ...ok, target: 'SPECIFIC_USERS' }).success).toBe(false);
  });
});
