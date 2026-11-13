import type { Review, ReviewFilter, ReviewSummary } from '../domain/review.types';
import { REVIEW_FILTERS } from '../domain/review.types';
import type { ReviewDto, ReviewSummaryDto } from './review.dto';

export function toReview(d: ReviewDto): Review {
  return {
    id: d.id,
    productId: d.product_id,
    authorName: d.author_name,
    anonymous: d.is_anonymous,
    rating: d.rating,
    createdAt: d.created_at,
    variantLabel: d.variant_label,
    text: d.content,
    images: d.images,
    reply: d.seller_reply ? { text: d.seller_reply.content } : null,
    helpfulCount: d.helpful_count,
    helpfulByMe: d.helpful_by_me,
    verifiedPurchase: d.verified_purchase,
  };
}

export function toSummary(d: ReviewSummaryDto): ReviewSummary {
  const counts = Object.fromEntries(REVIEW_FILTERS.map((f) => [f, d.counts[f] ?? 0])) as Record<
    ReviewFilter,
    number
  >;
  return { average: d.average, total: d.total, counts };
}
