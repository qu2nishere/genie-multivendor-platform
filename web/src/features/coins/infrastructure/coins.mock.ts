// Ví Điểm mẫu (prototype data.jsx USER.coins = 1250).
import type { CoinSummaryDto } from './coins.dto';

export const seedCoinSummary = (): CoinSummaryDto => ({
  balance: 1250,
  expiring_soon: { amount: 150, within_days: 30 },
  check_in: {
    streak_days: 3,
    checked_in_today: false,
    next_check_in_at: '2026-06-23T00:00:00+07:00',
    rewards: [100, 100, 100, 100, 100, 100, 1000],
  },
});
