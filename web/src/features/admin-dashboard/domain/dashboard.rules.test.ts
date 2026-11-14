import { describe, expect, it } from 'vitest';
import { gmvPointLabel, monthLabel, netProfit, previousMonth } from './dashboard.rules';

describe('dashboard.rules', () => {
  it('lợi nhuận ròng kỳ 06/2026 khớp prototype', () => {
    expect(
      netProfit({
        commission: 92132500,
        voucherCost: 45680000,
        coinCost: 3840000,
        gatewayFee: 5180000,
      }),
    ).toBe(37432500);
  });
  it('kỳ báo cáo', () => {
    const months = ['2026-06', '2026-05', '2026-04'];
    expect(monthLabel('2026-06')).toBe('06/2026');
    expect(previousMonth(months, '2026-06')).toBe('2026-05');
    expect(previousMonth(months, '2026-04')).toBeNull();
  });
  it('nhãn điểm biểu đồ', () => {
    expect(gmvPointLabel('2026-01', '6m', 'vi')).toBe('T1');
    expect(gmvPointLabel('2026-01', '6m', 'en')).toBe('Jan');
    expect(gmvPointLabel('2026-06-24', '7d', 'vi')).toBe('24/6');
    expect(gmvPointLabel('2026-06-01/2026-06-07', 'weekly', 'vi')).toBe('1–7/6');
    expect(gmvPointLabel('2026-05-29/2026-06-04', 'weekly', 'vi')).toBe('29/5–4/6');
  });
});
