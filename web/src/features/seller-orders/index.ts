// API công khai của feature "seller-orders" — nơi DUY NHẤT feature khác được import.
export type {
  SellerOrder,
  SellerOrderStatus,
  ShipmentStatus,
  OrderTab,
  BadgeTone,
  OrderListQuery,
} from './domain/order.types';
export {
  orderBadge,
  inTab,
  itemCount,
  workCounts,
  whenLabel,
  type WhenLabel,
} from './domain/order.rules';
export { useSellerOrders, sellerNow, SELLER_ORDERS_KEY } from './application/useSellerOrders';
