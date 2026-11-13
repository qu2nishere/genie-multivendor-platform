// Điểm Genie (plan G10.56; API_SPEC 3.13; prototype USER.coins, COIN_LOG, XU_REWARDS). 1 Điểm = 1₫.

export interface CoinSummary {
  balance: number;
  /** Điểm sắp hết hạn trong `withinDays` ngày. */
  expiringSoon: { amount: number; withinDays: number } | null;
  checkIn: {
    streakDays: number;
    checkedInToday: boolean;
    nextCheckInAt: string;
    /** rewards[i] = số Điểm của ngày thứ i+1 trong chuỗi 7 ngày. */
    rewards: number[];
  };
}
