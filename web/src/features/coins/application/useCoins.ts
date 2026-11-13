import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  checkIn,
  getCoins,
  listCoinRewards,
  listCoinTxns,
  redeemReward,
} from '../infrastructure/coins.api';

export const COINS_KEY = ['me', 'coins'] as const;

export function useCoins(enabled = true) {
  return useQuery({ queryKey: COINS_KEY, queryFn: getCoins, enabled });
}
export const useCoinTxns = () =>
  useQuery({ queryKey: [...COINS_KEY, 'txns'], queryFn: listCoinTxns });
export const useCoinRewards = () =>
  useQuery({ queryKey: ['coin-rewards'], queryFn: listCoinRewards, staleTime: 3_600_000 });

function useCoinsMutation<V, R>(fn: (v: V) => Promise<R>, alsoVouchers = false) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: COINS_KEY });
      if (alsoVouchers) void qc.invalidateQueries({ queryKey: ['me', 'vouchers'] });
    },
  });
}
export const useCheckIn = () => useCoinsMutation(() => checkIn());
export const useRedeemReward = () => useCoinsMutation(redeemReward, true);
