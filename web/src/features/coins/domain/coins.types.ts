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

export type CoinTxnType =
  | 'CHECK_IN'
  | 'ORDER_CASHBACK'
  | 'REVIEW_BONUS'
  | 'USED_AT_CHECKOUT'
  | 'ORDER_REFUND'
  | 'VOUCHER_REDEEM';

export interface CoinTxn {
  id: number;
  type: CoinTxnType | string;
  description: string;
  /** Âm khi tiêu. */
  amount: number;
  createdAt: string;
}

/** Quà đổi Điểm (prototype XU_REWARDS → POINT_REWARDS; GET /public/coin-rewards). */
export interface CoinReward {
  id: number;
  cost: number;
  voucherValue: number;
  minOrder: number;
}
