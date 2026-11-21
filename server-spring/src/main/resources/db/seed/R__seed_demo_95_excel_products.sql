-- Dữ liệu mẫu 95 — sản phẩm nhập từ file Excel (plan 2026-11-21 db/plan_seed_tu_excel.md).
-- FILE SINH TỰ ĐỘNG bởi scripts/seed/excel/excel_to_seed.py — KHÔNG SỬA TAY: sửa file Excel rồi chạy
--   python scripts/seed/excel/excel_to_seed.py build <file.xlsx>      (hướng dẫn: scripts/seed/excel/README.md)
-- SQL thuần, chạy lại được, không ghi cứng id: sản phẩm khớp theo (email chủ shop, tên); SKU 'XL-<ma_sp>-<ma_bien_the>'.
-- Ảnh lưu khóa đối tượng 'seed/excel/<băm8>.jpg' trong bucket product-media — ảnh ở scripts/seed/excel/assets/,
-- đẩy lên Silo bằng scripts/seed/upload-assets.sh | .ps1. Ô ảnh là link https thì lưu nguyên link.
-- Nguồn: products.xlsx · 4 sản phẩm, 7 biến thể, 8 ảnh.

DROP TABLE IF EXISTS xl_product, xl_variant, xl_image, xl_product_id;

CREATE TEMP TABLE xl_product (
    code TEXT PRIMARY KEY, owner_email TEXT NOT NULL, category_slug TEXT NOT NULL, name TEXT NOT NULL, description TEXT,
    brand TEXT, item_condition TEXT NOT NULL, origin TEXT, specs JSONB, weight_gram DOUBLE PRECISION NOT NULL,
    length_cm DOUBLE PRECISION, width_cm DOUBLE PRECISION, height_cm DOUBLE PRECISION, free_ship BOOLEAN NOT NULL,
    pre_order_days INT, status TEXT NOT NULL, sold INT NOT NULL, rating DOUBLE PRECISION, base_price NUMERIC(19,2) NOT NULL
);
INSERT INTO xl_product VALUES
    ('XL01', 'mai.tran@gmail.com', 'thiet-bi-am-thanh', 'Tai nghe chụp tai Studio Max chống ồn chủ động, pin 60 giờ', 'Tai nghe chụp tai không dây, chống ồn chủ động 3 mức.
Kết nối Bluetooth 5.3, sạc nhanh 10 phút nghe 5 giờ.', 'Sony', 'NEW', 'Malaysia', '{"color": "Đen", "warranty": "12 tháng"}'::jsonb, 320.0, 22.0, 18.0, 9.0, TRUE, NULL, 'PUBLISHED', 152, 4.8, 1490000),
    ('XL02', 'owner.boutique@genie.example', 'thoi-trang-nam', 'Áo thun oversize cotton 250gsm in hình Genie', 'Cotton 100%, form rộng, không xù lông sau khi giặt.', NULL, 'NEW', 'Việt Nam', '{"material": "Cotton"}'::jsonb, 280.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 420, 4.6, 149000),
    ('XL03', 'owner.home@genie.example', 'thiet-bi-gia-dung', 'Đèn ngủ gốm men lam thủ công Bát Tràng', 'Thân gốm vẽ tay, ánh sáng vàng ấm, công tắc cảm ứng.', 'No Brand', 'NEW', 'Việt Nam', '{"material": "Sứ"}'::jsonb, 900.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 37, 4.9, 329000),
    ('XL04', 'owner.travel@genie.example', 'tui-xach-balo', 'Balo du lịch 40L chống nước, ngăn laptop 16 inch', NULL, 'Xiaomi', 'NEW', NULL, '{"material": "Vải"}'::jsonb, 1100.0, 50.0, 32.0, 20.0, FALSE, 7, 'PUBLISHED', 0, NULL, 459000);

CREATE TEMP TABLE xl_variant (
    product_code TEXT NOT NULL, sku TEXT PRIMARY KEY, variant_name TEXT, attributes JSONB, price NUMERIC(19,2) NOT NULL,
    stock INT NOT NULL, image_url TEXT
);
INSERT INTO xl_variant VALUES
    ('XL01', 'XL-XL01-BLK', 'Đen', '{"color": "Đen"}'::jsonb, 1490000, 40, 'seed/excel/56769b09.jpg'),
    ('XL01', 'XL-XL01-SLV', 'Bạc', '{"color": "Bạc"}'::jsonb, 1550000, 12, NULL),
    ('XL02', 'XL-XL02-M', 'Size M', '{"size": "M"}'::jsonb, 149000, 80, NULL),
    ('XL02', 'XL-XL02-L', 'Size L', '{"size": "L"}'::jsonb, 149000, 60, NULL),
    ('XL02', 'XL-XL02-XL', 'Size XL', '{"size": "XL"}'::jsonb, 149000, 0, NULL),
    ('XL03', 'XL-XL03-STD', 'Mặc định', NULL, 329000, 25, NULL),
    ('XL04', 'XL-XL04-STD', 'Mặc định', NULL, 459000, 15, NULL);

CREATE TEMP TABLE xl_image (
    product_code TEXT NOT NULL, sku TEXT, image_url TEXT NOT NULL, width INT, height INT, file_size BIGINT,
    mime_type TEXT, is_primary BOOLEAN NOT NULL, display_order INT NOT NULL
);
INSERT INTO xl_image VALUES
    ('XL01', NULL, 'seed/excel/56769b09.jpg', 480, 480, 21105, 'image/jpeg', TRUE, 0),
    ('XL01', NULL, 'seed/excel/89baf85d.jpg', 480, 480, 40450, 'image/jpeg', FALSE, 1),
    ('XL01', 'XL-XL01-BLK', 'seed/excel/56769b09.jpg', 480, 480, 21105, 'image/jpeg', FALSE, 2),
    ('XL02', NULL, 'seed/excel/a9596e10.jpg', 480, 480, 22104, 'image/jpeg', TRUE, 0),
    ('XL02', NULL, 'seed/excel/401359cc.jpg', 480, 480, 42613, 'image/jpeg', FALSE, 1),
    ('XL03', NULL, 'seed/excel/ef270230.jpg', 480, 480, 10823, 'image/jpeg', TRUE, 0),
    ('XL03', NULL, 'seed/excel/88c3a3aa.jpg', 480, 480, 7081, 'image/jpeg', FALSE, 1),
    ('XL04', NULL, 'seed/excel/fde92203.jpg', 480, 480, 19517, 'image/jpeg', TRUE, 0);

-- ===== Kiểm tham chiếu trước khi ghi (X5): chủ shop phải có shop VERIFIED, danh mục phải tồn tại =====
DO $$
DECLARE bad TEXT;
BEGIN
    SELECT string_agg(x.code || ' (' || x.owner_email || ')', ', ' ORDER BY x.code) INTO bad
    FROM xl_product x
    WHERE NOT EXISTS (SELECT 1 FROM users u JOIN vendor_profiles vp ON vp.user_id = u.id
                      WHERE lower(u.email) = x.owner_email AND vp.kyc_status = 'VERIFIED');
    IF bad IS NOT NULL THEN
        RAISE EXCEPTION 'Seed Excel: chưa có shop VERIFIED của chủ shop: %', bad;
    END IF;
    SELECT string_agg(x.code || ' (' || x.category_slug || ')', ', ' ORDER BY x.code) INTO bad
    FROM xl_product x WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.slug = x.category_slug);
    IF bad IS NOT NULL THEN
        RAISE EXCEPTION 'Seed Excel: không có danh mục: %', bad;
    END IF;
END $$;

-- ===== 1) Sản phẩm Excel đã bỏ khỏi file (xóa dòng / đổi tên) → ẩn (D4) =====
UPDATE products p
SET status = 'HIDDEN', is_published = FALSE, version = p.version + 1, updated_at = now()
WHERE p.status <> 'HIDDEN'
  AND EXISTS (SELECT 1 FROM product_variants v WHERE v.product_id = p.id AND v.sku LIKE 'XL-%')
  AND NOT EXISTS (SELECT 1 FROM xl_product x JOIN users u ON lower(u.email) = x.owner_email
                  WHERE p.vendor_id = u.id AND p.name = x.name);

-- ===== 2) Cập nhật sản phẩm đã có (khớp chủ shop + tên) =====
UPDATE products p
SET category_id = (SELECT min(c.id) FROM categories c WHERE c.slug = x.category_slug),
    description = x.description, brand = x.brand,
    brand_id = (SELECT min(b.id) FROM brands b WHERE lower(b.name) = lower(x.brand)),
    item_condition = x.item_condition, origin = x.origin, specs_json = x.specs,
    weight = x.weight_gram, length = x.length_cm, width = x.width_cm, height = x.height_cm,
    ships_from = vp.pickup_province, shipping_payer = CASE WHEN x.free_ship THEN 'SELLER' ELSE 'BUYER' END,
    base_price = x.base_price, status = x.status, is_published = (x.status = 'PUBLISHED'),
    sold = x.sold, rating = x.rating, is_pre_order = x.pre_order_days IS NOT NULL, pre_order_days = x.pre_order_days,
    version = p.version + 1, updated_at = now()
FROM xl_product x
JOIN users u ON lower(u.email) = x.owner_email
JOIN vendor_profiles vp ON vp.user_id = u.id
WHERE p.vendor_id = u.id AND p.name = x.name;

-- ===== 3) Thêm sản phẩm mới =====
INSERT INTO products (vendor_id, category_id, name, description, brand, brand_id, item_condition, origin, specs_json,
                      weight, length, width, height, ships_from, shipping_payer, base_price, status, is_published,
                      version, created_at, updated_at, sold, rating, is_pre_order, pre_order_days, view_count)
SELECT u.id, (SELECT min(c.id) FROM categories c WHERE c.slug = x.category_slug), x.name, x.description, x.brand,
       (SELECT min(b.id) FROM brands b WHERE lower(b.name) = lower(x.brand)), x.item_condition, x.origin, x.specs,
       x.weight_gram, x.length_cm, x.width_cm, x.height_cm, vp.pickup_province,
       CASE WHEN x.free_ship THEN 'SELLER' ELSE 'BUYER' END, x.base_price, x.status, x.status = 'PUBLISHED',
       0, now(), now(), x.sold, x.rating, x.pre_order_days IS NOT NULL, x.pre_order_days, 0
FROM xl_product x
JOIN users u ON lower(u.email) = x.owner_email
JOIN vendor_profiles vp ON vp.user_id = u.id
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.vendor_id = u.id AND p.name = x.name)
ORDER BY x.code;

CREATE TEMP TABLE xl_product_id AS
SELECT x.code, (SELECT min(p.id) FROM products p WHERE p.vendor_id = u.id AND p.name = x.name) AS product_id
FROM xl_product x
JOIN users u ON lower(u.email) = x.owner_email;

-- ===== 4) Biến thể: 'XL-%' không còn trong file → xóa mềm; còn lại upsert theo SKU (D6) =====
UPDATE product_variants v
SET deleted_at = now(), version = v.version + 1
WHERE v.sku LIKE 'XL-%' AND v.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM xl_variant x WHERE x.sku = v.sku);

INSERT INTO product_variants (product_id, sku, variant_name, attributes_json, price, stock_count, reserved_count,
                              version, image_url)
SELECT m.product_id, x.sku, x.variant_name, x.attributes, x.price, x.stock, 0, 0, x.image_url
FROM xl_variant x
JOIN xl_product_id m ON m.code = x.product_code
ORDER BY x.sku
ON CONFLICT (sku) DO UPDATE
SET product_id = EXCLUDED.product_id, variant_name = EXCLUDED.variant_name, attributes_json = EXCLUDED.attributes_json,
    price = EXCLUDED.price, stock_count = EXCLUDED.stock_count, image_url = EXCLUDED.image_url, deleted_at = NULL,
    version = product_variants.version + 1;

-- ===== 5) Ảnh: xóa ảnh do công cụ tạo của các sản phẩm này rồi chèn lại theo file =====
DELETE FROM product_images i
USING xl_product_id m
WHERE i.product_id = m.product_id AND (i.image_url LIKE 'seed/excel/%' OR i.image_url LIKE 'https://%');

INSERT INTO product_images (product_id, variant_id, image_url, width, height, file_size, alt_text, is_primary,
                            mime_type, display_order)
SELECT m.product_id, v.id, x.image_url, x.width, x.height, x.file_size,
       left(CASE WHEN v.id IS NULL THEN p.name ELSE p.name || ' — ' || v.variant_name END, 500),
       x.is_primary, x.mime_type, x.display_order
FROM xl_image x
JOIN xl_product_id m ON m.code = x.product_code
JOIN xl_product p ON p.code = x.product_code
LEFT JOIN product_variants v ON v.sku = x.sku
ORDER BY x.product_code, x.display_order;

-- ===== 6) Đồng bộ bộ đếm sản phẩm đang bán của shop (như file 40) =====
UPDATE vendor_profiles vp
SET active_product_count = (SELECT count(*) FROM products p WHERE p.vendor_id = vp.user_id AND p.status = 'PUBLISHED')
WHERE vp.kyc_status = 'VERIFIED';

DROP TABLE xl_product_id;
DROP TABLE xl_image;
DROP TABLE xl_variant;
DROP TABLE xl_product;
