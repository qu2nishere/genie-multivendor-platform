import type { OrderDetail, OrderItem, OrderSummary } from '../domain/order.types';
import type { OrderDetailDto, OrderItemDto, OrderSummaryDto } from './order.dto';

const toItem = (d: OrderItemDto): OrderItem => ({
  id: d.id,
  productId: d.product_id,
  variantId: d.variant_id,
  productSlug: d.product_slug,
  title: d.product_name,
  image: d.image_url,
  variantLabel: d.variant_name,
  qty: d.quantity,
  price: d.unit_price,
  paidUnitPrice: d.paid_unit_price ?? null,
});

export const toOrderSummary = (d: OrderSummaryDto): OrderSummary => ({
  id: d.id,
  code: d.code,
  sessionId: d.checkout_session_id,
  status: d.status,
  paymentMethod: d.payment_method,
  shop: { id: d.shop.id, name: d.shop.name, authentic: d.shop.is_mall },
  items: d.items.map(toItem),
  subtotal: d.subtotal,
  shippingFee: d.shipping_fee,
  discountTotal: d.discount_total,
  grandTotal: d.grand_total,
  paymentExpiresAt: d.payment_expires_at,
  deliveredAt: d.delivered_at,
  reviewed: d.is_reviewed,
  actions: d.allowed_actions,
});

export const toOrderDetail = (d: OrderDetailDto): OrderDetail => ({
  ...toOrderSummary(d),
  placedAt: d.placed_at,
  coinDiscount: d.money.coin_discount,
  address: {
    recipient: d.shipping_address.recipient_name,
    phone: d.shipping_address.phone_number,
    full: d.shipping_address.full_address,
  },
  shipment: d.shipment && {
    carrier: d.shipment.carrier,
    trackingCode: d.shipment.tracking_code,
    estimatedDeliveryAt: d.shipment.estimated_delivery_at,
    deliveredAt: d.shipment.delivered_at,
    events: d.shipment.events.map((e) => ({
      status: e.status,
      description: e.description,
      location: e.location,
      occurredAt: e.occurred_at,
    })),
  },
  escrowReleaseAt: d.escrow_release_at,
  returnWindowClosed: d.return_window_closed,
  refund: d.refund_request && {
    id: d.refund_request.id,
    code: d.refund_request.code,
    status: d.refund_request.status,
    requestType: d.refund_request.request_type,
    reasonCode: d.refund_request.reason_code,
    refundAmount: d.refund_request.refund_amount,
    shopMessage: d.refund_request.shop_message,
    deadlineAt: d.refund_request.deadline_at,
    refundBankMasked: d.refund_request.refund_bank_masked,
    resolvedAt: d.refund_request.resolved_at,
  },
  cancelReason: d.cancel_reason,
  note: d.note,
});
