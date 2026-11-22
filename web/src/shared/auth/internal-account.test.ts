import { describe, expect, it } from 'vitest';
import { isBuyerOnlyPath, isInternalUser } from './session';

// Tài khoản nội bộ chỉ xem storefront, không mua hàng (plan B1 D16 hướng B).
describe('tài khoản nội bộ', () => {
  it('nhận diện STAFF / ADMIN', () => {
    expect(isInternalUser({ roles: ['ADMIN'] })).toBe(true);
    expect(isInternalUser({ roles: ['STAFF'] })).toBe(true);
    expect(isInternalUser({ roles: ['CUSTOMER', 'VENDOR'] })).toBe(false);
    expect(isInternalUser(null)).toBe(false);
  });

  it('trang chỉ dành cho người mua', () => {
    expect(isBuyerOnlyPath('/cart')).toBe(true);
    expect(isBuyerOnlyPath('/checkout/result')).toBe(true);
    expect(isBuyerOnlyPath('/orders/12')).toBe(true);
    expect(isBuyerOnlyPath('/account/profile')).toBe(true);
    expect(isBuyerOnlyPath('/')).toBe(false);
    expect(isBuyerOnlyPath('/p/tai-nghe-1')).toBe(false);
    expect(isBuyerOnlyPath('/cartoon')).toBe(false);
  });
});
