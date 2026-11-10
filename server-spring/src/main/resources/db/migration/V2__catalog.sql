-- Nhóm 2 — Danh mục & sản phẩm
-- Bảng: categories, category_attributes, brands, products, product_variants, product_images, product_documents, category_proposals, product_moderation_results, search_keyword_stats, product_view_stats, user_wishlists
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>

-- TODO
