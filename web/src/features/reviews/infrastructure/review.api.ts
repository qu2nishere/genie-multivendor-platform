// Đánh giá sản phẩm: GET /public/products/{id}/reviews (kèm summary), POST /reviews/{id}/helpful,
// POST /reviews/{id}/reports. Mock khi USE_MOCK.
import { api, request } from '@/shared/api/http';
import { mockDelay, mockFail, paginatePage, USE_MOCK } from '@/shared/api/mock';
import { activityStore } from '@/shared/api/mock/activity';
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

/** Dữ liệu gửi đánh giá một dòng hàng (API_SPEC 7.9). */
export interface ReviewInput {
  orderId: number;
  orderItemId: number;
  rating: number;
  content: string;
  imageKeys: string[];
  videoKey: string | null;
  anonymous: boolean;
}

/** POST /me/reviews — +50 Điểm khi có ảnh; đã đánh giá → 409 GNE-SOC-4091. */
export async function createReview(
  input: ReviewInput,
): Promise<{ id: number; coinsAwarded: number }> {
  if (USE_MOCK) {
    await mockDelay(400);
    const db = activityStore.get();
    if (db.reviews.some((r) => r.order_item_id === input.orderItemId)) mockFail('GNE-SOC-4091');
    const coins = input.imageKeys.length ? 50 : 0;
    const id = 5500 + db.reviews.length + 1;
    activityStore.set({
      ...db,
      reviews: [
        ...db.reviews,
        {
          id,
          order_id: input.orderId,
          order_item_id: input.orderItemId,
          rating: input.rating,
          with_media: input.imageKeys.length > 0 || !!input.videoKey,
          coins_awarded: coins,
          created_at: new Date().toISOString(),
        },
      ],
    });
    return { id, coinsAwarded: coins };
  }
  const r = await api.post<{ id: number; coins_awarded: number }>('/me/reviews', {
    order_item_id: input.orderItemId,
    rating: input.rating,
    content: input.content,
    image_object_keys: input.imageKeys,
    video_object_key: input.videoKey,
    is_anonymous: input.anonymous,
  });
  return { id: r.id, coinsAwarded: r.coins_awarded };
}
