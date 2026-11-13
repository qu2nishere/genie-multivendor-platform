// DTO snake_case — GET /seller/dashboard (API_SPEC 10.2).
import type { DashOutcome, ProductState } from '../domain/dashboard.types';

export interface DashboardDto {
  as_of: string;
  work_queue: {
    orders_to_confirm: number;
    orders_to_prepare: number;
    orders_delivery_failed: number;
    return_requests: { count: number; nearest_deadline_at: string | null };
    unread_chats: number;
  };
  money: { available: number; held: number; pending_withdrawal: number; cod_pending: number };
  orders_30d: {
    count: number;
    previous_count: number;
    visits: number;
    previous_visits: number;
    outcomes: Record<DashOutcome, number>;
  };
  products: Record<ProductState, number>;
}
