// Đơn hàng phía shop (API_SPEC 10.3; prototype vendor/data.jsx ORDERS, Fulfillment.jsx).
export type SellerOrderStatus =
  'NEW' | 'CONFIRMED' | 'PREPARING' | 'DELIVERING' | 'DELIVERED' | 'CANCELLED';

export type ShipmentStatus =
  | 'AWAITING_PICKUP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERY_FAILED'
  | 'DELIVERED'
  | 'CANCELLED';

export type Carrier = 'GHN' | 'GHTK' | 'VIETTEL_POST' | 'JNT_EXPRESS' | 'GENIE_EXPRESS';
export type PaymentMethod = 'VNPAY' | 'VIETQR' | 'COD';

/** Tab danh sách (10.3 `tab`; "Trả hàng" là màn riêng 10.4). */
export type OrderTab = 'ALL' | 'NEW' | 'TO_PREPARE' | 'DELIVERING' | 'DELIVERED' | 'CANCELLED';

export interface SellerOrderItem {
  name: string;
  variant: string;
  quantity: number;
  unitPrice: number;
}

export interface SellerOrder {
  id: number;
  code: string;
  status: SellerOrderStatus;
  payment: PaymentMethod;
  customer: { id: number; name: string; avatarUrl: string | null };
  shipTo: string;
  items: SellerOrderItem[];
  total: number;
  note: string | null;
  rejectReason: string | null;
  shipment: {
    carrier: Carrier;
    trackingCode: string | null;
    status: ShipmentStatus;
    statusText: string;
  } | null;
  createdAt: string;
}

/** Màu huy hiệu (trùng tên StatusBadge của shared/ui). */
export type BadgeTone = 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED' | 'ERROR';

export interface OrderListQuery {
  tab?: OrderTab;
  q?: string;
  sort?: 'NEWEST' | 'OLDEST';
  page?: number;
  size?: number;
}
