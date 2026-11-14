// Đánh giá phía shop (API_SPEC 10.8; prototype VendorPages.jsx Reviews, ReviewCard, DIST).
export type ReviewTab = 'ALL' | 'UNREPLIED' | 'LOW_RATING';

export interface ShopReview {
  id: number;
  reviewer: { name: string; avatarUrl: string | null };
  rating: number;
  productName: string;
  variantName: string | null;
  content: string;
  images: string[];
  createdAt: string;
  reply: { content: string; repliedAt: string } | null;
  reportedByMe: boolean;
}

export interface ReviewStats {
  avg: number;
  count: number;
  distribution: Record<1 | 2 | 3 | 4 | 5, number>;
  counts: Record<ReviewTab, number>;
}
