// Thao tác đơn + trả hàng phía shop. Xong mỗi thao tác làm mới mọi truy vấn ['seller', …] (danh sách đơn,
// Tổng quan, huy hiệu nav, tìm nhanh) vì một thay đổi đơn kéo theo cả các số đó.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { RejectReason } from '../domain/fulfillment.rules';
import type { SellerRefundAction } from '../domain/refund.types';
import {
  confirmOrder,
  createShipment,
  getSellerOrder,
  getShippingLabel,
  getShippingQuotes,
  prepareOrder,
  rejectOrder,
} from '../infrastructure/fulfillment.api';
import { actOnRefund, listSellerRefunds } from '../infrastructure/refund.api';

const SELLER = ['seller'] as const;

function useSellerMutation<V, R>(fn: (v: V) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: SELLER }),
  });
}

export const useSellerOrder = (id: number | null) =>
  useQuery({
    queryKey: [...SELLER, 'order', id],
    queryFn: () => getSellerOrder(id ?? 0),
    enabled: id != null,
  });

export const useConfirmOrder = () => useSellerMutation(confirmOrder);
export const usePrepareOrder = () => useSellerMutation(prepareOrder);
export const useRejectOrder = () =>
  useSellerMutation((v: { id: number; reason: RejectReason; note: string | null; label: string }) =>
    rejectOrder(v.id, v),
  );

export const useShippingQuotes = (id: number | null) =>
  useQuery({
    queryKey: [...SELLER, 'order', id, 'quotes'],
    queryFn: () => getShippingQuotes(id ?? 0),
    enabled: id != null,
    staleTime: 0,
  });

export const useCreateShipment = () => useSellerMutation(createShipment);

export const useShippingLabel = (id: number | null) =>
  useQuery({
    queryKey: [...SELLER, 'order', id, 'label'],
    queryFn: () => getShippingLabel(id ?? 0),
    enabled: id != null,
  });

export const useSellerRefunds = () =>
  useQuery({ queryKey: [...SELLER, 'refunds'], queryFn: listSellerRefunds });

export const useRefundAction = () =>
  useSellerMutation((v: { id: number; action: SellerRefundAction; text?: string }) =>
    actOnRefund(v.id, v.action, v.text),
  );
