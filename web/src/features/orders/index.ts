// API công khai của feature "orders" — nơi DUY NHẤT feature khác được import.
export type {
  OrderStatus,
  StatusGroup,
  OrderAction,
  CancelReason,
  OrderItem,
  OrderSummary,
  OrderDetail,
  OrderRefund,
  Shipment,
} from './domain/order.types';
export {
  STATUS_GROUPS,
  CANCEL_REASONS,
  statusGroupOf,
  statusTone,
  stepOf,
  allowedActions,
  groupOrders,
} from './domain/order.rules';
export { useOrders, useOrder, ORDERS_KEY } from './application/useOrders';
export { getOrder, listOrders } from './infrastructure/order.api';
export { default as OrdersPanel, type OrdersPanelProps } from './ui/OrdersPanel';
export { default as OrderTracking, type OrderTrackingProps } from './ui/OrderTracking';
