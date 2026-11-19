-- Dữ liệu mẫu 90 — ~180 sản phẩm sinh tự động để thử tìm kiếm, lọc, phân trang (plan seed 2026-11-16 S08, D4 mức "đầy").
-- SQL thuần, chạy lại được, không ghi cứng id. Kết quả TẤT ĐỊNH theo n (không random()) → chạy lại cho cùng dữ liệu.
-- Mỗi sản phẩm: tên '<loại> <thương hiệu> <mẫu> #<n>' (duy nhất), 1 SKU 'SEED-BULK-<n>', 1 ảnh xoay vòng 20 ảnh mẫu.
-- Loại hàng gắn đúng danh mục con + shop cùng ngành để trang danh mục / shop hiển thị hợp lý.

DROP TABLE IF EXISTS seed_bulk_kind;
CREATE TEMP TABLE seed_bulk_kind AS
SELECT * FROM (VALUES
    -- k  loại hàng             danh mục con            chủ shop                        thương hiệu  ảnh          giá gốc  thuộc tính   giá trị
    (0,  'Tai nghe',           'thiet-bi-am-thanh',    'mai.tran@gmail.com',           'Sony',      'headphones', 290000,  'warranty',  '12 tháng'),
    (1,  'Loa di động',        'thiet-bi-am-thanh',    'mai.tran@gmail.com',           'JBL',       'speaker',    350000,  'warranty',  '12 tháng'),
    (2,  'Bàn phím',           'may-tinh-laptop',      'mai.tran@gmail.com',           'Logitech',  'keyboard',   390000,  'warranty',  '24 tháng'),
    (3,  'Điện thoại',         'dien-thoai-phu-kien',  'mai.tran@gmail.com',           'Samsung',   'phone',      3990000, 'warranty',  '24 tháng'),
    (4,  'Đồng hồ thông minh', 'phu-kien-dien-tu',     'mai.tran@gmail.com',           'Xiaomi',    'watch',      790000,  'warranty',  '12 tháng'),
    (5,  'Giày thể thao',      'giay-dep',             'owner.shoe@genie.example',     'Biti''s',   'sneakers',   320000,  'material',  'Da'),
    (6,  'Giày nữ',            'giay-dep',             'owner.shoe@genie.example',     NULL,        'heels',      280000,  'material',  'Da'),
    (7,  'Áo thun',            'thoi-trang-nam',       'owner.boutique@genie.example', NULL,        'tshirt',     99000,   'material',  'Cotton'),
    (8,  'Áo khoác',           'thoi-trang-nam',       'owner.boutique@genie.example', NULL,        'jacket',     259000,  'material',  'Denim'),
    (9,  'Kính mát',           'thoi-trang-nu',        'owner.boutique@genie.example', NULL,        'sunglasses', 129000,  'material',  'Da'),
    (10, 'Cốc sứ',             'nha-bep',              'owner.home@genie.example',     NULL,        'mug',        69000,   'material',  'Sứ'),
    (11, 'Đèn ngủ',            'thiet-bi-gia-dung',    'owner.home@genie.example',     'Xiaomi',    'lamp',       159000,  'material',  'Nhựa'),
    (12, 'Ghế thư giãn',       'noi-that',             'owner.home@genie.example',     NULL,        'chair',      990000,  'material',  'Gỗ'),
    (13, 'Son môi',            'my-pham-cham-soc',     'owner.beauty@genie.example',   'Maybelline','cosmetics',  189000,  'skin_type', 'Mọi loại da'),
    (14, 'Kem dưỡng',          'my-pham-cham-soc',     'owner.beauty@genie.example',   'La Roche-Posay','skincare', 249000, 'skin_type', 'Da khô'),
    (15, 'Nước hoa',           'my-pham-cham-soc',     'owner.beauty@genie.example',   NULL,        'perfume',    390000,  'skin_type', 'Mọi loại da'),
    (16, 'Balo',               'tui-xach-balo',        'owner.travel@genie.example',   NULL,        'backpack',   219000,  'material',  'Vải'),
    (17, 'Đồng hồ dây da',     'dong-ho-trang-suc',    'owner.boutique@genie.example', NULL,        'watch2',     229000,  'material',  'Da')
) AS v(k, kind, category_slug, owner_email, brand, image, base_price, attr_code, attr_value);

DROP TABLE IF EXISTS seed_bulk;
CREATE TEMP TABLE seed_bulk AS
SELECT n, k.*,
       k.kind || ' ' || coalesce(k.brand, 'Genie') || ' '
           || (ARRAY['Basic', 'Plus', 'Pro', 'Lite', 'Max', 'Mini', 'Classic', 'Air', 'Neo', 'Prime'])[(n / 18) % 10 + 1]
           || ' #' || n AS name,
       -- giá tất định: dao động ±40% quanh giá gốc, làm tròn nghìn
       round(k.base_price * (0.6 + ((n * 37) % 81) / 100.0), -3) AS price,
       (n * 53) % 5000 AS sold,
       round((4.0 + ((n * 7) % 10) / 10.0)::numeric, 1) AS rating,
       ((n * 13) % 200) + 5 AS stock
FROM generate_series(1, 180) AS n
JOIN seed_bulk_kind k ON k.k = n % 18;

INSERT INTO products (vendor_id, category_id, name, description, brand, brand_id, item_condition, origin, specs_json,
                      weight, ships_from, shipping_payer, base_price, status, is_published, version,
                      created_at, updated_at, sold, rating, is_pre_order, pre_order_days, view_count)
SELECT u.id, c.id, b.name, b.name || ' — sản phẩm mẫu sinh tự động để thử tìm kiếm và phân trang.',
       coalesce(b.brand, 'No Brand'), br.id, 'NEW', 'Việt Nam', jsonb_build_object(b.attr_code, b.attr_value),
       500, vp.pickup_province, CASE WHEN b.n % 3 = 0 THEN 'SELLER' ELSE 'BUYER' END,
       b.price, 'PUBLISHED', TRUE, 0,
       now() - make_interval(days => 200 - b.n), now(), b.sold, b.rating, FALSE, NULL, b.sold * 5
FROM seed_bulk b
JOIN users u ON u.email = b.owner_email
JOIN vendor_profiles vp ON vp.user_id = u.id
LEFT JOIN categories c ON c.slug = b.category_slug
LEFT JOIN brands br ON br.name = b.brand
WHERE NOT EXISTS (SELECT 1 FROM products x WHERE x.vendor_id = u.id AND x.name = b.name);

INSERT INTO product_variants (product_id, sku, variant_name, attributes_json, price, stock_count, reserved_count, version)
SELECT p.id, 'SEED-BULK-' || b.n, 'Mặc định', NULL, b.price, b.stock, 0, 0
FROM seed_bulk b
JOIN users u ON u.email = b.owner_email
JOIN products p ON p.vendor_id = u.id AND p.name = b.name
ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_images (product_id, image_url, width, height, alt_text, is_primary, mime_type, display_order)
SELECT p.id, 'seed/product/' || b.image || '.jpg', 800, 800, b.name, TRUE, 'image/jpeg', 0
FROM seed_bulk b
JOIN users u ON u.email = b.owner_email
JOIN products p ON p.vendor_id = u.id AND p.name = b.name
WHERE NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = p.id);

UPDATE vendor_profiles vp
SET active_product_count = (SELECT count(*) FROM products p WHERE p.vendor_id = vp.user_id AND p.status = 'PUBLISHED')
WHERE vp.kyc_status = 'VERIFIED';

DROP TABLE seed_bulk;
DROP TABLE seed_bulk_kind;
