import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReviewTab } from '../domain/review.types';
import { listShopReviews, markReported, replyReview } from '../infrastructure/review.api';

const KEY = ['seller', 'reviews'] as const;

export const useShopReviews = (tab: ReviewTab) =>
  useQuery({
    queryKey: [...KEY, tab],
    queryFn: () => listShopReviews(tab),
    placeholderData: keepPreviousData,
  });

export function useReplyReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (v: { id: number; content: string }) => replyReview(v.id, v.content),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

/**
 * Báo cáo vi phạm — POST /reviews/{id}/reports dùng chung với phía khách (feature reviews); hàm gửi do UI
 * truyền vào để tầng application không phụ thuộc feature khác.
 */
export function useReportReview<R extends string>(
  send: (reviewId: string, reason: R, note?: string) => Promise<void>,
) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (v: { id: number; reason: R; note?: string }) => {
      await send(String(v.id), v.reason, v.note);
      markReported(v.id);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}
