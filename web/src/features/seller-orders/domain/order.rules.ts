// Quy tắc đơn phía shop: huy hiệu trạng thái (prototype orderBadge / ghnStatus), tab, số món, mốc thời
// gian tương đối ("15:05 hôm nay", "Hôm qua", "2 ngày trước").
import type {
  BadgeTone,
  OrderTab,
  SellerOrder,
  SellerOrderStatus,
  ShipmentStatus,
} from './order.types';

const SHIPMENT_TONE: Record<ShipmentStatus, BadgeTone> = {
  AWAITING_PICKUP: 'PENDING',
  IN_TRANSIT: 'SHIPPED',
  OUT_FOR_DELIVERY: 'SHIPPED',
  DELIVERY_FAILED: 'ERROR',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
};

/**
 * Huy hiệu: trạng thái đơn khi chưa giao hãng (NEW / CONFIRMED / PREPARING), sau đó theo trạng thái
 * vận đơn (10.3). `key` là khóa i18n.
 */
export function orderBadge(o: Pick<SellerOrder, 'status' | 'shipment'>): {
  tone: BadgeTone;
  key: string;
} {
  if (o.status === 'NEW') return { tone: 'PENDING', key: 'seller-orders.status.NEW' };
  if (o.status === 'CONFIRMED') return { tone: 'PAID', key: 'seller-orders.status.CONFIRMED' };
  if (o.status === 'PREPARING') return { tone: 'PAID', key: 'seller-orders.status.PREPARING' };
  if (o.shipment)
    return {
      tone: SHIPMENT_TONE[o.shipment.status],
      key: `seller-orders.shipment.${o.shipment.status}`,
    };
  const fallback: Record<SellerOrderStatus, BadgeTone> = {
    NEW: 'PENDING',
    CONFIRMED: 'PAID',
    PREPARING: 'PAID',
    DELIVERING: 'SHIPPED',
    DELIVERED: 'DELIVERED',
    CANCELLED: 'CANCELLED',
  };
  return { tone: fallback[o.status], key: `seller-orders.status.${o.status}` };
}

const TAB_MATCH: Record<Exclude<OrderTab, 'ALL'>, SellerOrderStatus[]> = {
  NEW: ['NEW'],
  TO_PREPARE: ['CONFIRMED', 'PREPARING'],
  DELIVERING: ['DELIVERING'],
  DELIVERED: ['DELIVERED'],
  CANCELLED: ['CANCELLED'],
};

export const inTab = (o: Pick<SellerOrder, 'status'>, tab: OrderTab): boolean =>
  tab === 'ALL' || TAB_MATCH[tab].includes(o.status);

export const itemCount = (o: Pick<SellerOrder, 'items'>): number =>
  o.items.reduce((n, i) => n + i.quantity, 0);

/** Số việc theo hàng chờ (Tổng quan, badge tab). */
export function workCounts(orders: Pick<SellerOrder, 'status' | 'shipment'>[]) {
  return {
    toConfirm: orders.filter((o) => o.status === 'NEW').length,
    toPrepare: orders.filter((o) => o.status === 'CONFIRMED' || o.status === 'PREPARING').length,
    shipping: orders.filter((o) => o.status === 'DELIVERING').length,
    failed: orders.filter((o) => o.shipment?.status === 'DELIVERY_FAILED').length,
  };
}

const DAY = 86_400_000;
/** Ngày lịch theo giờ Việt Nam (UTC+7) — để so "hôm nay / hôm qua". */
const vnDay = (ms: number) => Math.floor((ms + 7 * 3_600_000) / DAY);

export type WhenLabel =
  | { kind: 'today'; time: string }
  | { kind: 'yesterday' }
  | { kind: 'days'; days: number }
  | { kind: 'date' };

/** Mốc thời gian tương đối so với `now` (prototype: "15:05 hôm nay", "Hôm qua", "2 ngày trước"). */
export function whenLabel(iso: string, now: number): WhenLabel {
  const at = Date.parse(iso);
  const d = vnDay(now) - vnDay(at);
  if (d <= 0) {
    const vn = new Date(at + 7 * 3_600_000);
    const time = `${String(vn.getUTCHours()).padStart(2, '0')}:${String(vn.getUTCMinutes()).padStart(2, '0')}`;
    return { kind: 'today', time };
  }
  if (d === 1) return { kind: 'yesterday' };
  if (d < 7) return { kind: 'days', days: d };
  return { kind: 'date' };
}
