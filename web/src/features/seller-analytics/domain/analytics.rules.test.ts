import { describe, expect, it } from 'vitest';
import {
  conversion,
  isoToVnDate,
  pointLabel,
  positiveRate,
  shopCancelRate,
  sumPoints,
} from './analytics.rules';

describe('analytics.rules', () => {
  it('tỉ lệ chuyển đổi khớp bản mẫu (100 đơn / 3.120 lượt = 3,2%)', () => {
    expect(conversion(100, 3120).toFixed(1)).toBe('3.2');
    expect(conversion(5, 0)).toBe(0);
  });
  it('tỉ lệ shop từ chối', () => {
    expect(
      shopCancelRate(
        {
          COMPLETED: 81,
          CANCELLED_BY_CUSTOMER: 4,
          REJECTED_BY_SHOP: 2,
          REFUNDED: 2,
          IN_PROGRESS: 11,
        },
        100,
      ),
    ).toBe(2);
  });
  it('tỉ lệ tích cực', () => {
    expect(positiveRate({ positive: 47, neutral: 9, negative: 8 }).toFixed(1)).toBe('73.4');
  });
  it('nhãn mốc', () => {
    expect(pointLabel('2026-06-08', 'DAY', 'vi')).toBe('08/06');
    expect(pointLabel('2026-06', 'MONTH', 'vi')).toBe('T6');
    expect(pointLabel('2026-06', 'MONTH', 'en')).toBe('Jun');
    expect(pointLabel('09:00', 'HOUR', 'vi')).toBe('9h');
    expect(isoToVnDate('2026-06-08')).toBe('08/06/2026');
  });
  it('cộng chuỗi', () => {
    expect(sumPoints([{ value: 1 }, { value: 2 }])).toBe(3);
  });
});
