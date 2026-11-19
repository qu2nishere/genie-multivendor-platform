-- Dữ liệu mẫu 40 — 20 sản phẩm PUBLISHED + biến thể SKU + ảnh (plan seed 2026-11-16 S04.02–S04.06; mock web SEED_PRODUCTS).
-- SQL thuần, chạy lại được, không ghi cứng id.
-- - Bảng products KHÔNG có cột slug (slug URL /p/{slug}-{id} do backend sinh từ tên) → chống trùng theo (vendor_id, name).
-- - Không có cột giá gốc: base_price = giá bán của mock (price); giá gạch ngang thuộc Flash Sale / khuyến mãi — ngoài phạm vi.
-- - Danh mục: danh mục con theo SEED_SUB_CATEGORIES[].product_ids (sản phẩm 15 thuộc cả nam & nữ → lấy Thời trang Nam).
-- - Ảnh (N7): image_url lưu KHÓA ĐỐI TƯỢNG 'seed/product/<tệp>' trong bucket product-media — URL do backend ghép theo kho
--   tệp đang dùng; đẩy ảnh lên Silo bằng scripts/seed/upload-assets.sh | .ps1.
-- - weight tính bằng gram (ENTITY — GHN tính cước). search_vector là cột GENERATED nên tự điền khi INSERT (S04.05).

DROP TABLE IF EXISTS seed_product;
CREATE TEMP TABLE seed_product AS
SELECT * FROM (VALUES
    -- mã  chủ shop                         danh mục con            tên                                                   ảnh           giá       đã bán  sao  thương hiệu       thuộc tính   giá trị       free_ship pre_order cân(g)
    (1,  'mai.tran@gmail.com',            'thiet-bi-am-thanh',    'Tai nghe Bluetooth chống ồn ANC Pro 5 — Pin 40 giờ',  'headphones', 389000,   2100,  4.8, 'Sony',           'warranty',  '12 tháng',    TRUE,  NULL, 350),
    (2,  'mai.tran@gmail.com',            'phu-kien-dien-tu',     'Đồng hồ thông minh Watch S2 màn AMOLED, đo SpO2',     'watch',      690000,   986,   4.7, 'Samsung',        'warranty',  '24 tháng',    TRUE,  NULL, 200),
    (3,  'owner.shoe@genie.example',      'giay-dep',             'Giày sneaker thể thao Air Run nam nữ êm chân',        'sneakers',   259000,   5400,  4.6, 'Biti''s',        'material',  'Denim',       FALSE, NULL, 800),
    (4,  'mai.tran@gmail.com',            'phu-kien-dien-tu',     'Máy ảnh Mirrorless X-100 kèm lens kit 15-45mm',       'camera',     12990000, 214,   4.9, 'Sony',           'warranty',  '24 tháng',    FALSE, NULL, 900),
    (5,  'owner.boutique@genie.example',  'thoi-trang-nu',        'Kính mát thời trang UV400 chống tia cực tím',         'sunglasses', 99000,    8900,  4.5, 'No Brand',       'material',  'Da',          TRUE,  NULL, 150),
    (6,  'owner.travel@genie.example',    'tui-xach-balo',        'Balo laptop chống nước 25L ngăn chống sốc',           'backpack',   189000,   3200,  4.7, 'Xiaomi',         'material',  'Vải',         FALSE, NULL, 900),
    (7,  'mai.tran@gmail.com',            'may-tinh-laptop',      'Laptop UltraBook 14" Core i5, 16GB RAM, SSD 512GB',   'laptop',     15490000, 432,   4.8, 'Asus',           'warranty',  '12 tháng',    TRUE,  NULL, 1800),
    (8,  'mai.tran@gmail.com',            'dien-thoai-phu-kien',  'Điện thoại Galaxy A-series 8GB/256GB chính hãng',     'phone',      5290000,  1700,  4.7, 'Samsung',        'warranty',  '24 tháng',    FALSE, NULL, 400),
    (9,  'owner.home@genie.example',      'nha-bep',              'Cốc sứ cao cấp phong cách tối giản 350ml',            'mug',        59000,    6100,  4.6, 'No Brand',       'material',  'Nhựa',        TRUE,  NULL, 400),
    (10, 'owner.home@genie.example',      'thiet-bi-gia-dung',    'Đèn bàn LED cảm ứng 3 chế độ sáng, sạc USB',          'lamp',       149000,   4500,  4.8, 'Xiaomi',         'material',  'Sứ',          TRUE,  NULL, 700),
    (11, 'owner.home@genie.example',      'noi-that',             'Ghế sofa đơn bọc nỉ chân gỗ phong cách Bắc Âu',       'chair',      1290000,  198,   4.7, 'No Brand',       'material',  'Gỗ',          FALSE, 14,   12000),
    (12, 'owner.beauty@genie.example',    'my-pham-cham-soc',     'Bộ mỹ phẩm trang điểm 12 món đầy đủ cho người mới',   'cosmetics',  235000,   12000, 4.9, 'Maybelline',     'skin_type', 'Da khô',      TRUE,  NULL, 600),
    (13, 'owner.beauty@genie.example',    'my-pham-cham-soc',     'Serum dưỡng da Vitamin C 30ml làm sáng da',           'skincare',   178000,   9300,  4.8, 'La Roche-Posay', 'skin_type', 'Mọi loại da', TRUE,  NULL, 120),
    (14, 'owner.shoe@genie.example',      'giay-dep',             'Giày cao gót da mũi nhọn 7cm thanh lịch',             'heels',      320000,   1200,  4.6, 'No Brand',       'material',  'Da',          FALSE, NULL, 700),
    (15, 'owner.boutique@genie.example',  'thoi-trang-nam',       'Áo khoác denim unisex form rộng phong cách Hàn',      'jacket',     245000,   3800,  4.7, 'No Brand',       'material',  'Denim',       TRUE,  NULL, 650),
    (16, 'owner.boutique@genie.example',  'thoi-trang-nam',       'Áo thun cotton basic 100% co giãn nhiều màu',         'tshirt',     89000,    15000, 4.5, 'No Brand',       'material',  'Cotton',      FALSE, NULL, 250),
    (17, 'mai.tran@gmail.com',            'thiet-bi-am-thanh',    'Loa bluetooth mini chống nước IPX7 bass mạnh',        'speaker',    320000,   2600,  4.7, 'JBL',            'warranty',  '12 tháng',    TRUE,  NULL, 540),
    (18, 'mai.tran@gmail.com',            'may-tinh-laptop',      'Bàn phím cơ RGB switch blue gaming chống ồn',         'keyboard',   459000,   1900,  4.8, 'Logitech',       'warranty',  '24 tháng',    FALSE, NULL, 1100),
    (19, 'owner.beauty@genie.example',    'my-pham-cham-soc',     'Nước hoa nữ Eau de Parfum hương hoa cỏ 50ml',         'perfume',    420000,   4100,  4.9, 'Chanel',         'skin_type', 'Mọi loại da', TRUE,  NULL, 300),
    (20, 'owner.boutique@genie.example',  'dong-ho-trang-suc',    'Đồng hồ cổ điển dây da nâu mặt tròn tối giản',        'watch2',     199000,   2300,  4.6, 'No Brand',       'material',  'Vải',         FALSE, NULL, 180)
) AS v(code, owner_email, category_slug, name, image, price, sold, rating, brand, attr_code, attr_value, free_ship,
       pre_order_days, weight_gram);

-- Biến thể: 1–3 SKU / sản phẩm; SKU 'SEED-P<mã>-<biến thể>' là khóa tự nhiên (UNIQUE). Một SKU tồn 0 (áo thun XL) để test hết hàng.
DROP TABLE IF EXISTS seed_variant;
CREATE TEMP TABLE seed_variant AS
SELECT * FROM (VALUES
    (1,  'BLK',  'Đen',             '{"color": "Đen"}',                 389000,   120),
    (1,  'WHT',  'Trắng',           '{"color": "Trắng"}',               389000,   80),
    (2,  'BLK',  'Đen',             '{"color": "Đen"}',                 690000,   60),
    (2,  'SLV',  'Bạc',             '{"color": "Bạc"}',                 690000,   40),
    (3,  '40',   'Size 40',         '{"size": "40"}',                   259000,   50),
    (3,  '41',   'Size 41',         '{"size": "41"}',                   259000,   50),
    (3,  '42',   'Size 42',         '{"size": "42"}',                   259000,   30),
    (4,  'STD',  'Kèm lens kit',    NULL,                               12990000, 8),
    (5,  'STD',  'Mặc định',        NULL,                               99000,    300),
    (6,  'STD',  'Mặc định',        NULL,                               189000,   150),
    (7,  '512',  '16GB / 512GB',    '{"storage": "512GB"}',             15490000, 12),
    (7,  '1TB',  '16GB / 1TB',      '{"storage": "1TB"}',               17490000, 5),
    (8,  'BLK',  'Đen',             '{"color": "Đen"}',                 5290000,  25),
    (8,  'BLU',  'Xanh',            '{"color": "Xanh"}',                5290000,  20),
    (9,  'STD',  'Mặc định',        NULL,                               59000,    500),
    (10, 'STD',  'Mặc định',        NULL,                               149000,   200),
    (11, 'STD',  'Mặc định',        NULL,                               1290000,  10),
    (12, 'STD',  'Mặc định',        NULL,                               235000,   180),
    (13, 'STD',  'Mặc định',        NULL,                               178000,   220),
    (14, '37',   'Size 37',         '{"size": "37"}',                   320000,   25),
    (14, '38',   'Size 38',         '{"size": "38"}',                   320000,   25),
    (15, 'M',    'Size M',          '{"size": "M"}',                    245000,   60),
    (15, 'L',    'Size L',          '{"size": "L"}',                    245000,   60),
    (16, 'M',    'Size M',          '{"size": "M"}',                    89000,    400),
    (16, 'L',    'Size L',          '{"size": "L"}',                    89000,    350),
    (16, 'XL',   'Size XL',         '{"size": "XL"}',                   89000,    0),
    (17, 'STD',  'Mặc định',        NULL,                               320000,   90),
    (18, 'STD',  'Mặc định',        NULL,                               459000,   70),
    (19, 'STD',  'Mặc định',        NULL,                               420000,   45),
    (20, 'STD',  'Mặc định',        NULL,                               199000,   140)
) AS v(product_code, variant_code, variant_name, attributes, price, stock);

-- ===== S04.02 products =====
INSERT INTO products (vendor_id, category_id, name, description, brand, brand_id, item_condition, origin, specs_json,
                      weight, ships_from, shipping_payer, base_price, status, is_published, version,
                      created_at, updated_at, sold, rating, is_pre_order, pre_order_days, view_count)
SELECT u.id, c.id, p.name,
       p.name || '. Hàng mẫu của Genie để thử giao diện và luồng mua hàng.',
       p.brand, b.id, 'NEW', 'Việt Nam', jsonb_build_object(p.attr_code, p.attr_value),
       p.weight_gram, vp.pickup_province, CASE WHEN p.free_ship THEN 'SELLER' ELSE 'BUYER' END,
       p.price, 'PUBLISHED', TRUE, 0,
       now() - make_interval(days => 60 - p.code), now(), p.sold, p.rating,
       p.pre_order_days IS NOT NULL, p.pre_order_days, p.sold * 7
FROM seed_product p
JOIN users u ON u.email = p.owner_email
JOIN vendor_profiles vp ON vp.user_id = u.id
LEFT JOIN categories c ON c.slug = p.category_slug
LEFT JOIN brands b ON b.name = p.brand
WHERE NOT EXISTS (SELECT 1 FROM products x WHERE x.vendor_id = u.id AND x.name = p.name);

-- ===== S04.03 product_variants =====
INSERT INTO product_variants (product_id, sku, variant_name, attributes_json, price, stock_count, reserved_count, version)
SELECT pr.id, 'SEED-P' || v.product_code || '-' || v.variant_code, v.variant_name, v.attributes::jsonb, v.price, v.stock, 0, 0
FROM seed_variant v
JOIN seed_product p ON p.code = v.product_code
JOIN users u ON u.email = p.owner_email
JOIN products pr ON pr.vendor_id = u.id AND pr.name = p.name
ON CONFLICT (sku) DO NOTHING;

-- ===== S04.04 product_images — ảnh chính (khóa đối tượng trong bucket product-media) =====
INSERT INTO product_images (product_id, image_url, width, height, alt_text, is_primary, mime_type, display_order)
SELECT pr.id, 'seed/product/' || p.image || '.jpg', 800, 800, p.name, TRUE, 'image/jpeg', 0
FROM seed_product p
JOIN users u ON u.email = p.owner_email
JOIN products pr ON pr.vendor_id = u.id AND pr.name = p.name
WHERE NOT EXISTS (SELECT 1 FROM product_images i WHERE i.product_id = pr.id AND i.image_url = 'seed/product/' || p.image || '.jpg');

-- ===== S04.06 đồng bộ bộ đếm sản phẩm đang bán của shop =====
UPDATE vendor_profiles vp
SET active_product_count = (SELECT count(*) FROM products p WHERE p.vendor_id = vp.user_id AND p.status = 'PUBLISHED')
WHERE vp.kyc_status = 'VERIFIED';

DROP TABLE seed_variant;
DROP TABLE seed_product;
