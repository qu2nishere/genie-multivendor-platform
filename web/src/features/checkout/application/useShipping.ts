// Chính sách phí ship của các shop trong giỏ / đơn (TanStack Query, cache theo bộ shop).
import { useQuery } from '@tanstack/react-query';
import { getShipPolicies } from '../infrastructure/shipping.api';

export function useShipPolicies(shopIds: number[]) {
  const ids = [...new Set(shopIds)].sort((a, b) => a - b);
  return useQuery({
    queryKey: ['shipping', 'policies', ids.join(',')],
    queryFn: () => getShipPolicies(ids),
    enabled: ids.length > 0,
    staleTime: 10 * 60_000,
    placeholderData: (prev) => prev,
  });
}
