// Hồ sơ KYC (TanStack Query). Nộp xong làm mới mọi truy vấn ['seller', …] để khung đổi chip KYC + khóa mục.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { KYC_POLL_MS, kycJustSettled, shouldPollKyc } from '../domain/kyc.rules';
import { getKyc, requestManualReview, saveKycDraft, submitKyc } from '../infrastructure/kyc.api';

const KEY = ['seller', 'kyc'] as const;

export function useKyc() {
  const qc = useQueryClient();
  const query = useQuery({
    queryKey: KEY,
    queryFn: getKyc,
    refetchInterval: (q) => (shouldPollKyc(q.state.data?.status) ? KYC_POLL_MS : false),
  });
  // AI chấm xong → tải lại khung Seller Center (chip KYC + menu khóa) ngay
  const status = query.data?.status;
  const prev = useRef(status);
  useEffect(() => {
    if (kycJustSettled(prev.current, status)) {
      void qc.invalidateQueries({ queryKey: ['seller', 'me'] });
    }
    prev.current = status;
  }, [status, qc]);
  return query;
}

function useSellerMutation<V>(fn: (v: V) => Promise<void>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['seller'] }),
  });
}

export const useSaveKycDraft = () => useSellerMutation(saveKycDraft);
export const useSubmitKyc = () => useSellerMutation(submitKyc);
export const useRequestManualReview = () => useSellerMutation(() => requestManualReview());
