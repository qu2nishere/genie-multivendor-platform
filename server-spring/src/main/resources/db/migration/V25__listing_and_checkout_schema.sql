-- V25 — migration chung của chuỗi C1 Đăng bán (UC-CAT-09…13, 17; plan/2026-11-26/c1.md D1) và
--       chuỗi C2 Giỏ → Checkout (UC-ORD-01, 07, 08; plan/2026-11-25/server-spring/plan_gio_hang_checkout_c2.md D15).
-- Merge vào develop TRƯỚC khi hai chuỗi viết nghiệp vụ; sau khi merge không sửa file này — cần thêm thì V26+ theo thứ tự merge.
-- Cột mới đều NULL được hoặc có DEFAULT → code / seed cũ insert sản phẩm không phải đổi (ENTITY 2.2, 3.2, 5.1, 13.1).

-- ===== C1 — products: hàng chờ duyệt, sàn khóa, video =====
ALTER TABLE products
    ADD COLUMN submitted_at TIMESTAMPTZ,                          -- lần gửi duyệt gần nhất: sắp hàng chờ, đối chiếu kết quả AI cũ / mới
    ADD COLUMN is_requeued  BOOLEAN NOT NULL DEFAULT FALSE,       -- "Chờ duyệt lại": sửa trường quan trọng của sản phẩm đã bán / Staff đưa lại hàng chờ
    ADD COLUMN is_locked    BOOLEAN NOT NULL DEFAULT FALSE,       -- sàn khóa (đi kèm status = HIDDEN): shop không tự mở bán lại
    ADD COLUMN video_url    VARCHAR(500);                         -- khóa video sản phẩm trên Silo (bucket product-media)

-- Hàng chờ kiểm duyệt (UC-CAT-13): lọc theo trạng thái + người được giao, sắp theo lúc gửi duyệt
CREATE INDEX idx_products_moderation_queue ON products (status, moderator_id, submitted_at)
    WHERE status IN ('PENDING_APPROVAL', 'FLAGGED');

-- ===== C2 — giỏ hàng & phiên checkout =====
-- Một dòng / biến thể trong mỗi giỏ: thêm lại cùng SKU thì cộng dồn (INSERT … ON CONFLICT) — UC-ORD-01
CREATE UNIQUE INDEX uk_cart_items_cart_variant ON cart_items (cart_id, product_variant_id);

-- Cron hủy phiên quá hạn thanh toán & nhả tồn (UC-ORD-08): chỉ phiên còn mở
CREATE INDEX idx_checkout_sessions_open_expires ON checkout_sessions (status, expires_at)
    WHERE status IN ('INITIATED', 'PAYMENT_PENDING');
