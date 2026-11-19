-- Dữ liệu mẫu 60 — banner trang chủ theo vị trí (plan seed 2026-11-16 S05.03–S05.04; mock web BANNER_SEED).
-- SQL thuần, chạy lại được (chống trùng theo title + position), không ghi cứng id.
-- image_url = KHÓA ĐỐI TƯỢNG 'seed/banner/<tệp>' trong bucket content-assets (N7) — scripts/seed/upload-assets.sh đẩy ảnh lên Silo.
-- target_url là đường dẫn trong sàn (bắt đầu bằng '/'); /shop/{id} dùng vendor_profiles.id thật (tra theo email chủ shop).
-- Đủ 4 trạng thái cho màn Admin: LIVE (đa số), SCHEDULED ("Freeship" POPUP), ENDED ("Tết 2026" — ảnh mượn camp-1212),
-- OFF ("Mở shop" POPUP). created_by_admin_id = yen.dh@genie.example.

INSERT INTO home_banners (title, image_url, mobile_image_url, target_url, position, category_id, sort_order,
                          starts_at, ends_at, is_active, target_audience_json, created_by_admin_id,
                          impression_count, click_count, created_at, updated_at)
SELECT v.title, 'seed/banner/' || v.image || '.jpg', NULL,
       CASE WHEN v.shop_email IS NOT NULL THEN '/shop/' || vp.id ELSE v.target END,
       v.position, c.id, v.sort_order,
       now() + make_interval(days => v.start_days), now() + make_interval(days => v.end_days), v.active, NULL, a.id,
       0, 0, now(), now()
FROM (VALUES
    -- tiêu đề                              ảnh             đích                          vị trí               danh mục      shop (email)                    thứ tự bắt đầu kết thúc bật
    ('Nhận hàng rồi, tiền mới đến shop',   'home-trust',    '/discover',                  'HOME_TOP_CAROUSEL', NULL,         NULL,                           1, -30,  1500, TRUE),
    ('Công nghệ chính hãng',               'home-tech',     '/c/dien-tu',                 'HOME_TOP_CAROUSEL', NULL,         NULL,                           2, -30,  1500, TRUE),
    ('Làm đẹp mỗi ngày',                   'home-beauty',   '/c/lam-dep',                 'HOME_TOP_CAROUSEL', NULL,         NULL,                           3, -30,  1500, TRUE),
    ('Thời trang mới về',                  'home-fashion',  '/c/thoi-trang',              'HOME_TOP_CAROUSEL', NULL,         NULL,                           4, -30,  1500, TRUE),
    ('Siêu sale 12.12',                    'camp-1212',     '/campaign/sieu-sale-12-12',  'HOME_TOP_CAROUSEL', NULL,         NULL,                           5, -7,   30,   TRUE),
    ('Freeship đơn từ 99.000₫',            'side-freeship', '/#flash-sale',               'HOME_TOP_SIDE',     NULL,         NULL,                           1, -30,  1500, TRUE),
    ('Mở shop 0₫ trên Genie',              'side-seller',   '/become-seller',             'HOME_TOP_SIDE',     NULL,         NULL,                           2, -30,  1500, TRUE),
    ('Nhà cửa và đời sống',                'mid-home',      '/c/nha-cua',                 'HOME_MIDDLE',       NULL,         NULL,                           1, -30,  1500, TRUE),
    ('Phụ kiện công nghệ',                 'mid-acc',       '/c/phu-kien',                'HOME_MIDDLE',       NULL,         NULL,                           2, -30,  1500, TRUE),
    ('Shop nổi bật: Beauty Box',           'cat-beauty',    NULL,                         'CATEGORY_TOP',      'lam-dep',    'owner.beauty@genie.example',   1, -30,  1500, TRUE),
    ('Shop nổi bật: TechZone Official',    'cat-tech',      NULL,                         'CATEGORY_TOP',      'dien-tu',    'mai.tran@gmail.com',           1, -30,  1500, TRUE),
    ('Shop nổi bật: Boutique Sài Gòn',     'cat-fashion',   NULL,                         'CATEGORY_TOP',      'thoi-trang', 'owner.boutique@genie.example', 1, -30,  1500, TRUE),
    ('Shop nổi bật: Home & Living',        'cat-home',      NULL,                         'CATEGORY_TOP',      'nha-cua',    'owner.home@genie.example',     1, -30,  1500, TRUE),
    ('Shop nổi bật: TravelGear',           'cat-acc',       NULL,                         'CATEGORY_TOP',      'phu-kien',   'owner.travel@genie.example',   1, -30,  1500, TRUE),
    ('Tai nghe ANC Pro 5 chỉ 389.000₫',    'flash-anc',     '/c/thiet-bi-am-thanh',       'FLASH_SALE_TOP',    NULL,         NULL,                           1, -30,  1500, TRUE),
    ('Siêu sale 12.12',                    'camp-1212',     '/campaign/sieu-sale-12-12',  'POPUP',             NULL,         NULL,                           1, -7,   30,   TRUE),
    ('Freeship đơn từ 99.000₫',            'side-freeship', '/#flash-sale',               'POPUP',             NULL,         NULL,                           2, 3,    60,   TRUE),
    ('Mở shop 0₫ trên Genie',              'side-seller',   '/become-seller',             'POPUP',             NULL,         NULL,                           3, -30,  1500, FALSE),
    ('Tết 2026 giảm sâu',                  'camp-1212',     '/discover',                  'POPUP',             NULL,         NULL,                           4, -300, -270, TRUE)
) AS v(title, image, target, position, category_slug, shop_email, sort_order, start_days, end_days, active)
JOIN users a ON a.email = 'yen.dh@genie.example'
LEFT JOIN categories c ON c.slug = v.category_slug AND c.parent_id IS NULL
LEFT JOIN users so ON so.email = v.shop_email
LEFT JOIN vendor_profiles vp ON vp.user_id = so.id
WHERE (v.shop_email IS NULL OR vp.id IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM home_banners b WHERE b.title = v.title AND b.position = v.position);
