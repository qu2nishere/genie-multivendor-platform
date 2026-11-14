// GET /me/coins (API_SPEC 3.13).
export interface CoinSummaryDto {
  balance: number;
  expiring_soon: { amount: number; within_days: number } | null;
  check_in: {
    streak_days: number;
    checked_in_today: boolean;
    next_check_in_at: string;
    rewards: number[];
  };
}
