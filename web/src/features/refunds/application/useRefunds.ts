// Trả hàng / hoàn tiền (TanStack Query) — làm mới đơn (khóa ['me','orders']) sau mỗi thao tác.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { EvidenceRef, RefundInput, RefundRequest } from '../domain/refund.types';
import {
  acceptRejection,
  addEvidence,
  createRefund,
  escalateRefund,
  listBanks,
} from '../infrastructure/refund.api';

const ORDERS = ['me', 'orders'] as const;

export const useBanks = (enabled = true) =>
  useQuery({ queryKey: ['banks'], queryFn: listBanks, staleTime: 24 * 3_600_000, enabled });

function useOrderMutation<V>(fn: (v: V) => Promise<unknown>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ORDERS }),
  });
}

export const useCreateRefund = () =>
  useOrderMutation(({ input, amount }: { input: RefundInput; amount: number }) =>
    createRefund(input, amount),
  );
export const useAddEvidence = () =>
  useOrderMutation(({ r, evidence }: { r: RefundRequest; evidence: EvidenceRef[] }) =>
    addEvidence(r, evidence),
  );
export const useEscalateRefund = () => useOrderMutation(escalateRefund);
export const useAcceptRejection = () => useOrderMutation(acceptRejection);
