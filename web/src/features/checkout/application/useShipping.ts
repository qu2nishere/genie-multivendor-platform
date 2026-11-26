// Chính sách phí ship của các shop trong giỏ / đơn (TanStack Query, cache theo bộ shop + dòng + địa chỉ).
import { useQuery } from '@tanstack/react-query';
import { getShipPolicies, type ShipQuoteScope } from '../infrastructure/shipping.api';

export function useShipPolicies(shopIds: number[], scope?: ShipQuoteScope) {
  const ids = [...new Set(shopIds)].sort((a, b) => a - b);
  const lines = scope ? [...scope.cartItemIds].sort((a, b) => a - b).join(',') : '';
  return useQuery({
    queryKey: ['shipping', 'policies', ids.join(','), lines, scope?.addressId ?? null],
    queryFn: () => getShipPolicies(ids, scope),
    enabled: ids.length > 0,
    staleTime: 10 * 60_000,
    placeholderData: (prev) => prev,
  });
}
