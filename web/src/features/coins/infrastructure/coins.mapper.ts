import type { CoinSummary } from '../domain/coins.types';
import type { CoinSummaryDto } from './coins.dto';

export const toCoinSummary = (d: CoinSummaryDto): CoinSummary => ({
  balance: d.balance,
  expiringSoon: d.expiring_soon
    ? { amount: d.expiring_soon.amount, withinDays: d.expiring_soon.within_days }
    : null,
  checkIn: {
    streakDays: d.check_in.streak_days,
    checkedInToday: d.check_in.checked_in_today,
    nextCheckInAt: d.check_in.next_check_in_at,
    rewards: d.check_in.rewards,
  },
});
