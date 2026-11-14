// Đơn mua (API_SPEC 7.1–7.4). Mock: đơn mẫu + đơn vừa đặt ở checkout, giữ thay đổi trong phiên tab.
import { api } from '@/shared/api/http';
import { mockDelay, mockFail, mockState, USE_MOCK } from '@/shared/api/mock';
import { activityStore } from '@/shared/api/mock/activity';
import { placedStore, type MockPlacedSession } from '@/shared/api/mock/placed';
import { allowedActions, countByGroup, filterByGroup } from '../domain/order.rules';
import type {
  CancelReason,
  OrderDetail,
  OrderStatus,
  OrderSummary,
  StatusGroup,
} from '../domain/order.types';
import type { OrderDetailDto, OrderSummaryDto } from './order.dto';
import { toOrderDetail, toOrderSummary } from './order.mapper';
import { placedToDto, SEED_PENDING_SESSION, seedOrders } from './order.mock';

const seedDb = mockState<OrderDetailDto[]>('orders', () => seedOrders());
/** Thay đổi trên đơn vừa đặt (hủy, đã nhận…) — đơn gốc nằm ở kho checkout. */
const placedPatch = mockState<Record<number, Partial<OrderDetailDto>>>('orders.placed', () => ({}));

/** Phiên chờ thanh toán của nhóm đơn mẫu, để "Thanh toán ngay" gọi được 6.12 như đơn thật. */
function ensureSeedSession(): void {
  const db = placedStore.get();
  if (db.sessions.some((s) => s.id === SEED_PENDING_SESSION)) return;
  const pending = seedDb.get().filter((o) => o.checkout_session_id === SEED_PENDING_SESSION);
  const sum = (k: 'subtotal' | 'shipping_fee' | 'discount_total' | 'grand_total') =>
    pending.reduce((n, o) => n + o[k], 0);
  const session: MockPlacedSession = {
    id: SEED_PENDING_SESSION,
    code: 'CS-8F21A',
    status: 'PAYMENT_PENDING',
    payment_method: 'VNPAY',
    created_at: new Date().toISOString(),
    reservation_expires_at: pending[0]?.payment_expires_at ?? new Date().toISOString(),
    order_ids: [],
    cart_item_ids: [],
    coin_discount: 0,
    idempotency_key: 'seed',
    summary: {
      subtotal: sum('subtotal'),
      shipping_total: sum('shipping_fee'),
      discount_total: sum('discount_total'),
      coin_discount: 0,
      grand_total: sum('grand_total'),
    },
  };
  placedStore.set({ ...db, sessions: [...db.sessions, session] });
}

/** Mọi đơn mock (mới đặt trước), đã áp trạng thái thanh toán của phiên và các thay đổi. */
function allMock(): OrderDetailDto[] {
  ensureSeedSession();
  const { sessions, orders } = placedStore.get();
  const statusOf = (sid: number | null) => sessions.find((s) => s.id === sid)?.status;
  const patches = placedPatch.get();
  const placed = [...orders]
    .reverse()
    .map((o) => ({ ...placedToDto(o, statusOf(o.session_id)), ...patches[o.id] }));
  const seed = seedDb.get().map((o) => {
    const st = statusOf(o.checkout_session_id);
    if (o.status !== 'PENDING_PAYMENT' || st !== 'COMPLETED') return o;
    const paid = { ...o, status: 'PAID' as OrderStatus, payment_expires_at: null };
    return { ...paid, allowed_actions: actionsOf(paid) };
  });
  return [...placed, ...seed].map(withActivity);
}

/** Áp yêu cầu trả hàng / đánh giá (kho "hoạt động" mock) lên đơn — server làm việc này khi ghi. */
function withActivity(o: OrderDetailDto): OrderDetailDto {
  const { refunds, reviews } = activityStore.get();
  const r = [...refunds].reverse().find((x) => x.order_id === o.id);
  const reviewed =
    o.items.length > 0 && o.items.every((it) => reviews.some((v) => v.order_item_id === it.id));
  let next: OrderDetailDto = { ...o, is_reviewed: o.is_reviewed || reviewed };
  if (r) {
    const status: OrderStatus =
      r.status === 'REJECTED'
        ? 'COMPLETED'
        : r.status === 'REFUNDED'
          ? 'REFUNDED'
          : 'RETURN_REQUESTED';
    next = {
      ...next,
      status,
      return_window_closed: next.return_window_closed || r.status === 'REJECTED',
      refund_request: {
        id: r.id,
        code: r.code,
        status: r.status,
        request_type: r.request_type,
        reason_code: r.reason_code,
        refund_amount: r.refund_amount,
        shop_message: r.shop_message,
        deadline_at: r.deadline_at,
        refund_bank_masked: r.refund_bank_masked,
        resolved_at: r.resolved_at,
      },
    };
  }
  return { ...next, allowed_actions: actionsOf(next) };
}

const actionsOf = (o: OrderDetailDto) =>
  allowedActions({
    status: o.status,
    paymentMethod: o.payment_method,
    deliveredAt: o.delivered_at,
    reviewed: o.is_reviewed,
    returnWindowClosed: o.return_window_closed,
  });

/** Sửa một đơn mock (đơn mẫu hoặc đơn vừa đặt) và tính lại nút hợp lệ. */
export function patchMockOrder(id: number, patch: Partial<OrderDetailDto>): OrderDetailDto {
  const cur = allMock().find((o) => o.id === id);
  if (!cur) mockFail('GNE-ORD-4041');
  const next = { ...cur, ...patch };
  next.allowed_actions = actionsOf(next);
  if (seedDb.get().some((o) => o.id === id)) {
    seedDb.set(seedDb.get().map((o) => (o.id === id ? next : o)));
  } else {
    placedPatch.set({
      ...placedPatch.get(),
      [id]: { ...placedPatch.get()[id], ...patch, allowed_actions: next.allowed_actions },
    });
  }
  return next;
}

export async function listOrders(
  group?: StatusGroup,
): Promise<{ items: OrderSummary[]; counts: Record<StatusGroup, number> }> {
  if (USE_MOCK) {
    await mockDelay();
    const all = allMock().map(toOrderSummary);
    return { items: filterByGroup(all, group), counts: countByGroup(all) };
  }
  const r = await api.get<OrderSummaryDto[]>('/me/orders', {
    query: { status_group: group, limit: 50 },
  });
  const items = r.map(toOrderSummary);
  return { items, counts: countByGroup(items) };
}

export async function getOrder(id: number): Promise<OrderDetail> {
  if (USE_MOCK) {
    await mockDelay(150);
    const o = allMock().find((x) => x.id === id);
    if (!o) mockFail('GNE-ORD-4041');
    return toOrderDetail(o);
  }
  return toOrderDetail(await api.get<OrderDetailDto>(`/me/orders/${id}`));
}

/** POST /me/orders/{id}/cancel — từ READY_FOR_PICKUP trở đi → 409 GNE-ORD-4092. */
export async function cancelOrder(
  id: number,
  reason: CancelReason,
  note: string | null,
): Promise<{ refundInitiated: boolean }> {
  if (USE_MOCK) {
    await mockDelay(300);
    const o = allMock().find((x) => x.id === id);
    if (!o) mockFail('GNE-ORD-4041');
    if (!o.allowed_actions.includes('CANCEL')) mockFail('GNE-ORD-4092');
    patchMockOrder(id, {
      status: 'CANCELLED',
      cancel_reason: reason,
      note: note ?? o.note,
      payment_expires_at: null,
    });
    return { refundInitiated: o.status !== 'PENDING_PAYMENT' && o.payment_method !== 'COD' };
  }
  const r = await api.post<{ status: string; refund_initiated: boolean }>(
    `/me/orders/${id}/cancel`,
    { reason_code: reason, note },
  );
  return { refundInitiated: r.refund_initiated };
}

/** POST /me/orders/{id}/confirm-receipt — chưa DELIVERED → 409 GNE-ORD-4095. */
export async function confirmReceipt(id: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(300);
    const o = allMock().find((x) => x.id === id);
    if (!o) mockFail('GNE-ORD-4041');
    if (!o.allowed_actions.includes('CONFIRM_RECEIPT')) mockFail('GNE-ORD-4095');
    patchMockOrder(id, { status: 'COMPLETED', escrow_release_at: new Date().toISOString() });
    return;
  }
  await api.post(`/me/orders/${id}/confirm-receipt`);
}
