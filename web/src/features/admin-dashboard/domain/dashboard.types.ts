// Tổng quan sàn (API_SPEC 11.2; prototype admin/Dashboard.jsx, data.jsx DASHBOARD).
export type Money = number;

/** Kỳ báo cáo dạng "2026-06". */
export type MonthKey = string;

export interface Compared {
  value: Money;
  previous: Money | null;
}

export interface DashboardFinance {
  month: MonthKey;
  availableMonths: MonthKey[];
  gmv: Compared;
  commission: Compared;
  voucherCost: Compared;
  coinCost: Money;
  gatewayFee: Money;
  netProfit: Money;
  holdings: { escrowHeld: Money; walletBalances: Money };
  pendingMoney: {
    payableWithdrawals: Money;
    blockedWithdrawals: Money;
    codRemittances: Money;
    refundsPending: Money;
    openDisputes: number;
  };
  topShopsByGmv: { shopId: number; name: string; gmv: Money }[];
}

/** 7 ngày cuối kỳ / theo tuần trong tháng / 6 tháng. */
export type GmvRange = '7d' | 'weekly' | '6m';

export interface GmvChart {
  titleKey: 'LAST_7_DAYS' | 'WEEKS_IN_MONTH' | 'LAST_6_MONTHS';
  points: { label: string; value: Money }[];
}

export type MovingGroup = 'AWAITING_PAYMENT' | 'PAID' | 'PREPARING' | 'DELIVERING' | 'DELIVERED';
export type FinishedGroup = 'COMPLETED' | 'CANCELLED' | 'RETURNED_REFUNDED';

export interface Operations {
  inProgress: { group: MovingGroup; count: number }[];
  finished: { group: FinishedGroup; count: number }[];
  topProducts: { productId: number; name: string; sold: number }[];
}

export interface GrowingShop {
  shop: { id: number; name: string; logoUrl: string | null };
  categoryName: string;
  sold: number;
  rating: number;
  growthPercent: number;
  /** Chỉ có khi người xem có VIEW_FINANCE. */
  gmv: Money | null;
}
