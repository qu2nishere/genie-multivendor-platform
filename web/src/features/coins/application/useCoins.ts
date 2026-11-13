import { useQuery } from '@tanstack/react-query';
import { getCoins } from '../infrastructure/coins.api';

export const COINS_KEY = ['me', 'coins'] as const;

export function useCoins(enabled = true) {
  return useQuery({ queryKey: COINS_KEY, queryFn: getCoins, enabled });
}
