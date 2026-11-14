import { describe, expect, it } from 'vitest';
import { isFreeByShop, makeShipFn, shipFee, shipOf, type ShipPolicy } from './shipping.rules';

const policies: Record<number, ShipPolicy> = {
  1: { fee: 25000, freeFrom: 199000 },
  2: { fee: 18000, freeFrom: null },
};

describe('shipping.rules', () => {
  it('phí cố định, shop tự miễn phí khi đạt ngưỡng', () => {
    expect(shipFee(policies[1], 198999)).toBe(25000);
    expect(shipFee(policies[1], 199000)).toBe(0);
    expect(shipFee(policies[2], 10_000_000)).toBe(18000);
    expect(shipFee(undefined, 1)).toBe(0);
    expect(isFreeByShop(policies[1], 250000)).toBe(true);
    expect(isFreeByShop(policies[2], 250000)).toBe(false);
  });

  it('tính theo tiền hàng của đúng shop', () => {
    const lines = [
      { shopId: 1, price: 150000, qty: 1 },
      { shopId: 1, price: 60000, qty: 1 },
      { shopId: 2, price: 259000, qty: 2 },
    ];
    expect(makeShipFn(policies)(1, lines)).toBe(0);
    expect(makeShipFn(policies)(2, lines)).toBe(18000);
    expect(shipOf(lines, policies)).toBe(18000);
    expect(shipOf(lines.slice(0, 1), policies)).toBe(25000);
  });
});
