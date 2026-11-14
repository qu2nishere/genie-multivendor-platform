// Đánh giá bị báo cáo (prototype admin/data.jsx REVIEWS) — hình DTO 11.5.
import { mockState } from '@/shared/api/mock';
import type { ReviewReportDto } from './review.dto';

export const reviewReportsStore = mockState<ReviewReportDto[]>('admin.reviewReports', () => [
  {
    review_id: 9921,
    code: 'RV-9921',
    product_name: 'Tai nghe ANC Pro 5',
    reviewer_masked: 'kh***92',
    rating: 1,
    content: 'Sản phẩm rác, shop lừa đảo, đừng ai mua *** số điện thoại 09xx để mua ngoài rẻ hơn',
    report_count: 3,
    top_reason: 'SPAM',
    reported_by_vendor: false,
    state: 'PENDING',
    sentiment: { label: 'NEGATIVE', score: 0.96 },
  },
  {
    review_id: 9918,
    code: 'RV-9918',
    product_name: 'Serum Vitamin C',
    reviewer_masked: 'an***le',
    rating: 2,
    content: 'Đóng gói tệ, giao chậm 3 ngày so với hẹn.',
    report_count: 1,
    top_reason: 'FAKE_PURCHASE',
    reported_by_vendor: true,
    state: 'PENDING',
    sentiment: { label: 'NEGATIVE', score: 0.71 },
  },
  {
    review_id: 9910,
    code: 'RV-9910',
    product_name: 'Giày sneaker Air Run',
    reviewer_masked: 'min***',
    rating: 5,
    content: 'Ngôn từ thô tục ***',
    report_count: 5,
    top_reason: 'OFFENSIVE',
    reported_by_vendor: false,
    state: 'HIDDEN',
    sentiment: null,
  },
]);
