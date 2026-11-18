// Ghi lượt xem khi mở trang sản phẩm (UC-CAT-06 bước 9, UC-CAT-18 bước 5; API_SPEC 5.7; plan 2026-11-17 C07.04).
// Gọi một lần từ <script> của trang /p/{slug}-{id}. `src` do trang đọc lúc SSR (BuyBox xóa ?src khỏi URL khi hydrate).
import { viewSourceOf } from '../domain/product.rules';
import { recordView } from '../infrastructure/product.api';

export function trackProductView(productId: number, src: string | null): void {
  if (!Number.isInteger(productId) || productId <= 0) return;
  recordView(productId, viewSourceOf(src, document.referrer, window.location.origin));
}
