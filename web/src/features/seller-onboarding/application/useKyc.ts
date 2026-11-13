// Hồ sơ KYC (TanStack Query). Nộp xong làm mới mọi truy vấn ['seller', …] để khung đổi chip KYC + khóa mục.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getKyc, requestManualReview, saveKycDraft, submitKyc } from '../infrastructure/kyc.api';

const KEY = ['seller', 'kyc'] as const;

export const useKyc = () => useQuery({ queryKey: KEY, queryFn: getKyc });

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
