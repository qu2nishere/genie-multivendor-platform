import { describe, expect, it } from 'vitest';
import { canOrderFrom, isOnVacation, shopHref, shopSearchHref, shopShipFee } from './shop.rules';

describe('shop.rules', () => {
  it('đường dẫn', () => {
    expect(shopHref(3)).toBe('/shop/3');
    expect(shopSearchHref(1, ' tai nghe ')).toBe('/search?q=tai%20nghe&scope=shop&shop=1');
  });
  it('tạm nghỉ chặn đặt hàng', () => {
    expect(isOnVacation({ vacation: { until: '15/07/2026', message: '' } })).toBe(true);
    expect(canOrderFrom({ vacation: null })).toBe(true);
  });
  it('phí ship theo ngưỡng miễn phí của shop', () => {
    expect(shopShipFee({ shipFee: 25000, freeShipFrom: 199000 }, 200000)).toBe(0);
    expect(shopShipFee({ shipFee: 25000, freeShipFrom: 199000 }, 100000)).toBe(25000);
    expect(shopShipFee({ shipFee: 18000, freeShipFrom: null }, 999999)).toBe(18000);
  });
});
