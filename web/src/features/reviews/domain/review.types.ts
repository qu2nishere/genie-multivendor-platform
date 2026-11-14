// Đánh giá sản phẩm (prototype ProductDetail.jsx REVIEWS, R_FILTERS, REPORT_REASONS) — plan G08.32.

export interface ReviewReply {
  text: string;
}

export interface Review {
  id: string;
  productId: number;
  /** Tên hiển thị gốc; đánh giá ẩn tên được che bằng maskName() ở UI. */
  authorName: string;
  anonymous: boolean;
  rating: number;
  /** ISO date (YYYY-MM-DD). */
  createdAt: string;
  /** Phân loại đã mua ("Đen / Bản Pro"). */
  variantLabel: string;
  text: string;
  images: string[];
  /** Phản hồi của người bán. */
  reply: ReviewReply | null;
  helpfulCount: number;
  /** Người xem hiện tại đã bấm "Hữu ích". */
  helpfulByMe: boolean;
  /** Mua hàng đã xác thực (mọi đánh giá đều gắn với đơn đã nhận). */
  verifiedPurchase: boolean;
}

/** Bộ lọc đánh giá (prototype R_FILTERS). */
export type ReviewFilter = 'all' | '5' | '4' | '3' | 'img' | 'buy';
export const REVIEW_FILTERS: readonly ReviewFilter[] = ['all', '5', '4', '3', 'img', 'buy'];

/** Lý do báo cáo đánh giá (prototype REPORT_REASONS) — mã gửi backend, nhãn ở i18n. */
export type ReportReason = 'SPAM' | 'OFFENSIVE' | 'FAKE_BUYER';
export const REPORT_REASONS: readonly ReportReason[] = ['SPAM', 'OFFENSIVE', 'FAKE_BUYER'];

/** Số đánh giá theo từng bộ lọc + điểm trung bình (GET …/reviews trả kèm `summary`). */
export interface ReviewSummary {
  average: number;
  total: number;
  counts: Record<ReviewFilter, number>;
}

/** Một trang đánh giá + tổng hợp (phân trang số trang — API_SPEC 0.4). */
export interface ReviewPage {
  items: Review[];
  page: number;
  size: number;
  total_items: number;
  total_pages: number;
  summary: ReviewSummary;
}
