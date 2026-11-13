import { describe, expect, it } from 'vitest';
import { canRedeem, checkInDays, rewardLabel, rewardOf } from './coins.rules';

const ci = {
  streakDays: 3,
  checkedInToday: false,
  nextCheckInAt: '',
  rewards: [100, 100, 100, 100, 100, 100, 1000],
};

describe('coins.rules', () => {
  it('7 ô điểm danh', () => {
    expect(checkInDays(ci)).toEqual(['done', 'done', 'done', 'today', 'next', 'next', 'next']);
    expect(checkInDays({ ...ci, streakDays: 4, checkedInToday: true })[4]).toBe('next');
  });
  it('thưởng & đổi quà', () => {
    expect(rewardOf(ci, 7)).toBe(1000);
    expect(rewardLabel(1000)).toBe('+1k');
    expect(rewardLabel(100)).toBe('+100');
    expect(canRedeem(1250, 1000)).toBe(true);
    expect(canRedeem(1250, 2000)).toBe(false);
  });
});
