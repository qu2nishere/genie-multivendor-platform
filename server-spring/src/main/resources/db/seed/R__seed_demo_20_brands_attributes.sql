-- Dữ liệu mẫu 20 — thương hiệu + thuộc tính lọc theo ngành (plan seed 2026-11-16 S02.04–S02.05; mock web
-- SEED_PRODUCTS[].brand, SEED_VERIFIED_BRANDS, SEED_ATTR_FILTERS). SQL thuần, chạy lại được, không ghi cứng id.
-- "No Brand" của mock không phải thương hiệu → sản phẩm để brand_id NULL, cột products.brand = 'No Brand'.

-- ===== brands (slug không UNIQUE ở V2 → NOT EXISTS theo slug) =====
WITH v(name, slug, is_verified) AS (VALUES
    ('Sony', 'sony', TRUE), ('Samsung', 'samsung', TRUE), ('Biti''s', 'bitis', TRUE), ('Xiaomi', 'xiaomi', TRUE),
    ('Asus', 'asus', TRUE), ('Maybelline', 'maybelline', TRUE), ('La Roche-Posay', 'la-roche-posay', TRUE),
    ('JBL', 'jbl', TRUE), ('Logitech', 'logitech', TRUE), ('Chanel', 'chanel', TRUE)
), upd AS (
    UPDATE brands b SET name = v.name, is_verified = v.is_verified FROM v WHERE b.slug = v.slug RETURNING b.slug
)
INSERT INTO brands (name, slug, logo_url, is_verified, created_by_user_id, created_at)
SELECT v.name, v.slug, NULL, v.is_verified, NULL, now()
FROM v
WHERE NOT EXISTS (SELECT 1 FROM brands b WHERE b.slug = v.slug);

-- ===== category_attributes — thuộc tính lọc theo danh mục gốc (uq_category_attribute_code) =====
INSERT INTO category_attributes (category_id, code, name, data_type, allowed_values_json, unit, is_required,
                                 is_filterable, is_variant_axis, display_order, is_active, created_at, updated_at)
SELECT c.id, v.code, v.name, v.data_type, v.allowed::jsonb, NULL, FALSE, TRUE, v.variant_axis, v.display_order, TRUE, now(), now()
FROM (VALUES
    ('dien-tu',    'warranty',  'Bảo hành',  'ENUM', '["12 tháng", "24 tháng"]',              FALSE, 1),
    ('dien-tu',    'color',     'Màu sắc',   'ENUM', '["Đen", "Trắng", "Bạc", "Xanh"]',       TRUE,  2),
    ('thoi-trang', 'material',  'Chất liệu', 'ENUM', '["Cotton", "Da", "Denim"]',             FALSE, 1),
    ('thoi-trang', 'size',      'Kích cỡ',   'ENUM', '["S", "M", "L", "XL", "38", "39", "40", "41", "42"]', TRUE, 2),
    ('lam-dep',    'skin_type', 'Loại da',   'ENUM', '["Mọi loại da", "Da dầu", "Da khô"]',   FALSE, 1),
    ('nha-cua',    'material',  'Chất liệu', 'ENUM', '["Sứ", "Gỗ", "Nhựa"]',                  FALSE, 1),
    ('phu-kien',   'material',  'Chất liệu', 'ENUM', '["Da", "Vải"]',                         FALSE, 1)
) AS v(category_slug, code, name, data_type, allowed, variant_axis, display_order)
JOIN categories c ON c.slug = v.category_slug AND c.parent_id IS NULL
ON CONFLICT (category_id, code) DO UPDATE
SET name = EXCLUDED.name, data_type = EXCLUDED.data_type, allowed_values_json = EXCLUDED.allowed_values_json,
    is_filterable = EXCLUDED.is_filterable, is_variant_axis = EXCLUDED.is_variant_axis,
    display_order = EXCLUDED.display_order, is_active = TRUE, updated_at = now();
