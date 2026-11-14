// TanStack Query cho đánh giá: trang đầu SSR làm dữ liệu khởi tạo, đổi bộ lọc / trang thì tải lại.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReportReason, ReviewFilter, ReviewPage } from '../domain/review.types';
import {
  createReview,
  listReviews,
  reportReview,
  toggleHelpful,
  type ReviewInput,
} from '../infrastructure/review.api';

export const reviewsKey = (productId: number, filter: ReviewFilter, page: number) =>
  ['reviews', productId, filter, page] as const;

export function useReviews(
  productId: number,
  filter: ReviewFilter,
  page: number,
  initial?: { filter: ReviewFilter; page: number; data: ReviewPage },
) {
  const isInitial = !!initial && initial.filter === filter && initial.page === page;
  return useQuery({
    queryKey: reviewsKey(productId, filter, page),
    queryFn: () => listReviews(productId, { filter, page }),
    initialData: isInitial ? initial.data : undefined,
    placeholderData: keepPreviousData,
    staleTime: 60_000,
  });
}

export function useToggleHelpful(productId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: toggleHelpful,
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['reviews', productId] }),
  });
}

export function useReportReview() {
  return useMutation({
    mutationFn: (v: { reviewId: string; reason: ReportReason; note?: string }) =>
      reportReview(v.reviewId, v.reason, v.note),
  });
}

/** Gửi đánh giá một dòng hàng (7.9); làm mới đơn (đã đánh giá) và ví Điểm (+50). */
export function useCreateReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ReviewInput) => createReview(input),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['me', 'orders'] });
      void qc.invalidateQueries({ queryKey: ['me', 'coins'] });
    },
  });
}
