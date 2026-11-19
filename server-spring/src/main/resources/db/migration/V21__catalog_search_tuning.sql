-- V21 — Tinh chỉnh tìm kiếm sản phẩm (plan 2026-11-17 C01.01, quyết định D3, D4; ENTITY 13.1; UC-CAT-02, 04, 05, 07)
--
-- 1) search_vector có TRỌNG SỐ: tên (A) > thương hiệu (B) > mô tả (D) → ts_rank_cd xếp sản phẩm khớp tên lên trước.
--    PostgreSQL 16 (RDS) chưa có ALTER COLUMN … SET EXPRESSION cho cột sinh → xóa cột rồi tạo lại (index đi theo cột).
-- 2) Index trigram trên tên ĐÃ BỎ DẤU + chữ thường: gợi ý khi gõ / bù gõ sai bằng word_similarity ("tai nge" ≈ "Tai nghe").
-- 3) Index giá theo SKU còn bán: lọc / sắp theo giá thấp nhất (min_price) và giá ±50% của sản phẩm tương tự.

ALTER TABLE products DROP COLUMN search_vector;

ALTER TABLE products ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', immutable_unaccent(lower(coalesce(name, '')))), 'A') ||
        setweight(to_tsvector('simple', immutable_unaccent(lower(coalesce(brand, '')))), 'B') ||
        setweight(to_tsvector('simple', immutable_unaccent(lower(coalesce(description, '')))), 'D')
    ) STORED;

CREATE INDEX idx_products_search_vector ON products USING GIN (search_vector);

CREATE INDEX idx_products_name_unaccent_trgm ON products
    USING GIN (immutable_unaccent(lower(name)) gin_trgm_ops);

CREATE INDEX idx_product_variants_product_price ON product_variants (product_id, price)
    WHERE deleted_at IS NULL;

-- Lưới công khai lọc theo trạng thái + sắp theo bán chạy / mới nhất
CREATE INDEX idx_products_status_sold ON products (status, sold DESC NULLS LAST);

-- Gợi ý theo tiền tố của danh mục / shop (tên ít, quét trong bộ nhớ là đủ — không thêm index)
