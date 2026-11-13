// Đánh giá sản phẩm: GET /public/products/{id}/reviews (kèm summary), POST /reviews/{id}/helpful,
// POST /reviews/{id}/reports. Mock khi USE_MOCK.
import { api, request } from '@/shared/api/http';
import { mockDelay, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { env } from '@/shared/config/env';
import { isPagePagination } from '@/shared/api/types';
import { matchesFilter, summarize } from '../domain/review.rules';
import type { ReportReason, ReviewFilter, ReviewPage } from '../domain/review.types';
import type { ReviewDto, ReviewSummaryDto } from './review.dto';
import { toReview, toSummary } from './review.mapper';
import { reported, reviewDtos, toggleMockHelpful } from './review.mock';

export const REVIEW_PAGE_SIZE = 5;

const baseUrl = () =>
  typeof window === 'undefined' ? (env.INTERNAL_API_URL ?? env.PUBLIC_API_URL) : env.PUBLIC_API_URL;

export async function listReviews(
  productId: number,
  opts: { filter?: ReviewFilter; page?: number; size?: number } = {},
): Promise<ReviewPage> {
  const filter = opts.filter ?? 'all';
  const size = opts.size ?? REVIEW_PAGE_SIZE;
  if (USE_MOCK) {
    await mockDelay();
    const all = reviewDtos(productId).map(toReview);
    const page = paginatePage(
      all.filter((r) => matchesFilter(r, filter)),
      { page: opts.page, size },
    );
    return { ...page, summary: summarize(all) };
  }
  const res = await request<ReviewDto[]>(`/public/products/${productId}/reviews`, {
    auth: false,
    baseUrl: baseUrl(),
    query: { filter, page: opts.page ?? 1, size },
  });
  const pg = isPagePagination(res.pagination)
    ? res.pagination
    : { page: 1, size, total_items: res.data.length, total_pages: 1 };
  const summary = (res as { summary?: ReviewSummaryDto }).summary;
  const items = res.data.map(toReview);
  return { items, ...pg, summary: summary ? toSummary(summary) : summarize(items) };
}

/** Bật / tắt "Hữu ích" — trả trạng thái mới. Cần đăng nhập. */
export async function toggleHelpful(reviewId: string): Promise<boolean> {
  if (USE_MOCK) {
    await mockDelay(150);
    return toggleMockHelpful(reviewId);
  }
  return (await api.post<{ helpful: boolean }>(`/reviews/${encodeURIComponent(reviewId)}/helpful`))
    .helpful;
}

/** Báo cáo đánh giá vi phạm (chuyển bộ phận kiểm duyệt). Cần đăng nhập. */
export async function reportReview(
  reviewId: string,
  reason: ReportReason,
  note?: string,
): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(250);
    reported.add(reviewId);
    return;
  }
  await api.post(`/reviews/${encodeURIComponent(reviewId)}/reports`, {
    reason,
    note: note?.trim() || undefined,
  });
}
