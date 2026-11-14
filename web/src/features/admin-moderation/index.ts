// API công khai của feature "admin-moderation" — kiểm duyệt đánh giá (/admin/reviews) và duyệt sản phẩm
// (/admin/product-moderation, /admin/product-moderation/:id).
export type { ReviewReport } from './domain/review.types';
export type { ModerationRow, ModerationDetail } from './domain/moderation.types';
export { default as ReviewModerationPage } from './ui/ReviewModerationPage';
export { default as ProductModerationPage } from './ui/ProductModerationPage';
export { default as ProductModerationDetail } from './ui/ProductModerationDetail';
