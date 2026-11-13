// Theo dõi shop (prototype FOLLOWS) — trạng thái lấy từ danh sách shop đang theo dõi của người dùng.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listFollowedShops, setFollow } from '../infrastructure/shop.api';

export const FOLLOWED_SHOPS_KEY = ['me', 'shop-follows'] as const;

export function useFollowedShops(enabled: boolean) {
  return useQuery({
    queryKey: FOLLOWED_SHOPS_KEY,
    queryFn: listFollowedShops,
    enabled,
    staleTime: 60_000,
  });
}

export function useSetFollow() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { shopId: number; on: boolean }) => setFollow(v.shopId, v.on),
    onSuccess: () => void qc.invalidateQueries({ queryKey: FOLLOWED_SHOPS_KEY }),
  });
}
