import { describe, expect, it } from 'vitest';
import { coinLeftPct, fillPct, MONEY_STEPS } from './escrow';

describe('Money Rail (escrow)', () => {
  it('4 bước theo DESIGN.md: trả tiền → Genie giữ → nhận hàng → shop nhận', () => {
    expect(MONEY_STEPS).toEqual(['pay', 'hold', 'receive', 'release']);
  });
  it('đồng xu nằm giữa từng ô của lưới 4 cột', () => {
    expect([0, 1, 2, 3].map(coinLeftPct)).toEqual([12.5, 37.5, 62.5, 87.5]);
  });
  it('đường đã đi chạy từ 0% tới 100% giữa ô đầu và ô cuối', () => {
    expect(fillPct(0)).toBe(0);
    expect(fillPct(3)).toBe(100);
    expect(fillPct(1)).toBeCloseTo(33.33, 2);
  });
});
