// GET /me/orders (7.1), GET /me/orders/{id} (7.2) — snake_case.
import type {
  CancelReason,
  Carrier,
  OrderAction,
  OrderStatus,
  PaymentMethod,
} from '../domain/order.types';

export interface OrderItemDto {
  id: number;
  product_id: number;
  variant_id: number;
  /** Chưa có trong $OrderItem — client cần để dựng liên kết /p/{slug}-{id} (ghi G13.20). */
  product_slug: string;
  product_name: string;
  image_url: string;
  variant_name: string;
  quantity: number;
  unit_price: number;
  paid_unit_price?: number;
}

export interface ShopMiniDto {
  id: number;
  name: string;
  is_mall: boolean;
}

export interface OrderSummaryDto {
  id: number;
  code: string;
  checkout_session_id: number | null;
  status: OrderStatus;
  payment_method: PaymentMethod;
  shop: ShopMiniDto;
  items: OrderItemDto[];
  subtotal: number;
  shipping_fee: number;
  discount_total: number;
  grand_total: number;
  payment_expires_at: string | null;
  delivered_at: string | null;
  is_reviewed: boolean;
  allowed_actions: OrderAction[];
}

export interface ShipmentDto {
  carrier: Carrier;
  tracking_code: string;
  estimated_delivery_at: string | null;
  delivered_at: string | null;
  events: { status: string; description: string; location: string | null; occurred_at: string }[];
}

export interface RefundSummaryDto {
  id: number;
  code: string;
  status: string;
  request_type: 'RETURN_AND_REFUND' | 'REFUND_ONLY';
  reason_code: string;
  refund_amount: number;
  shop_message: string | null;
  deadline_at: string | null;
  refund_bank_masked: string | null;
  resolved_at: string | null;
}

export interface OrderDetailDto extends OrderSummaryDto {
  placed_at: string;
  money: {
    subtotal: number;
    shipping_fee: number;
    discount_total: number;
    coin_discount: number;
    grand_total: number;
  };
  shipping_address: { recipient_name: string; phone_number: string; full_address: string };
  shipment: ShipmentDto | null;
  escrow_release_at: string | null;
  return_window_closed: boolean;
  refund_request: RefundSummaryDto | null;
  cancel_reason: CancelReason | string | null;
  note: string | null;
}
