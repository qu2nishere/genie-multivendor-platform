// Miền đơn mua (plan G10.30; API_SPEC 7.1–7.4, 16.3; prototype data.jsx ORDERS, Account.jsx, OrderTracking.jsx).

/** Trạng thái đơn — đúng enum backend (ENTITY orders.status). */
export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY_FOR_PICKUP'
  | 'DELIVERING'
  | 'DELIVERY_FAILED'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'RETURN_REQUESTED'
  | 'RETURN_APPROVED'
  | 'RETURN_REJECTED'
  | 'RETURNING'
  | 'RETURNED_TO_VENDOR'
  | 'PARTIALLY_REFUNDED'
  | 'REFUNDED'
  | 'CANCELLED'
  | 'CANCELLED_BY_VENDOR'
  | 'AUTO_CANCELLED';

export type PaymentMethod = 'VNPAY' | 'VIETQR' | 'COD';

/** Tab "Đơn mua" (status_group). */
export type StatusGroup =
  'TO_PAY' | 'SHIPPING' | 'TO_RECEIVE' | 'COMPLETED' | 'RETURN' | 'CANCELLED';

/** Nút được phép trên đơn (allowed_actions do server trả). */
export type OrderAction =
  | 'PAY'
  | 'CANCEL'
  | 'TRACK'
  | 'CHAT'
  | 'CONFIRM_RECEIPT'
  | 'REQUEST_RETURN'
  | 'VIEW_RETURN'
  | 'REVIEW'
  | 'BUY_AGAIN';

/** Lý do hủy của khách (API_SPEC 16.4). */
export type CancelReason =
  'CHANGE_OF_MIND' | 'CHANGE_ADDRESS' | 'CHANGE_VARIANT' | 'FOUND_BETTER_PRICE' | 'OTHER';

export interface OrderShop {
  id: number;
  name: string;
  authentic: boolean;
}

export interface OrderItem {
  id: number;
  productId: number;
  variantId: number;
  productSlug: string;
  title: string;
  image: string;
  variantLabel: string;
  qty: number;
  price: number;
  /** Đơn giá sau giảm giá đã phân bổ (trang chi tiết) — tính tiền hoàn dự kiến. */
  paidUnitPrice: number | null;
}

/** Thẻ đơn trong danh sách (7.1). */
export interface OrderSummary {
  id: number;
  code: string;
  sessionId: number | null;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  shop: OrderShop;
  items: OrderItem[];
  subtotal: number;
  shippingFee: number;
  discountTotal: number;
  grandTotal: number;
  paymentExpiresAt: string | null;
  /** ĐVVC đã báo giao thành công (hiện "Giao thành công", tính tab Chờ giao hàng cho COD). */
  deliveredAt: string | null;
  reviewed: boolean;
  actions: OrderAction[];
}

export type Carrier = 'GHN' | 'GHTK' | 'VIETTEL_POST' | 'JNT_EXPRESS' | 'GENIE_EXPRESS';

export interface ShipmentEvent {
  status: string;
  description: string;
  location: string | null;
  occurredAt: string;
}

export interface Shipment {
  carrier: Carrier;
  trackingCode: string;
  estimatedDeliveryAt: string | null;
  deliveredAt: string | null;
  /** Mới nhất trước. */
  events: ShipmentEvent[];
}

/** Tóm tắt yêu cầu trả hàng gắn với đơn (7.2 refund_request). */
export interface OrderRefund {
  id: number;
  code: string;
  status: string;
  requestType: 'RETURN_AND_REFUND' | 'REFUND_ONLY';
  reasonCode: string;
  refundAmount: number;
  shopMessage: string | null;
  deadlineAt: string | null;
  refundBankMasked: string | null;
  resolvedAt: string | null;
}

/** Trang theo dõi đơn (7.2). */
export interface OrderDetail extends OrderSummary {
  placedAt: string;
  coinDiscount: number;
  address: { recipient: string; phone: string; full: string };
  shipment: Shipment | null;
  escrowReleaseAt: string | null;
  returnWindowClosed: boolean;
  refund: OrderRefund | null;
  cancelReason: string | null;
  note: string | null;
}

/** Nhiều đơn cùng phiên chờ thanh toán / đã hủy → hiện thành một "Nhóm đơn hàng". */
export type OrderRow =
  | { kind: 'single'; order: OrderSummary }
  | { kind: 'group'; sessionId: number; orders: OrderSummary[] };
