// Quy tắc đơn mua — TS thuần (plan G10.30; API_SPEC 16.3; prototype STEPS / STEP_OF / BADGE).
import type {
  CancelReason,
  OrderAction,
  OrderRow,
  OrderStatus,
  OrderSummary,
  PaymentMethod,
  StatusGroup,
} from './order.types';

export const STATUS_GROUPS: StatusGroup[] = [
  'TO_PAY',
  'SHIPPING',
  'TO_RECEIVE',
  'COMPLETED',
  'RETURN',
  'CANCELLED',
];

export const CANCEL_REASONS: CancelReason[] = [
  'CHANGE_OF_MIND',
  'CHANGE_ADDRESS',
  'CHANGE_VARIANT',
  'FOUND_BETTER_PRICE',
  'OTHER',
];

const SHIPPING: OrderStatus[] = [
  'PAID',
  'CONFIRMED',
  'PREPARING',
  'READY_FOR_PICKUP',
  'DELIVERING',
  'DELIVERY_FAILED',
];
const RETURN: OrderStatus[] = [
  'RETURN_REQUESTED',
  'RETURN_APPROVED',
  'RETURN_REJECTED',
  'RETURNING',
  'RETURNED_TO_VENDOR',
  'PARTIALLY_REFUNDED',
  'REFUNDED',
];
const CANCELLED: OrderStatus[] = ['CANCELLED', 'CANCELLED_BY_VENDOR', 'AUTO_CANCELLED'];

/** Tab của đơn (16.3): PAID của đơn COD đã giao thuộc "Chờ giao hàng". */
export function statusGroupOf(
  o: Pick<OrderSummary, 'status' | 'paymentMethod' | 'deliveredAt'>,
): StatusGroup {
  if (o.status === 'PENDING_PAYMENT') return 'TO_PAY';
  if (o.status === 'DELIVERED') return 'TO_RECEIVE';
  if (o.status === 'PAID' && o.paymentMethod === 'COD' && o.deliveredAt) return 'TO_RECEIVE';
  if (SHIPPING.includes(o.status)) return 'SHIPPING';
  if (o.status === 'COMPLETED') return 'COMPLETED';
  if (RETURN.includes(o.status)) return 'RETURN';
  return 'CANCELLED';
}

export const isCancelled = (s: OrderStatus): boolean => CANCELLED.includes(s);
export const isReturn = (s: OrderStatus): boolean => RETURN.includes(s);

/** Tông màu nhãn trạng thái — khóa của STATUS_TONES (prototype badgeStatus). */
export function statusTone(
  s: OrderStatus,
): 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'ERROR' {
  if (s === 'PAID') return 'PAID';
  if (s === 'DELIVERING') return 'SHIPPED';
  if (s === 'DELIVERED') return 'DELIVERED';
  if (s === 'COMPLETED') return 'COMPLETED';
  if (s === 'DELIVERY_FAILED') return 'ERROR';
  if (isCancelled(s)) return 'CANCELLED';
  return 'PENDING'; // chờ thanh toán, đang xử lý, trả hàng / hoàn tiền
}

/** 7 bước của thanh tiến trình (prototype OrderTracking STEPS). */
export const STEPS = [
  { key: 'placed', icon: 'file-text' },
  { key: 'paid', icon: 'wallet' },
  { key: 'confirmed', icon: 'badge-check' },
  { key: 'prep', icon: 'package' },
  { key: 'ship', icon: 'truck' },
  { key: 'delivered', icon: 'house' },
  { key: 'completed', icon: 'badge-check' },
] as const;

/** Bước hiện tại theo trạng thái (prototype STEP_OF). */
export function stepOf(s: OrderStatus): number {
  switch (s) {
    case 'PENDING_PAYMENT':
    case 'AUTO_CANCELLED':
      return 0;
    case 'PAID':
    case 'CANCELLED':
    case 'CANCELLED_BY_VENDOR':
      return 1;
    case 'CONFIRMED':
      return 2;
    case 'PREPARING':
    case 'READY_FOR_PICKUP':
      return 3;
    case 'DELIVERING':
    case 'DELIVERY_FAILED':
      return 4;
    case 'COMPLETED':
      return 6;
    default:
      return 5; // DELIVERED, trả hàng / hoàn tiền
  }
}

/**
 * Nút hợp lệ theo trạng thái (server trả allowed_actions; mock dùng hàm này). Bám prototype Actions:
 * hủy được tới PREPARING (từ READY_FOR_PICKUP → GNE-ORD-4092); trả hàng khi đã giao và còn hạn 7 ngày.
 */
export function allowedActions(o: {
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  deliveredAt: string | null;
  reviewed: boolean;
  returnWindowClosed?: boolean;
}): OrderAction[] {
  const s = o.status;
  if (s === 'PENDING_PAYMENT') return o.paymentMethod === 'COD' ? ['CANCEL'] : ['PAY', 'CANCEL'];
  if (s === 'PAID' && o.paymentMethod === 'COD' && o.deliveredAt)
    return o.returnWindowClosed ? ['TRACK', 'CHAT'] : ['REQUEST_RETURN', 'TRACK', 'CHAT'];
  if (s === 'PAID' || s === 'CONFIRMED' || s === 'PREPARING') return ['TRACK', 'CHAT', 'CANCEL'];
  if (s === 'READY_FOR_PICKUP' || s === 'DELIVERING' || s === 'DELIVERY_FAILED')
    return ['TRACK', 'CHAT'];
  if (s === 'DELIVERED')
    return o.returnWindowClosed
      ? ['CONFIRM_RECEIPT', 'TRACK', 'CHAT']
      : ['CONFIRM_RECEIPT', 'REQUEST_RETURN', 'TRACK', 'CHAT'];
  if (s === 'COMPLETED') return [...(o.reviewed ? [] : (['REVIEW'] as const)), 'BUY_AGAIN', 'CHAT'];
  if (isReturn(s) && s !== 'REFUNDED') return ['VIEW_RETURN', 'CHAT'];
  return ['BUY_AGAIN', 'CHAT'];
}

/** Lọc theo tab; undefined = tất cả. */
export const filterByGroup = (orders: OrderSummary[], g?: StatusGroup): OrderSummary[] =>
  g ? orders.filter((o) => statusGroupOf(o) === g) : orders;

export function countByGroup(orders: OrderSummary[]): Record<StatusGroup, number> {
  const c = Object.fromEntries(STATUS_GROUPS.map((g) => [g, 0])) as Record<StatusGroup, number>;
  for (const o of orders) c[statusGroupOf(o)] += 1;
  return c;
}

/**
 * Gộp đơn online cùng phiên đang chờ thanh toán / đã hủy thành "Nhóm đơn hàng" (7.1) — giữ thứ tự
 * xuất hiện của đơn đầu nhóm.
 */
export function groupOrders(orders: OrderSummary[]): OrderRow[] {
  const rows: OrderRow[] = [];
  for (const o of orders) {
    const groupable =
      o.sessionId != null &&
      o.paymentMethod !== 'COD' &&
      (o.status === 'PENDING_PAYMENT' || o.status === 'CANCELLED');
    const hit = groupable
      ? rows.find(
          (r): r is Extract<OrderRow, { kind: 'group' }> =>
            r.kind === 'group' && r.sessionId === o.sessionId && r.orders[0].status === o.status,
        )
      : undefined;
    if (hit) hit.orders.push(o);
    else if (groupable && o.sessionId != null)
      rows.push({ kind: 'group', sessionId: o.sessionId, orders: [o] });
    else rows.push({ kind: 'single', order: o });
  }
  // Nhóm chỉ có một đơn → thẻ đơn thường
  return rows.map((r) =>
    r.kind === 'group' && r.orders.length === 1 ? { kind: 'single', order: r.orders[0] } : r,
  );
}

export const sumOrders = (
  orders: OrderSummary[],
  k: 'subtotal' | 'shippingFee' | 'discountTotal' | 'grandTotal',
) => orders.reduce((n, o) => n + o[k], 0);
