// Tổng quan của shop (API_SPEC 10.2). Mock: hàng việc + số sản phẩm đếm từ kho dùng chung
// shared/api/mock/seller.ts (xác nhận đơn ở màn Đơn hàng thì số ở đây đổi theo); tiền, số 30 ngày,
// trả hàng, chat lấy từ bản mẫu (vendor/data.jsx WALLET, PERF, Fulfillment.jsx RETURNS).
import { api } from '@/shared/api/http';
import { mockDelay, USE_MOCK } from '@/shared/api/mock';
import {
  sellerChatUnreadStore,
  sellerMoneyStore,
  sellerOrdersStore,
  sellerProductsStore,
  sellerRefundsStore,
} from '@/shared/api/mock/seller';
import type { Dashboard } from '../domain/dashboard.types';
import type { DashboardDto } from './dashboard.dto';

const toDashboard = (d: DashboardDto): Dashboard => ({
  asOf: d.as_of,
  work: {
    toConfirm: d.work_queue.orders_to_confirm,
    toPrepare: d.work_queue.orders_to_prepare,
    deliveryFailed: d.work_queue.orders_delivery_failed,
    returns: {
      count: d.work_queue.return_requests.count,
      nearestDeadlineAt: d.work_queue.return_requests.nearest_deadline_at,
    },
    unreadChats: d.work_queue.unread_chats,
  },
  money: {
    available: d.money.available,
    held: d.money.held,
    pendingWithdrawal: d.money.pending_withdrawal,
    codPending: d.money.cod_pending,
  },
  orders30: {
    count: d.orders_30d.count,
    previous: d.orders_30d.previous_count,
    visits: d.orders_30d.visits,
    previousVisits: d.orders_30d.previous_visits,
    outcomes: d.orders_30d.outcomes,
  },
  products: d.products,
});

function mockDashboard(): DashboardDto {
  const orders = sellerOrdersStore.get();
  const products = sellerProductsStore.get();
  const count = (s: string) => products.filter((p) => p.status === s).length;
  const open = sellerRefundsStore.get().filter((r) => r.status === 'REQUESTED');
  return {
    as_of: '2026-06-08',
    work_queue: {
      orders_to_confirm: orders.filter((o) => o.status === 'NEW').length,
      orders_to_prepare: orders.filter((o) => o.status === 'CONFIRMED' || o.status === 'PREPARING')
        .length,
      orders_delivery_failed: orders.filter((o) => o.shipment?.status === 'DELIVERY_FAILED').length,
      return_requests: {
        count: open.length,
        nearest_deadline_at:
          open
            .map((r) => r.deadline_at ?? '')
            .filter(Boolean)
            .sort()[0] ?? null,
      },
      unread_chats: sellerChatUnreadStore.get().count,
    },
    money: {
      available: sellerMoneyStore.get().available,
      held: 1_907_600,
      pending_withdrawal: sellerMoneyStore.get().pending_withdrawal,
      cod_pending: 1_950_000,
    },
    orders_30d: {
      count: 100,
      previous_count: 92,
      visits: 3120,
      previous_visits: 2786,
      outcomes: {
        COMPLETED: 81,
        CANCELLED_BY_CUSTOMER: 4,
        REJECTED_BY_SHOP: 2,
        REFUNDED: 2,
        IN_PROGRESS: 11,
      },
    },
    products: {
      PUBLISHED: count('PUBLISHED'),
      PENDING_APPROVAL: count('PENDING_APPROVAL'),
      DRAFT: count('DRAFT'),
      REJECTED: count('REJECTED'),
    },
  };
}

export async function getDashboard(): Promise<Dashboard> {
  if (USE_MOCK) {
    await mockDelay(200);
    return toDashboard(mockDashboard());
  }
  return toDashboard(await api.get<DashboardDto>('/seller/dashboard'));
}
