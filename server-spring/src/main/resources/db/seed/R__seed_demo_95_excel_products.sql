-- Dữ liệu mẫu 95 — sản phẩm nhập từ file Excel (plan 2026-11-21 db/plan_seed_tu_excel.md).
-- FILE SINH TỰ ĐỘNG bởi scripts/seed/excel/excel_to_seed.py — KHÔNG SỬA TAY: sửa file Excel rồi chạy
--   python scripts/seed/excel/excel_to_seed.py build <file.xlsx>      (hướng dẫn: scripts/seed/excel/README.md)
-- SQL thuần, chạy lại được, không ghi cứng id: sản phẩm khớp theo (email chủ shop, tên); SKU 'XL-<ma_sp>-<ma_bien_the>'.
-- Ảnh lưu khóa đối tượng 'seed/excel/<băm8>.jpg' trong bucket product-media — ảnh ở scripts/seed/excel/assets/,
-- đẩy lên Silo bằng scripts/seed/upload-assets.sh | .ps1. Ô ảnh là link https thì lưu nguyên link.
-- Nguồn: products.xlsx · 54 sản phẩm, 89 biến thể, 54 ảnh.

DROP TABLE IF EXISTS xl_product, xl_variant, xl_image, xl_product_id;

CREATE TEMP TABLE xl_product (
    code TEXT PRIMARY KEY, owner_email TEXT NOT NULL, category_slug TEXT NOT NULL, name TEXT NOT NULL, description TEXT,
    brand TEXT, item_condition TEXT NOT NULL, origin TEXT, specs JSONB, weight_gram DOUBLE PRECISION NOT NULL,
    length_cm DOUBLE PRECISION, width_cm DOUBLE PRECISION, height_cm DOUBLE PRECISION, free_ship BOOLEAN NOT NULL,
    pre_order_days INT, status TEXT NOT NULL, sold INT NOT NULL, rating DOUBLE PRECISION, base_price NUMERIC(19,2) NOT NULL
);
INSERT INTO xl_product VALUES
    ('XL01', 'mai.tran@gmail.com', 'thiet-bi-am-thanh', 'Tai nghe chống ồn Bose QuietComfort Ultra Headphones', 'Tai nghe chụp tai chống ồn chủ động, âm thanh không gian Immersive Audio.
Pin 24 giờ, sạc USB-C. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Bose', 'NEW', 'Trung Quốc', '{"warranty": "12 tháng"}'::jsonb, 250.0, 22.0, 18.0, 9.0, TRUE, NULL, 'PUBLISHED', 152, 4.8, 9990000),
    ('XL02', 'owner.boutique@genie.example', 'thoi-trang-nam', 'Áo thun Uniqlo U Crew Neck ngắn tay', 'Áo thun cotton dày dặn, form rộng vừa. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Uniqlo', 'NEW', 'Việt Nam', '{"material": "Cotton"}'::jsonb, 250.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 420, 4.6, 391000),
    ('XL03', 'owner.home@genie.example', 'thiet-bi-gia-dung', 'Đèn ngủ thông minh Xiaomi Mi Bedside Lamp 2', 'Đèn ngủ 16 triệu màu, điều khiển bằng ứng dụng và giọng nói. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Xiaomi', 'NEW', 'Trung Quốc', '{"material": "Nhựa"}'::jsonb, 700.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 37, 4.9, 690000),
    ('XL04', 'owner.travel@genie.example', 'tui-xach-balo', 'Balo du lịch Osprey Farpoint 40', 'Balo du lịch 40L đạt chuẩn hành lý xách tay, ngăn laptop riêng. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Osprey', 'NEW', 'Việt Nam', '{"material": "Vải"}'::jsonb, 1600.0, 50.0, 32.0, 20.0, FALSE, 7, 'PUBLISHED', 0, NULL, 4290000),
    ('XL05', 'mai.tran@gmail.com', 'dien-thoai-phu-kien', 'Apple iPhone 15 Pro Max 256GB', 'Apple iPhone 15 Pro Max 256GB chính hãng Apple. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Apple', 'NEW', 'Trung Quốc', '{"warranty": "12 tháng"}'::jsonb, 221.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 640, 4.9, 29990000),
    ('XL06', 'mai.tran@gmail.com', 'dien-thoai-phu-kien', 'Samsung Galaxy S24 Ultra 12GB/256GB', 'Samsung Galaxy S24 Ultra 12GB/256GB chính hãng Samsung. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Samsung', 'NEW', 'Việt Nam', '{"warranty": "12 tháng"}'::jsonb, 232.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 520, 4.8, 26990000),
    ('XL07', 'mai.tran@gmail.com', 'phu-kien-dien-tu', 'Củ sạc Anker 735 Nano II 65W GaN 3 cổng', 'Củ sạc Anker 735 Nano II 65W GaN 3 cổng chính hãng Anker. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Anker', 'NEW', 'Trung Quốc', '{"warranty": "18 tháng"}'::jsonb, 130.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 1850, 4.8, 990000),
    ('XL08', 'mai.tran@gmail.com', 'thiet-bi-am-thanh', 'Loa Bluetooth JBL Flip 6 chống nước IP67', 'Loa Bluetooth JBL Flip 6 chống nước IP67 chính hãng JBL. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'JBL', 'NEW', 'Trung Quốc', '{"warranty": "12 tháng"}'::jsonb, 550.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 1300, 4.8, 2690000),
    ('XL09', 'mai.tran@gmail.com', 'may-tinh-laptop', 'Laptop ASUS ROG Strix G16 i7-13650HX RTX 4060 16GB 512GB', 'Laptop ASUS ROG Strix G16 i7-13650HX RTX 4060 16GB 512GB chính hãng Asus. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Asus', 'NEW', 'Trung Quốc', '{"warranty": "24 tháng"}'::jsonb, 2500.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 96, 4.8, 34990000),
    ('XL10', 'mai.tran@gmail.com', 'phu-kien-dien-tu', 'Chuột không dây Logitech MX Master 3S', 'Chuột không dây Logitech MX Master 3S chính hãng Logitech. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Logitech', 'NEW', 'Trung Quốc', '{"warranty": "12 tháng"}'::jsonb, 141.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 2600, 4.9, 2490000),
    ('XL11', 'mai.tran@gmail.com', 'may-tinh-laptop', 'Bàn phím cơ không dây Keychron K2 Pro hot-swap', 'Bàn phím cơ không dây Keychron K2 Pro hot-swap chính hãng Keychron. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Keychron', 'NEW', 'Trung Quốc', '{"warranty": "12 tháng"}'::jsonb, 900.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 410, 4.8, 2290000),
    ('XL12', 'mai.tran@gmail.com', 'may-tinh-laptop', 'Apple MacBook Air 13 inch M3 8GB/256GB', 'Apple MacBook Air 13 inch M3 8GB/256GB chính hãng Apple. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Apple', 'NEW', 'Trung Quốc', '{"warranty": "12 tháng"}'::jsonb, 1240.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 380, 4.9, 27990000),
    ('XL13', 'mai.tran@gmail.com', 'thiet-bi-am-thanh', 'Tai nghe Apple AirPods Pro 2 cổng USB-C', 'Tai nghe Apple AirPods Pro 2 cổng USB-C chính hãng Apple. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Apple', 'NEW', 'Việt Nam', '{"warranty": "12 tháng"}'::jsonb, 60.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 2300, 4.9, 5990000),
    ('XL14', 'mai.tran@gmail.com', 'thiet-bi-am-thanh', 'Tai nghe chống ồn Sony WH-1000XM5', 'Tai nghe chống ồn Sony WH-1000XM5 chính hãng Sony. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Sony', 'NEW', 'Malaysia', '{"warranty": "12 tháng"}'::jsonb, 250.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 870, 4.9, 7990000),
    ('XL15', 'mai.tran@gmail.com', 'phu-kien-dien-tu', 'Máy ảnh Sony ZV-1 II quay vlog', 'Máy ảnh Sony ZV-1 II quay vlog (hàng trưng bày) chính hãng Sony. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Sony', 'REFURBISHED', 'Thái Lan', '{"warranty": "6 tháng"}'::jsonb, 290.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 41, 4.7, 16990000),
    ('XL16', 'mai.tran@gmail.com', 'phu-kien-dien-tu', 'Đồng hồ Apple Watch Series 9 GPS 41mm', 'Đồng hồ Apple Watch Series 9 GPS 41mm chính hãng Apple. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Apple', 'NEW', 'Việt Nam', '{"warranty": "12 tháng"}'::jsonb, 32.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 760, 4.8, 9490000),
    ('XL17', 'owner.shoe@genie.example', 'giay-dep', 'Giày chạy bộ Nike Pegasus 41', 'Giày chạy bộ Nike Pegasus 41 chính hãng Nike. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Nike', 'NEW', 'Việt Nam', '{"material": "Vải"}'::jsonb, 290.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 2300, 4.8, 3519000),
    ('XL18', 'owner.shoe@genie.example', 'giay-dep', 'Giày chạy bộ Adidas Ultraboost Light', 'Giày chạy bộ Adidas Ultraboost Light chính hãng Adidas. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Adidas', 'NEW', 'Việt Nam', '{"material": "Vải"}'::jsonb, 300.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 1450, 4.8, 4500000),
    ('XL19', 'owner.shoe@genie.example', 'giay-dep', 'Giày Converse Chuck Taylor All Star Classic cổ thấp', 'Giày Converse Chuck Taylor All Star Classic cổ thấp chính hãng Converse. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Converse', 'NEW', 'Việt Nam', '{"material": "Vải"}'::jsonb, 700.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 5200, 4.7, 1500000),
    ('XL20', 'owner.shoe@genie.example', 'giay-dep', 'Dép Crocs Classic Clog', 'Dép Crocs Classic Clog chính hãng Crocs. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Crocs', 'NEW', 'Việt Nam', '{"material": "Nhựa"}'::jsonb, 400.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 3700, 4.7, 1290000),
    ('XL21', 'owner.shoe@genie.example', 'giay-dep', 'Giày Vans Old Skool đen trắng', 'Giày Vans Old Skool đen trắng chính hãng Vans. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Vans', 'NEW', 'Việt Nam', '{"material": "Da"}'::jsonb, 900.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 2900, 4.8, 1750000),
    ('XL22', 'owner.shoe@genie.example', 'giay-dep', 'Giày thể thao Biti''s Hunter Street', 'Giày thể thao Biti''s Hunter Street chính hãng Biti''s. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Biti''s', 'NEW', 'Việt Nam', '{"material": "Vải"}'::jsonb, 650.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 4100, 4.7, 899000),
    ('XL23', 'owner.shoe@genie.example', 'giay-dep', 'Giày boot Dr. Martens 1460 Smooth 8 lỗ', 'Giày boot Dr. Martens 1460 Smooth 8 lỗ chính hãng Dr. Martens. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Dr. Martens', 'NEW', 'Việt Nam', '{"material": "Da"}'::jsonb, 1300.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 120, 4.8, 5290000),
    ('XL24', 'owner.boutique@genie.example', 'thoi-trang-nam', 'Áo sơ mi Oxford dài tay nam Uniqlo', 'Áo sơ mi Oxford dài tay nam Uniqlo chính hãng Uniqlo. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Uniqlo', 'NEW', 'Việt Nam', '{"material": "Cotton"}'::jsonb, 280.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 1300, 4.7, 784000),
    ('XL25', 'owner.boutique@genie.example', 'thoi-trang-nam', 'Quần jeans nam Levi''s 511 Slim', 'Quần jeans nam Levi''s 511 Slim chính hãng Levi''s. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Levi''s', 'NEW', 'Bangladesh', '{"material": "Denim"}'::jsonb, 650.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 2100, 4.7, 1699000),
    ('XL26', 'owner.boutique@genie.example', 'thoi-trang-nam', 'Áo hoodie Champion Reverse Weave', 'Áo hoodie Champion Reverse Weave chính hãng Champion. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Champion', 'NEW', 'Việt Nam', '{"material": "Cotton"}'::jsonb, 700.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 640, 4.7, 1890000),
    ('XL27', 'owner.boutique@genie.example', 'thoi-trang-nam', 'Áo polo Lacoste L.12.12 Classic Fit', 'Áo polo Lacoste L.12.12 Classic Fit chính hãng Lacoste. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Lacoste', 'NEW', 'Peru', '{"material": "Cotton"}'::jsonb, 250.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 410, 4.8, 2990000),
    ('XL28', 'owner.boutique@genie.example', 'thoi-trang-nu', 'Đầm midi satin Zara', 'Đầm midi satin Zara chính hãng Zara. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Zara', 'NEW', 'Thổ Nhĩ Kỳ', NULL, 300.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 780, 4.6, 1199000),
    ('XL29', 'owner.boutique@genie.example', 'thoi-trang-nu', 'Chân váy xếp ly H&M', 'Chân váy xếp ly H&M chính hãng H&M. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'H&M', 'NEW', 'Bangladesh', NULL, 250.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 1650, 4.5, 599000),
    ('XL30', 'owner.boutique@genie.example', 'thoi-trang-nu', 'Áo cardigan len Merino nữ Uniqlo', 'Áo cardigan len Merino nữ Uniqlo chính hãng Uniqlo. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Uniqlo', 'NEW', 'Trung Quốc', NULL, 300.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 990, 4.7, 784000),
    ('XL31', 'owner.boutique@genie.example', 'thoi-trang-nu', 'Kính mát Ray-Ban Aviator Classic RB3025', 'Kính mát Ray-Ban Aviator Classic RB3025 chính hãng Ray-Ban. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Ray-Ban', 'NEW', 'Ý', NULL, 120.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 520, 4.8, 4590000),
    ('XL32', 'owner.boutique@genie.example', 'dong-ho-trang-suc', 'Đồng hồ Casio G-Shock GA-2100-1A1', 'Đồng hồ Casio G-Shock GA-2100-1A1 chính hãng Casio. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Casio', 'NEW', 'Thái Lan', '{"material": "Nhựa"}'::jsonb, 150.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 910, 4.8, 3369000),
    ('XL33', 'owner.boutique@genie.example', 'dong-ho-trang-suc', 'Vòng tay Pandora Moments mắt xích rắn bạc 925', 'Vòng tay Pandora Moments mắt xích rắn bạc 925 chính hãng Pandora. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Pandora', 'NEW', 'Thái Lan', '{"material": "Bạc 925"}'::jsonb, 50.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 340, 4.8, 2190000),
    ('XL34', 'owner.home@genie.example', 'thiet-bi-gia-dung', 'Nồi chiên không dầu Philips HD9252/90 4.1L', 'Nồi chiên không dầu Philips HD9252/90 4.1L chính hãng Philips.', 'Philips', 'NEW', 'Trung Quốc', NULL, 5000.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 2900, 4.8, 2290000),
    ('XL35', 'owner.home@genie.example', 'thiet-bi-gia-dung', 'Máy lọc không khí Xiaomi Smart Air Purifier 4', 'Máy lọc không khí Xiaomi Smart Air Purifier 4 chính hãng Xiaomi. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Xiaomi', 'NEW', 'Trung Quốc', NULL, 6800.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 670, 4.7, 3490000),
    ('XL36', 'owner.home@genie.example', 'thiet-bi-gia-dung', 'Robot hút bụi lau nhà Roborock Q7 Max', 'Robot hút bụi lau nhà Roborock Q7 Max chính hãng Roborock. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Roborock', 'NEW', 'Trung Quốc', NULL, 4700.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 340, 4.8, 7990000),
    ('XL37', 'owner.home@genie.example', 'nha-bep', 'Bộ nồi inox Elmich Tri-Max 3 chiếc', 'Bộ nồi inox Elmich Tri-Max 3 chiếc chính hãng Elmich. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Elmich', 'NEW', 'Việt Nam', '{"material": "Inox"}'::jsonb, 4000.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 1100, 4.6, 1290000),
    ('XL38', 'owner.home@genie.example', 'nha-bep', 'Bình giữ nhiệt Lock&Lock Energetic 500ml', 'Bình giữ nhiệt Lock&Lock Energetic 500ml chính hãng Lock&Lock. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Lock&Lock', 'NEW', 'Trung Quốc', NULL, 350.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 6300, 4.7, 359000),
    ('XL39', 'owner.home@genie.example', 'nha-bep', 'Dao đầu bếp Zwilling Four Star 20cm', 'Dao đầu bếp Zwilling Four Star 20cm chính hãng Zwilling. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Zwilling', 'NEW', 'Đức', NULL, 300.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 270, 4.9, 2990000),
    ('XL40', 'owner.home@genie.example', 'noi-that', 'Kệ sách IKEA BILLY 80x28x202 cm', 'Kệ sách IKEA BILLY 80x28x202 cm chính hãng IKEA. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'IKEA', 'NEW', 'Ba Lan', '{"material": "Gỗ"}'::jsonb, 30000.0, NULL, NULL, NULL, FALSE, 10, 'PUBLISHED', 260, 4.6, 1990000),
    ('XL41', 'owner.home@genie.example', 'noi-that', 'Bàn làm việc IKEA LAGKAPTEN / ADILS 120x60 cm', 'Bàn làm việc IKEA LAGKAPTEN / ADILS 120x60 cm chính hãng IKEA. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'IKEA', 'NEW', 'Ba Lan', '{"material": "Gỗ"}'::jsonb, 16000.0, NULL, NULL, NULL, FALSE, 14, 'PUBLISHED', 190, 4.6, 1490000),
    ('XL42', 'owner.home@genie.example', 'noi-that', 'Ghế công thái học Sihoo M57', 'Ghế công thái học Sihoo M57 chính hãng Sihoo. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Sihoo', 'NEW', 'Trung Quốc', '{"material": "Nhựa"}'::jsonb, 15000.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 520, 4.7, 2990000),
    ('XL43', 'owner.beauty@genie.example', 'my-pham-cham-soc', 'Kem chống nắng La Roche-Posay Anthelios UVMune 400 SPF50+ 50ml', 'Kem chống nắng La Roche-Posay Anthelios UVMune 400 SPF50+ 50ml chính hãng La Roche-Posay. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'La Roche-Posay', 'NEW', 'Pháp', '{"skin_type": "Mọi loại da"}'::jsonb, 80.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 8800, 4.9, 545000),
    ('XL44', 'owner.beauty@genie.example', 'my-pham-cham-soc', 'Sữa rửa mặt CeraVe Foaming Cleanser 236ml', 'Sữa rửa mặt CeraVe Foaming Cleanser 236ml chính hãng CeraVe. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'CeraVe', 'NEW', 'Pháp', '{"skin_type": "Da dầu"}'::jsonb, 280.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 5600, 4.8, 365000),
    ('XL45', 'owner.beauty@genie.example', 'my-pham-cham-soc', 'Son kem Maybelline SuperStay Matte Ink', 'Son kem Maybelline SuperStay Matte Ink chính hãng Maybelline. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Maybelline', 'NEW', 'Mỹ', '{"skin_type": "Mọi loại da"}'::jsonb, 30.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 12400, 4.7, 239000),
    ('XL46', 'owner.beauty@genie.example', 'my-pham-cham-soc', 'Phấn nước Laneige Neo Cushion Matte SPF46', 'Phấn nước Laneige Neo Cushion Matte SPF46 chính hãng Laneige. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Laneige', 'NEW', 'Hàn Quốc', '{"skin_type": "Da dầu"}'::jsonb, 60.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 4300, 4.7, 890000),
    ('XL47', 'owner.beauty@genie.example', 'my-pham-cham-soc', 'Mascara Maybelline Lash Sensational Sky High', 'Mascara Maybelline Lash Sensational Sky High chính hãng Maybelline. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Maybelline', 'NEW', 'Pháp', NULL, 30.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 3900, 4.7, 259000),
    ('XL48', 'owner.beauty@genie.example', 'my-pham-cham-soc', 'Nước hoa nam Bleu de Chanel Eau de Parfum 100ml', 'Nước hoa nam Bleu de Chanel Eau de Parfum 100ml chính hãng Chanel. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Chanel', 'NEW', 'Pháp', NULL, 400.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 410, 4.9, 4590000),
    ('XL49', 'owner.beauty@genie.example', 'my-pham-cham-soc', 'Mặt nạ giấy Innisfree My Real Squeeze hộp 10 miếng', 'Mặt nạ giấy Innisfree My Real Squeeze hộp 10 miếng chính hãng Innisfree. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Innisfree', 'NEW', 'Hàn Quốc', '{"skin_type": "Da khô"}'::jsonb, 250.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 7700, 4.6, 250000),
    ('XL50', 'owner.travel@genie.example', 'tui-xach-balo', 'Vali American Tourister Curio 2 size 69cm', 'Vali American Tourister Curio 2 size 69cm chính hãng American Tourister. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'American Tourister', 'NEW', 'Ấn Độ', '{"material": "Nhựa"}'::jsonb, 4200.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 560, 4.7, 3490000),
    ('XL51', 'owner.travel@genie.example', 'tui-xach-balo', 'Túi đeo chéo Xiaomi Mi City Sling Bag', 'Túi đeo chéo Xiaomi Mi City Sling Bag chính hãng Xiaomi. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Xiaomi', 'NEW', 'Trung Quốc', '{"material": "Vải"}'::jsonb, 300.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 2200, 4.6, 299000),
    ('XL52', 'owner.travel@genie.example', 'tui-xach-balo', 'Balo The North Face Borealis 28L', 'Balo The North Face Borealis 28L chính hãng The North Face. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'The North Face', 'NEW', 'Việt Nam', '{"material": "Vải"}'::jsonb, 1100.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 1300, 4.8, 2590000),
    ('XL53', 'owner.travel@genie.example', 'tui-xach-balo', 'Túi du lịch Herschel Novel Duffle 42.5L', 'Túi du lịch Herschel Novel Duffle 42.5L chính hãng Herschel.', 'Herschel', 'NEW', 'Trung Quốc', '{"material": "Vải"}'::jsonb, 900.0, NULL, NULL, NULL, TRUE, NULL, 'PUBLISHED', 690, 4.7, 2190000),
    ('XL54', 'owner.travel@genie.example', 'tui-xach-balo', 'Gối kê cổ Cabeau Evolution S3', 'Gối kê cổ Cabeau Evolution S3 chính hãng Cabeau. Sản phẩm mẫu để thử tìm kiếm, lọc và giỏ hàng.', 'Cabeau', 'NEW', 'Trung Quốc', NULL, 300.0, NULL, NULL, NULL, FALSE, NULL, 'PUBLISHED', 1100, 4.7, 1290000);

CREATE TEMP TABLE xl_variant (
    product_code TEXT NOT NULL, sku TEXT PRIMARY KEY, variant_name TEXT, attributes JSONB, price NUMERIC(19,2) NOT NULL,
    stock INT NOT NULL, image_url TEXT
);
INSERT INTO xl_variant VALUES
    ('XL01', 'XL-XL01-BLK', 'Đen', '{"color": "Đen"}'::jsonb, 9990000, 40, NULL),
    ('XL01', 'XL-XL01-WHT', 'Trắng Khói', '{"color": "Trắng Khói"}'::jsonb, 9990000, 25, NULL),
    ('XL02', 'XL-XL02-M', 'Size M', '{"size": "M"}'::jsonb, 391000, 80, NULL),
    ('XL02', 'XL-XL02-L', 'Size L', '{"size": "L"}'::jsonb, 391000, 60, NULL),
    ('XL02', 'XL-XL02-XL', 'Size XL', '{"size": "XL"}'::jsonb, 391000, 0, NULL),
    ('XL03', 'XL-XL03-STD', 'Mặc định', NULL, 690000, 25, NULL),
    ('XL04', 'XL-XL04-STD', 'Mặc định', NULL, 4290000, 15, NULL),
    ('XL05', 'XL-XL05-NAT', 'Titan Tự nhiên', '{"color": "Titan Tự nhiên"}'::jsonb, 29990000, 30, NULL),
    ('XL05', 'XL-XL05-BLK', 'Titan Đen', '{"color": "Titan Đen"}'::jsonb, 29990000, 25, NULL),
    ('XL05', 'XL-XL05-WHT', 'Titan Trắng', '{"color": "Titan Trắng"}'::jsonb, 29990000, 18, NULL),
    ('XL06', 'XL-XL06-GRY', 'Xám Titan', '{"color": "Xám Titan"}'::jsonb, 26990000, 28, NULL),
    ('XL06', 'XL-XL06-BLK', 'Đen Titan', '{"color": "Đen Titan"}'::jsonb, 26990000, 24, NULL),
    ('XL06', 'XL-XL06-VIO', 'Tím Titan', '{"color": "Tím Titan"}'::jsonb, 26990000, 15, NULL),
    ('XL07', 'XL-XL07-STD', 'Mặc định', NULL, 990000, 120, NULL),
    ('XL08', 'XL-XL08-BLK', 'Đen', '{"color": "Đen"}'::jsonb, 2690000, 60, NULL),
    ('XL08', 'XL-XL08-BLU', 'Xanh', '{"color": "Xanh"}'::jsonb, 2690000, 45, NULL),
    ('XL08', 'XL-XL08-RED', 'Đỏ', '{"color": "Đỏ"}'::jsonb, 2690000, 35, NULL),
    ('XL09', 'XL-XL09-STD', 'Mặc định', NULL, 34990000, 8, NULL),
    ('XL10', 'XL-XL10-GPH', 'Graphite', '{"color": "Graphite"}'::jsonb, 2490000, 80, NULL),
    ('XL10', 'XL-XL10-GRY', 'Pale Gray', '{"color": "Pale Gray"}'::jsonb, 2490000, 50, NULL),
    ('XL11', 'XL-XL11-RED', 'Gateron Red', '{"switch": "Red"}'::jsonb, 2290000, 40, NULL),
    ('XL11', 'XL-XL11-BRN', 'Gateron Brown', '{"switch": "Brown"}'::jsonb, 2290000, 25, NULL),
    ('XL12', 'XL-XL12-MID', 'Midnight', '{"color": "Midnight"}'::jsonb, 27990000, 15, NULL),
    ('XL12', 'XL-XL12-STL', 'Starlight', '{"color": "Starlight"}'::jsonb, 27990000, 12, NULL),
    ('XL12', 'XL-XL12-SLV', 'Bạc', '{"color": "Bạc"}'::jsonb, 27990000, 10, NULL),
    ('XL13', 'XL-XL13-STD', 'Mặc định', NULL, 5990000, 70, NULL),
    ('XL14', 'XL-XL14-BLK', 'Đen', '{"color": "Đen"}'::jsonb, 7990000, 40, NULL),
    ('XL14', 'XL-XL14-SLV', 'Bạc', '{"color": "Bạc"}'::jsonb, 7990000, 30, NULL),
    ('XL15', 'XL-XL15-STD', 'Mặc định', NULL, 16990000, 3, NULL),
    ('XL16', 'XL-XL16-MID', 'Midnight', '{"color": "Midnight"}'::jsonb, 9490000, 35, NULL),
    ('XL16', 'XL-XL16-STL', 'Starlight', '{"color": "Starlight"}'::jsonb, 9490000, 30, NULL),
    ('XL16', 'XL-XL16-PNK', 'Hồng', '{"color": "Hồng"}'::jsonb, 9490000, 20, NULL),
    ('XL17', 'XL-XL17-40', 'Size 40', '{"size": "40"}'::jsonb, 3519000, 30, NULL),
    ('XL17', 'XL-XL17-41', 'Size 41', '{"size": "41"}'::jsonb, 3519000, 40, NULL),
    ('XL17', 'XL-XL17-42', 'Size 42', '{"size": "42"}'::jsonb, 3519000, 40, NULL),
    ('XL17', 'XL-XL17-43', 'Size 43', '{"size": "43"}'::jsonb, 3519000, 20, NULL),
    ('XL18', 'XL-XL18-40', 'Size 40', '{"size": "40"}'::jsonb, 4500000, 25, NULL),
    ('XL18', 'XL-XL18-41', 'Size 41', '{"size": "41"}'::jsonb, 4500000, 30, NULL),
    ('XL18', 'XL-XL18-42', 'Size 42', '{"size": "42"}'::jsonb, 4500000, 25, NULL),
    ('XL19', 'XL-XL19-38', 'Size 38', '{"size": "38"}'::jsonb, 1500000, 40, NULL),
    ('XL19', 'XL-XL19-39', 'Size 39', '{"size": "39"}'::jsonb, 1500000, 50, NULL),
    ('XL19', 'XL-XL19-40', 'Size 40', '{"size": "40"}'::jsonb, 1500000, 50, NULL),
    ('XL19', 'XL-XL19-41', 'Size 41', '{"size": "41"}'::jsonb, 1500000, 40, NULL),
    ('XL20', 'XL-XL20-STD', 'Mặc định', NULL, 1290000, 160, NULL),
    ('XL21', 'XL-XL21-38', 'Size 38', '{"size": "38"}'::jsonb, 1750000, 30, NULL),
    ('XL21', 'XL-XL21-39', 'Size 39', '{"size": "39"}'::jsonb, 1750000, 35, NULL),
    ('XL21', 'XL-XL21-40', 'Size 40', '{"size": "40"}'::jsonb, 1750000, 35, NULL),
    ('XL21', 'XL-XL21-41', 'Size 41', '{"size": "41"}'::jsonb, 1750000, 25, NULL),
    ('XL22', 'XL-XL22-STD', 'Mặc định', NULL, 899000, 90, NULL),
    ('XL23', 'XL-XL23-STD', 'Mặc định', NULL, 5290000, 0, NULL),
    ('XL24', 'XL-XL24-M', 'Size M', '{"size": "M"}'::jsonb, 784000, 60, NULL),
    ('XL24', 'XL-XL24-L', 'Size L', '{"size": "L"}'::jsonb, 784000, 50, NULL),
    ('XL24', 'XL-XL24-XL', 'Size XL', '{"size": "XL"}'::jsonb, 784000, 30, NULL),
    ('XL25', 'XL-XL25-30', 'Size 30', '{"size": "30"}'::jsonb, 1699000, 40, NULL),
    ('XL25', 'XL-XL25-31', 'Size 31', '{"size": "31"}'::jsonb, 1699000, 45, NULL),
    ('XL25', 'XL-XL25-32', 'Size 32', '{"size": "32"}'::jsonb, 1699000, 35, NULL),
    ('XL26', 'XL-XL26-STD', 'Mặc định', NULL, 1890000, 140, NULL),
    ('XL27', 'XL-XL27-STD', 'Mặc định', NULL, 2990000, 60, NULL),
    ('XL28', 'XL-XL28-STD', 'Mặc định', NULL, 1199000, 60, NULL),
    ('XL29', 'XL-XL29-STD', 'Mặc định', NULL, 599000, 95, NULL),
    ('XL30', 'XL-XL30-STD', 'Mặc định', NULL, 784000, 70, NULL),
    ('XL31', 'XL-XL31-STD', 'Mặc định', NULL, 4590000, 30, NULL),
    ('XL32', 'XL-XL32-STD', 'Mặc định', NULL, 3369000, 40, NULL),
    ('XL33', 'XL-XL33-STD', 'Mặc định', NULL, 2190000, 75, NULL),
    ('XL34', 'XL-XL34-STD', 'Mặc định', NULL, 2290000, 35, NULL),
    ('XL35', 'XL-XL35-STD', 'Mặc định', NULL, 3490000, 22, NULL),
    ('XL36', 'XL-XL36-STD', 'Mặc định', NULL, 7990000, 12, NULL),
    ('XL37', 'XL-XL37-STD', 'Mặc định', NULL, 1290000, 50, NULL),
    ('XL38', 'XL-XL38-BLK', 'Đen', '{"color": "Đen"}'::jsonb, 359000, 120, NULL),
    ('XL38', 'XL-XL38-WHT', 'Trắng', '{"color": "Trắng"}'::jsonb, 359000, 100, NULL),
    ('XL39', 'XL-XL39-STD', 'Mặc định', NULL, 2990000, 30, NULL),
    ('XL40', 'XL-XL40-STD', 'Mặc định', NULL, 1990000, 18, NULL),
    ('XL41', 'XL-XL41-STD', 'Mặc định', NULL, 1490000, 10, NULL),
    ('XL42', 'XL-XL42-STD', 'Mặc định', NULL, 2990000, 15, NULL),
    ('XL43', 'XL-XL43-STD', 'Mặc định', NULL, 545000, 300, NULL),
    ('XL44', 'XL-XL44-STD', 'Mặc định', NULL, 365000, 260, NULL),
    ('XL45', 'XL-XL45-20', '20 Pioneer', '{"color": "20 Pioneer"}'::jsonb, 239000, 200, NULL),
    ('XL45', 'XL-XL45-80', '80 Ruler', '{"color": "80 Ruler"}'::jsonb, 239000, 180, NULL),
    ('XL45', 'XL-XL45-65', '65 Seductress', '{"color": "65 Seductress"}'::jsonb, 239000, 160, NULL),
    ('XL46', 'XL-XL46-STD', 'Mặc định', NULL, 890000, 180, NULL),
    ('XL47', 'XL-XL47-STD', 'Mặc định', NULL, 259000, 220, NULL),
    ('XL48', 'XL-XL48-STD', 'Mặc định', NULL, 4590000, 25, NULL),
    ('XL49', 'XL-XL49-STD', 'Mặc định', NULL, 250000, 400, NULL),
    ('XL50', 'XL-XL50-GRY', 'Xám', '{"color": "Xám"}'::jsonb, 3490000, 20, NULL),
    ('XL50', 'XL-XL50-NVY', 'Xanh navy', '{"color": "Xanh navy"}'::jsonb, 3490000, 15, NULL),
    ('XL51', 'XL-XL51-STD', 'Mặc định', NULL, 299000, 140, NULL),
    ('XL52', 'XL-XL52-STD', 'Mặc định', NULL, 2590000, 65, NULL),
    ('XL53', 'XL-XL53-STD', 'Mặc định', NULL, 2190000, 40, NULL),
    ('XL54', 'XL-XL54-STD', 'Mặc định', NULL, 1290000, 200, NULL);

CREATE TEMP TABLE xl_image (
    product_code TEXT NOT NULL, sku TEXT, image_url TEXT NOT NULL, width INT, height INT, file_size BIGINT,
    mime_type TEXT, is_primary BOOLEAN NOT NULL, display_order INT NOT NULL
);
INSERT INTO xl_image VALUES
    ('XL01', NULL, 'seed/excel/002eb390.jpg', 447, 447, 14654, 'image/jpeg', TRUE, 0),
    ('XL02', NULL, 'seed/excel/65ec1ce9.jpg', 194, 259, 3939, 'image/jpeg', TRUE, 0),
    ('XL03', NULL, 'seed/excel/5df1e34b.jpg', 1020, 680, 10643, 'image/jpeg', TRUE, 0),
    ('XL04', NULL, 'seed/excel/5d27e518.jpg', 554, 554, 14748, 'image/jpeg', TRUE, 0),
    ('XL05', NULL, 'seed/excel/26b20a0b.jpg', 700, 700, 40586, 'image/jpeg', TRUE, 0),
    ('XL06', NULL, 'seed/excel/e994a744.jpg', 554, 554, 21467, 'image/jpeg', TRUE, 0),
    ('XL07', NULL, 'seed/excel/eea1d85b.jpg', 1000, 1000, 22378, 'image/jpeg', TRUE, 0),
    ('XL08', NULL, 'seed/excel/4dbeb8c6.jpg', 700, 700, 38438, 'image/jpeg', TRUE, 0),
    ('XL09', NULL, 'seed/excel/c3d91176.jpg', 447, 447, 14529, 'image/jpeg', TRUE, 0),
    ('XL10', NULL, 'seed/excel/c73f1cfa.jpg', 547, 365, 9054, 'image/jpeg', TRUE, 0),
    ('XL11', NULL, 'seed/excel/4e169ef1.jpg', 225, 225, 6092, 'image/jpeg', TRUE, 0),
    ('XL12', NULL, 'seed/excel/f9cb364d.jpg', 275, 183, 14011, 'image/jpeg', TRUE, 0),
    ('XL13', NULL, 'seed/excel/8ca12b8c.jpg', 600, 600, 16789, 'image/jpeg', TRUE, 0),
    ('XL14', NULL, 'seed/excel/551ee910.jpg', 358, 358, 6550, 'image/jpeg', TRUE, 0),
    ('XL15', NULL, 'seed/excel/aa52e2d2.jpg', 447, 447, 16907, 'image/jpeg', TRUE, 0),
    ('XL16', NULL, 'seed/excel/c697c18c.jpg', 447, 447, 14536, 'image/jpeg', TRUE, 0),
    ('XL17', NULL, 'seed/excel/c55e2556.jpg', 500, 500, 19109, 'image/jpeg', TRUE, 0),
    ('XL18', NULL, 'seed/excel/9df08c3b.jpg', 840, 840, 42653, 'image/jpeg', TRUE, 0),
    ('XL19', NULL, 'seed/excel/9403dd3e.jpg', 447, 447, 11577, 'image/jpeg', TRUE, 0),
    ('XL20', NULL, 'seed/excel/3821c458.jpg', 447, 447, 8587, 'image/jpeg', TRUE, 0),
    ('XL21', NULL, 'seed/excel/9290be75.jpg', 900, 900, 67088, 'image/jpeg', TRUE, 0),
    ('XL22', NULL, 'seed/excel/2d7f4060.jpg', 1200, 1200, 128732, 'image/jpeg', TRUE, 0),
    ('XL23', NULL, 'seed/excel/1e4a552f.jpg', 399, 501, 16977, 'image/jpeg', TRUE, 0),
    ('XL24', NULL, 'seed/excel/27659266.jpg', 720, 900, 36615, 'image/jpeg', TRUE, 0),
    ('XL25', NULL, 'seed/excel/54f816ab.jpg', 1000, 1000, 52066, 'image/jpeg', TRUE, 0),
    ('XL26', NULL, 'seed/excel/a085bd18.jpg', 337, 427, 18164, 'image/jpeg', TRUE, 0),
    ('XL27', NULL, 'seed/excel/bc9280ac.jpg', 1024, 1024, 49271, 'image/jpeg', TRUE, 0),
    ('XL28', NULL, 'seed/excel/f572c92c.jpg', 800, 1200, 50810, 'image/jpeg', TRUE, 0),
    ('XL29', NULL, 'seed/excel/f4ef3b6d.jpg', 183, 275, 5841, 'image/jpeg', TRUE, 0),
    ('XL30', NULL, 'seed/excel/2d881d94.jpg', 387, 516, 11955, 'image/jpeg', TRUE, 0),
    ('XL31', NULL, 'seed/excel/7b37083a.jpg', 1200, 779, 37221, 'image/jpeg', TRUE, 0),
    ('XL32', NULL, 'seed/excel/aaae3109.jpg', 600, 600, 51910, 'image/jpeg', TRUE, 0),
    ('XL33', NULL, 'seed/excel/af103795.jpg', 1080, 1080, 80908, 'image/jpeg', TRUE, 0),
    ('XL34', NULL, 'seed/excel/4a0b11d6.jpg', 600, 600, 20376, 'image/jpeg', TRUE, 0),
    ('XL35', NULL, 'seed/excel/46bb8db7.jpg', 600, 600, 38921, 'image/jpeg', TRUE, 0),
    ('XL36', NULL, 'seed/excel/22653249.jpg', 1020, 680, 98896, 'image/jpeg', TRUE, 0),
    ('XL37', NULL, 'seed/excel/10ba1639.jpg', 1020, 680, 120934, 'image/jpeg', TRUE, 0),
    ('XL38', NULL, 'seed/excel/ef40f031.jpg', 800, 800, 12217, 'image/jpeg', TRUE, 0),
    ('XL39', NULL, 'seed/excel/459dfdb8.jpg', 500, 500, 6033, 'image/jpeg', TRUE, 0),
    ('XL40', NULL, 'seed/excel/c40a8a26.jpg', 502, 493, 7136, 'image/jpeg', TRUE, 0),
    ('XL41', NULL, 'seed/excel/1a39b46e.jpg', 447, 447, 8184, 'image/jpeg', TRUE, 0),
    ('XL42', NULL, 'seed/excel/1e5ae77c.jpg', 1200, 1200, 76171, 'image/jpeg', TRUE, 0),
    ('XL43', NULL, 'seed/excel/09a771db.jpg', 447, 447, 12010, 'image/jpeg', TRUE, 0),
    ('XL44', NULL, 'seed/excel/dc182e5a.jpg', 1200, 1200, 52548, 'image/jpeg', TRUE, 0),
    ('XL45', NULL, 'seed/excel/6865b89d.jpg', 554, 554, 13861, 'image/jpeg', TRUE, 0),
    ('XL46', NULL, 'seed/excel/15a2e640.jpg', 900, 900, 42479, 'image/jpeg', TRUE, 0),
    ('XL47', NULL, 'seed/excel/dd5d64ae.jpg', 760, 1130, 67104, 'image/jpeg', TRUE, 0),
    ('XL48', NULL, 'seed/excel/4f694e1e.jpg', 777, 799, 45799, 'image/jpeg', TRUE, 0),
    ('XL49', NULL, 'seed/excel/a13a14ad.jpg', 1024, 1024, 129694, 'image/jpeg', TRUE, 0),
    ('XL50', NULL, 'seed/excel/44ffc60c.jpg', 900, 900, 76608, 'image/jpeg', TRUE, 0),
    ('XL51', NULL, 'seed/excel/afdf894a.jpg', 500, 500, 17262, 'image/jpeg', TRUE, 0),
    ('XL52', NULL, 'seed/excel/3e7b9864.jpg', 494, 619, 21502, 'image/jpeg', TRUE, 0),
    ('XL53', NULL, 'seed/excel/8153c26f.jpg', 461, 433, 21953, 'image/jpeg', TRUE, 0),
    ('XL54', NULL, 'seed/excel/b0dc4a23.jpg', 554, 554, 24861, 'image/jpeg', TRUE, 0);

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
