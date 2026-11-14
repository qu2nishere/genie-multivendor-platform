import { describe, expect, it } from 'vitest';
import { badgeLabel, totalJobs } from './queues.rules';

describe('queues.rules', () => {
  it('cộng các hàng chờ có quyền, bỏ qua lệnh bị chặn', () => {
    expect(
      totalJobs({
        kycPending: 3,
        productsPending: 3,
        disputesOpen: 2,
        withdrawalsPending: 4,
        withdrawalsBlocked: 1,
        codRemittancesWaiting: 2,
      }),
    ).toBe(14);
    expect(totalJobs({ disputesOpen: 2 })).toBe(2);
    expect(totalJobs(undefined)).toBe(0);
  });
  it('nhãn huy hiệu', () => {
    expect(badgeLabel(0)).toBeNull();
    expect(badgeLabel(undefined)).toBeNull();
    expect(badgeLabel(4)).toBe('4');
    expect(badgeLabel(120)).toBe('99+');
  });
});
