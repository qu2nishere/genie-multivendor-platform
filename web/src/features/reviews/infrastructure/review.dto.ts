// Dạng dữ liệu API đánh giá (snake_case — API_SPEC).
export interface ReviewDto {
  id: string;
  product_id: number;
  author_name: string;
  is_anonymous: boolean;
  rating: number;
  created_at: string;
  variant_label: string;
  content: string;
  images: string[];
  seller_reply: { content: string } | null;
  helpful_count: number;
  helpful_by_me: boolean;
  verified_purchase: boolean;
}

export interface ReviewSummaryDto {
  average: number;
  total: number;
  counts: Record<string, number>;
}
