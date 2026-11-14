import type { SellerOrder } from '../domain/order.types';
import type { SellerOrderDto } from './order.dto';

export const toSellerOrder = (d: SellerOrderDto): SellerOrder => ({
  id: d.id,
  code: d.code,
  status: d.status,
  payment: d.payment_method,
  customer: { id: d.customer.id, name: d.customer.name, avatarUrl: d.customer.avatar_url },
  shipTo: d.ship_to,
  items: d.items.map((i) => ({
    name: i.product_name,
    variant: i.variant_name,
    quantity: i.quantity,
    unitPrice: i.unit_price,
  })),
  total: d.grand_total,
  note: d.buyer_note,
  rejectReason: d.reject_reason,
  shipment: d.shipment && {
    carrier: d.shipment.carrier,
    trackingCode: d.shipment.tracking_code,
    status: d.shipment.status,
    statusText: d.shipment.carrier_status_text,
  },
  createdAt: d.created_at,
});
