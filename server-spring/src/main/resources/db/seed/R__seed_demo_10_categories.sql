-- Dữ liệu mẫu 10 — danh mục gốc + con, tỷ lệ hoa hồng theo ngành (plan seed 2026-11-16 S02; mock web SEED_ROOT_CATEGORIES,
-- SEED_SUB_CATEGORIES). Thư mục db/seed chỉ được Flyway nạp khi genie.seed.demo-data=true (GENIE_SEED_DEMO — profile local),
-- hoặc chạy tay bằng scripts/seed/seed.sh | seed.ps1 (CSDL local, không bao giờ production — ADR 0010).
-- Nguyên tắc: N1 chạy lại được nhiều lần (NOT EXISTS / ON CONFLICT) · N2 không ghi cứng id, tra bằng khóa tự nhiên (slug, email…)
-- · N3 SQL thuần — không placeholder Flyway, không lệnh meta psql.
--
-- categories.slug KHÔNG có ràng buộc UNIQUE (V2) → chống trùng bằng NOT EXISTS theo slug, chạy lại thì UPDATE cột hiển thị.
-- Hoa hồng (CLAUDE.md mục 7, plan S02.03): 3,5–8% theo ngành, mặc định 5%; danh mục con để NULL (kế thừa danh mục gốc).
-- Lưu ý: bảng hoa hồng trên trang chính sách của web (policies.data.ts) đang ghi 1,5–7% — lệch tài liệu nghiệp vụ,
-- seed theo tài liệu. requires_business_license = true với ngành cần giấy phép (mỹ phẩm, mẹ & bé, thực phẩm).

-- ===== 10 danh mục gốc =====
WITH v(slug, name, icon, description, display_order, commission_rate, requires_license) AS (VALUES
    ('dien-tu',    'Điện tử',    'zap',           'Smartphone, laptop, tai nghe, camera và phụ kiện công nghệ chính hãng.', 1, 0.0350, FALSE),
    ('thoi-trang', 'Thời trang', 'shopping-bag',  'Thời trang nam nữ, giày dép, túi xách và phụ kiện theo xu hướng mới nhất.', 2, 0.0800, FALSE),
    ('lam-dep',    'Làm đẹp',    'star',          'Mỹ phẩm, chăm sóc da, trang điểm và nước hoa từ các thương hiệu uy tín.', 3, 0.0700, TRUE),
    ('nha-cua',    'Nhà cửa',    'house',         'Nội thất, đồ dùng nhà bếp, trang trí và đồ gia dụng thông minh.', 4, 0.0600, FALSE),
    ('phu-kien',   'Phụ kiện',   'tag',           'Đồng hồ, trang sức, balo, túi xách và phụ kiện thời trang.', 5, 0.0500, FALSE),
    ('me-be',      'Mẹ & Bé',    'gift',          'Tã bỉm, sữa, đồ chơi, thời trang mẹ và bé đầy đủ.', 6, 0.0550, TRUE),
    ('the-thao',   'Thể thao',   'package',       'Dụng cụ thể thao, quần áo tập luyện và phụ kiện outdoor.', 7, 0.0500, FALSE),
    ('sach',       'Sách',       'file-text',     'Sách văn học, sách kỹ năng, truyện tranh và sách thiếu nhi.', 8, 0.0400, FALSE),
    ('bach-hoa',   'Bách hoá',   'shopping-cart', 'Thực phẩm, đồ uống, gia vị và đồ dùng hằng ngày.', 9, 0.0500, TRUE),
    ('voucher',    'Voucher',    'ticket',        'Voucher giảm giá, thẻ quà tặng và dịch vụ số.', 10, 0.0500, FALSE)
), upd AS (
    UPDATE categories c
    SET name = v.name, icon = v.icon, description = v.description, display_order = v.display_order,
        commission_rate = v.commission_rate, requires_business_license = v.requires_license, is_active = TRUE
    FROM v WHERE c.slug = v.slug AND c.parent_id IS NULL
    RETURNING c.slug
)
INSERT INTO categories (name, slug, parent_id, display_order, icon, description, commission_rate, is_active, requires_business_license)
SELECT v.name, v.slug, NULL, v.display_order, v.icon, v.description, v.commission_rate, TRUE, v.requires_license
FROM v
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.slug = v.slug);

-- ===== 13 danh mục con (parent tra theo slug cha) =====
WITH v(slug, name, icon, description, display_order, parent_slug) AS (VALUES
    ('dien-thoai-phu-kien', 'Điện thoại & Phụ kiện', 'smartphone',   'Điện thoại smartphone, ốp lưng, sạc cáp và phụ kiện chính hãng.', 1, 'dien-tu'),
    ('may-tinh-laptop',     'Máy tính & Laptop',     'laptop',       'Laptop, PC, linh kiện và phụ kiện máy tính.', 2, 'dien-tu'),
    ('thiet-bi-am-thanh',   'Thiết bị âm thanh',     'headphones',   'Tai nghe, loa Bluetooth và thiết bị âm thanh chất lượng cao.', 3, 'dien-tu'),
    ('phu-kien-dien-tu',    'Phụ kiện điện tử',      'plug',         'Sạc, cáp, chuột, bàn phím và phụ kiện công nghệ.', 4, 'dien-tu'),
    ('thoi-trang-nam',      'Thời trang Nam',        'shopping-bag', 'Áo thun, quần jeans, giày và phụ kiện thời trang nam.', 1, 'thoi-trang'),
    ('thoi-trang-nu',       'Thời trang Nữ',         'shopping-bag', 'Đầm, váy, quần áo và phụ kiện thời trang nữ.', 2, 'thoi-trang'),
    ('giay-dep',            'Giày dép',              'footprints',   'Giày sneaker, dép, sandal và giày thể thao.', 3, 'thoi-trang'),
    ('my-pham-cham-soc',    'Mỹ phẩm & Chăm sóc',    'star',         'Kem chống nắng, serum, trang điểm và chăm sóc da.', 1, 'lam-dep'),
    ('thiet-bi-gia-dung',   'Thiết bị gia dụng',     'house',        'Máy lọc không khí, robot hút bụi và đồ gia dụng thông minh.', 1, 'nha-cua'),
    ('noi-that',            'Nội thất',              'house',        NULL, 2, 'nha-cua'),
    ('nha-bep',             'Nhà bếp',               'house',        NULL, 3, 'nha-cua'),
    ('dong-ho-trang-suc',   'Đồng hồ & Trang sức',   'watch',        'Đồng hồ nam nữ, trang sức và phụ kiện thời trang.', 1, 'phu-kien'),
    ('tui-xach-balo',       'Túi xách & Balo',       'briefcase',    'Balo, túi đeo chéo, túi xách thời trang và du lịch.', 2, 'phu-kien')
), parent AS (
    SELECT DISTINCT ON (slug) id, slug FROM categories WHERE parent_id IS NULL ORDER BY slug, id
), upd AS (
    UPDATE categories c
    SET name = v.name, icon = v.icon, description = v.description, display_order = v.display_order,
        parent_id = p.id, commission_rate = NULL, is_active = TRUE
    FROM v JOIN parent p ON p.slug = v.parent_slug
    WHERE c.slug = v.slug
    RETURNING c.slug
)
INSERT INTO categories (name, slug, parent_id, display_order, icon, description, commission_rate, is_active, requires_business_license)
SELECT v.name, v.slug, p.id, v.display_order, v.icon, v.description, NULL, TRUE, FALSE
FROM v JOIN parent p ON p.slug = v.parent_slug
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.slug = v.slug);
