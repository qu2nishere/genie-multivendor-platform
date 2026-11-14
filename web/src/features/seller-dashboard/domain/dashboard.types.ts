// Tổng quan của shop (API_SPEC 10.2 GET /seller/dashboard; prototype vendor/Overview.jsx).
export type ProductState = 'PUBLISHED' | 'PENDING_APPROVAL' | 'DRAFT' | 'REJECTED';
export type DashOutcome =
  'COMPLETED' | 'CANCELLED_BY_CUSTOMER' | 'REJECTED_BY_SHOP' | 'REFUNDED' | 'IN_PROGRESS';

export interface Dashboard {
  asOf: string;
  work: {
    toConfirm: number;
    toPrepare: number;
    deliveryFailed: number;
    returns: { count: number; nearestDeadlineAt: string | null };
    unreadChats: number;
  };
  money: { available: number; held: number; pendingWithdrawal: number; codPending: number };
  orders30: {
    count: number;
    previous: number;
    visits: number;
    previousVisits: number;
    outcomes: Record<DashOutcome, number>;
  };
  products: Record<ProductState, number>;
}
