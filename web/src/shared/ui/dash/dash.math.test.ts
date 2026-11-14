import { describe, expect, it } from 'vitest';
import { formatMoneyShort } from '@/shared/lib/format';
import { axisTicks, change, niceStep, shareOf } from './dash.math';

// Plan G06.34 — niceStep, rút gọn số, phần trăm thay đổi
describe('dash.math', () => {
  it('niceStep chọn 1 / 2 / 2,5 / 5 / 10 × 10^k', () => {
    expect(niceStep(100, 4)).toBe(25);
    expect(niceStep(90, 4)).toBe(25);
    expect(niceStep(1_840_000_000, 4)).toBe(500_000_000);
    expect(niceStep(7, 4)).toBe(2);
    expect(niceStep(0, 4)).toBe(1); // như prototype: không có dữ liệu vẫn có bước 1
  });

  it('axisTicks bao trùm giá trị lớn nhất', () => {
    const { top, ticks } = axisTicks(45_800_000, 4);
    expect(top).toBeGreaterThanOrEqual(45_800_000);
    expect(ticks[0]).toBe(0);
    expect(ticks.at(-1)).toBe(top);
  });

  it('change: phần trăm có dấu, null khi không có kỳ trước', () => {
    expect(change(120, 100)).toBeCloseTo(20);
    expect(change(80, 100)).toBeCloseTo(-20);
    expect(change(5, 0)).toBeNull();
    expect(change(5, null)).toBeNull();
  });

  it('shareOf an toàn khi tổng bằng 0', () => {
    expect(shareOf(1, 4)).toBe(25);
    expect(shareOf(1, 0)).toBe(0);
  });

  it('formatMoneyShort như trục prototype', () => {
    expect(formatMoneyShort(1_840_000_000, { locale: 'vi' })).toBe('1,84 tỷ');
    expect(formatMoneyShort(45_800_000, { locale: 'vi' })).toBe('45,8 tr');
    expect(formatMoneyShort(950_000, { locale: 'vi' })).toBe('950k');
    expect(formatMoneyShort(500, { locale: 'vi' })).toBe('500');
    expect(formatMoneyShort(45_800_000, { locale: 'en' })).toBe('45.8M');
  });
});
