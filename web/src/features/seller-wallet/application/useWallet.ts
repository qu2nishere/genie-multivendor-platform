import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { LedgerQuery } from '../domain/wallet.types';
import { createWithdrawal, getWallet, listTransactions } from '../infrastructure/wallet.api';

const KEY = ['seller', 'wallet'] as const;

export const useWallet = () => useQuery({ queryKey: KEY, queryFn: getWallet });

export const useLedger = (q: LedgerQuery) =>
  useQuery({
    queryKey: [...KEY, 'tx', q],
    queryFn: () => listTransactions(q),
    placeholderData: keepPreviousData,
  });

/** Rút tiền xong làm mới ví + Tổng quan (Tiền của shop). */
export function useWithdraw() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { amount: number; proof: { challengeId: string; code: string } }) =>
      createWithdrawal(v.amount, v.proof),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['seller'] }),
  });
}
