// Đơn hàng của shop (TanStack Query).
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import type { OrderListQuery } from '../domain/order.types';
import { listSellerOrders, sellerNow } from '../infrastructure/order.api';

export const SELLER_ORDERS_KEY = ['seller', 'orders'] as const;

export const useSellerOrders = (q: OrderListQuery = {}) =>
  useQuery({
    queryKey: [...SELLER_ORDERS_KEY, q],
    queryFn: () => listSellerOrders(q),
    placeholderData: keepPreviousData,
  });

export { sellerNow };
