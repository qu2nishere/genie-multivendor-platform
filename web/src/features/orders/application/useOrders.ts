// Đơn mua (TanStack Query) — danh sách theo tab, chi tiết, hủy, đã nhận hàng.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { CancelReason, StatusGroup } from '../domain/order.types';
import { cancelOrder, confirmReceipt, getOrder, listOrders } from '../infrastructure/order.api';

export const ORDERS_KEY = ['me', 'orders'] as const;

export function useOrders(group?: StatusGroup) {
  return useQuery({
    queryKey: [...ORDERS_KEY, 'list', group ?? 'ALL'],
    queryFn: () => listOrders(group),
    placeholderData: (prev) => prev,
  });
}

export function useOrder(id: number) {
  return useQuery({ queryKey: [...ORDERS_KEY, 'detail', id], queryFn: () => getOrder(id) });
}

export function useCancelOrders() {
  const qc = useQueryClient();
  return useMutation({
    /** Hủy một đơn hoặc cả nhóm đơn cùng phiên (gọi lần lượt từng đơn). */
    mutationFn: async ({
      ids,
      reason,
      note,
    }: {
      ids: number[];
      reason: CancelReason;
      note: string | null;
    }) => {
      let refund = false;
      for (const id of ids)
        refund = (await cancelOrder(id, reason, note)).refundInitiated || refund;
      return { refund };
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ORDERS_KEY }),
  });
}

export function useConfirmReceipt() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: confirmReceipt,
    onSuccess: () => qc.invalidateQueries({ queryKey: ORDERS_KEY }),
  });
}
