// Hồ sơ KYC (TanStack Query). Nộp xong làm mới mọi truy vấn ['seller', …] để khung đổi chip KYC + khóa mục.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { KYC_POLL_MS, shouldPollKyc } from '../domain/kyc.rules';
import { getKyc, requestManualReview, saveKycDraft, submitKyc } from '../infrastructure/kyc.api';

const KEY = ['seller', 'kyc'] as const;

export const useKyc = () =>
  useQuery({
    queryKey: KEY,
    queryFn: getKyc,
    refetchInterval: (q) => (shouldPollKyc(q.state.data?.status) ? KYC_POLL_MS : false),
  });

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
