-- V26 — ảnh theo giá trị phân loại cho NHIỀU nhóm (plan C1, yêu cầu chủ dự án 2026-11-27; ENTITY 2.2).
-- Trước đây chỉ một nhóm có ảnh (variant_image_attribute) và ảnh nằm trên từng SKU (product_variants.image_url).
-- Cột mới lưu ảnh của mọi nhóm: {"<tên nhóm>": {"<giá trị>": "<khóa ảnh trên Silo>"}}.
-- variant_image_attribute giữ nghĩa "nhóm ảnh chính" — ảnh của SKU (giỏ, đơn hàng) lấy theo nhóm này.
-- NULL được → code / seed cũ không phải đổi; sản phẩm cũ đọc ảnh từ SKU như trước.
ALTER TABLE products
    ADD COLUMN option_images_json JSONB;
