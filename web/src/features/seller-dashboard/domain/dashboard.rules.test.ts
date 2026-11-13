import { describe, expect, it } from 'vitest';
import { backlog, hoursLeft, moneyTotal } from './dashboard.rules';

describe('dashboard.rules', () => {
  const now = Date.parse('2026-06-08T18:00:00+07:00');
  it('giờ còn lại làm tròn lên', () => {
    expect(hoursLeft('2026-06-09T06:00:00+07:00', now)).toBe(12);
    expect(hoursLeft('2026-06-08T18:20:00+07:00', now)).toBe(1);
    expect(hoursLeft('2026-06-08T10:00:00+07:00', now)).toBe(0);
    expect(hoursLeft(null, now)).toBeNull();
  });
  it('tổng việc tồn = 12 như bản mẫu', () => {
    expect(
      backlog({
        toConfirm: 2,
        toPrepare: 2,
        deliveryFailed: 1,
        returns: { count: 2, nearestDeadlineAt: null },
        unreadChats: 5,
      }),
    ).toBe(12);
  });
  it('tổng tiền 4 ngăn', () => {
    expect(
      moneyTotal({
        available: 12_450_000,
        held: 1_907_600,
        pendingWithdrawal: 5_000_000,
        codPending: 1_950_000,
      }),
    ).toBe(21_307_600);
  });
});
