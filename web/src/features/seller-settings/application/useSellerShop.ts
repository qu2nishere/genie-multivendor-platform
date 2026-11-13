// Ngữ cảnh shop cho khung Seller Center (TanStack Query).
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { searchable } from '../domain/shop.rules';
import { getPayoutAccount, getSellerMe, searchSeller } from '../infrastructure/shop.api';

export const SELLER_ME_KEY = ['seller', 'me'] as const;

export const useSellerMe = () =>
  useQuery({ queryKey: SELLER_ME_KEY, queryFn: getSellerMe, refetchInterval: 60_000 });

export const usePayoutAccount = () =>
  useQuery({ queryKey: ['seller', 'payout-account'], queryFn: getPayoutAccount });

/** Tìm nhanh có debounce 250 ms (10.1b). */
export function useSellerSearch(q: string) {
  const [term, setTerm] = useState(q.trim());
  useEffect(() => {
    const id = setTimeout(() => setTerm(q.trim()), 250);
    return () => clearTimeout(id);
  }, [q]);
  const query = useQuery({
    queryKey: ['seller', 'search', term],
    queryFn: () => searchSeller(term),
    enabled: searchable(term),
    staleTime: 15_000,
  });
  return { ...query, term };
}
