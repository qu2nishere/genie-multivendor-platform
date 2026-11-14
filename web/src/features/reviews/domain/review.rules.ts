// Quy tắc đánh giá — TS thuần, có test.
import type { Review, ReviewFilter, ReviewSummary } from './review.types';
import { REVIEW_FILTERS } from './review.types';

/** Đánh giá ẩn tên: "Lê Hoàng" → "L***g" (prototype name[0] + '***' + name[last]). */
export function maskName(name: string): string {
  const s = name.trim();
  if (s.length <= 1) return `${s}***`;
  return `${s[0]}***${s[s.length - 1]}`;
}

export const displayName = (r: Pick<Review, 'authorName' | 'anonymous'>): string =>
  r.anonymous ? maskName(r.authorName) : r.authorName;

export function matchesFilter(
  r: Pick<Review, 'rating' | 'images' | 'verifiedPurchase'>,
  f: ReviewFilter,
): boolean {
  switch (f) {
    case 'all':
      return true;
    case 'img':
      return r.images.length > 0;
    case 'buy':
      return r.verifiedPurchase;
    default:
      return r.rating === Number(f);
  }
}

/** Tổng hợp số lượng theo bộ lọc + điểm trung bình (1 chữ số thập phân). */
export function summarize(
  list: Pick<Review, 'rating' | 'images' | 'verifiedPurchase'>[],
): ReviewSummary {
  const counts = Object.fromEntries(
    REVIEW_FILTERS.map((f) => [f, list.filter((r) => matchesFilter(r, f)).length]),
  ) as Record<ReviewFilter, number>;
  const average = list.length
    ? Math.round((list.reduce((s, r) => s + r.rating, 0) / list.length) * 10) / 10
    : 0;
  return { average, total: list.length, counts };
}

export const isReviewFilter = (v: string | null | undefined): v is ReviewFilter =>
  !!v && (REVIEW_FILTERS as readonly string[]).includes(v);
