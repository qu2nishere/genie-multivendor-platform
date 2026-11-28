-- Dữ liệu mẫu 50 — voucher sàn / shop / freeship + 3 voucher đổi Điểm (plan seed 2026-11-16 S05.01–S05.02; implement 3.1; API 3.13)
-- + 3 voucher shop cho bộ lọc "Shop có voucher" (plan 2026-11-24 D10: còn dùng được / đã tắt / hết lượt).
-- SQL thuần, chạy lại được (vouchers.code UNIQUE), không ghi cứng id. Thời hạn tương đối để luôn còn hiệu lực khi demo.
-- Quy tắc entity Voucher (ENTITY): SHOP → có vendor_id + sponsor_type VENDOR; PLATFORM / FREESHIP → không gắn shop, sponsor PLATFORM.
-- Không seed voucher "hoàn Điểm": ck_vouchers_type chỉ có SHOP / PLATFORM / FREESHIP — hoàn Điểm thuộc luồng đơn hàng (ORDER_CASHBACK).
-- COIN500 / COIN1000 / COIN2000 = QUÀ ĐỔI ĐIỂM (plan Điểm Genie D3: genie.coin.rewards[] trỏ tới mã) — không phải voucher phát công khai;
-- bảng vouchers không có cờ ẩn nên danh sách voucher công khai phải loại các mã nằm trong genie.coin.rewards[].

INSERT INTO vouchers (code, title, scope, type, discount_type, discount_value, max_discount_cap, min_order_value,
                      usage_limit, usage_per_user, used_count, priority, vendor_id, valid_from, valid_to, is_active,
                      version, budget, total_discount_used, notification_body, sponsor_type, platform_share_percent)
SELECT v.code, v.title, 'ALL', v.type, v.discount_type, v.discount_value, v.max_cap, v.min_order,
       v.usage_limit, v.per_user, v.used, v.priority, u.id,
       now() - interval '1 day', now() + make_interval(days => v.valid_days), v.active,
       0, v.budget, 0, v.notification, v.sponsor, NULL
FROM (VALUES
    -- mã             tiêu đề                                        loại        kiểu giảm       giá trị  trần    đơn từ   lượt    /khách ưu tiên shop (email)          ngày ngân sách  nhà tài trợ thông báo  đã dùng bật
    ('GENIE10',      'Giảm 10% tối đa 100.000₫ đơn từ 500.000₫',     'PLATFORM', 'PERCENTAGE',   10,      100000, 500000,  1000,   1,     2, NULL,                 60,  50000000, 'PLATFORM', 'Genie tặng bạn mã giảm 10% cho mọi ngành hàng.', 0,   TRUE),
    ('GENIE50K',     'Giảm 50.000₫ đơn từ 300.000₫',                 'PLATFORM', 'FIXED_AMOUNT', 50000,   NULL,   300000,  500,    1,     2, NULL,                 60,  25000000, 'PLATFORM', NULL, 0,   TRUE),
    ('TECHZONE5',    'TechZone giảm 5% tối đa 200.000₫',             'SHOP',     'PERCENTAGE',   5,       200000, 1000000, 200,    1,     1, 'mai.tran@gmail.com', 60,  NULL,     'VENDOR',   NULL, 0,   TRUE),
    ('TECHZONE100K', 'TechZone giảm 100.000₫ đơn từ 2.000.000₫',     'SHOP',     'FIXED_AMOUNT', 100000,  NULL,   2000000, 100,    1,     1, 'mai.tran@gmail.com', 60,  NULL,     'VENDOR',   NULL, 0,   TRUE),
    ('FREESHIP30K',  'Freeship tối đa 30.000₫ đơn từ 99.000₫',       'FREESHIP', 'FREESHIP',     30000,   30000,  99000,   2000,   3,     3, NULL,                 60,  NULL,     'PLATFORM', NULL, 0,   TRUE),
    ('COIN500',      'Quà đổi Điểm: giảm 5.000₫ đơn từ 50.000₫',     'PLATFORM', 'FIXED_AMOUNT', 5000,    NULL,   50000,   100000, 10,    2, NULL,                 365, NULL,     'PLATFORM', NULL, 0,   TRUE),
    ('COIN1000',     'Quà đổi Điểm: giảm 10.000₫ đơn từ 100.000₫',   'PLATFORM', 'FIXED_AMOUNT', 10000,   NULL,   100000,  100000, 10,    2, NULL,                 365, NULL,     'PLATFORM', NULL, 0,   TRUE),
    ('COIN2000',     'Quà đổi Điểm: giảm 20.000₫ đơn từ 200.000₫',   'PLATFORM', 'FIXED_AMOUNT', 20000,   NULL,   200000,  100000, 10,    2, NULL,                 365, NULL,     'PLATFORM', NULL, 0,   TRUE),
    -- plan 2026-11-24 D10: bộ lọc "Shop có voucher" (API 5.3) — 1 shop thứ hai có voucher dùng được, 1 shop voucher đã tắt, 1 shop voucher hết lượt
    ('SHOEREP30K',   'Shoe Republic giảm 30.000₫ đơn từ 399.000₫',   'SHOP',     'FIXED_AMOUNT', 30000,   NULL,   399000,  300,    1,     1, 'owner.shoe@genie.example',   60,  NULL,     'VENDOR',   NULL, 0,   TRUE),
    ('HOMEOFF10',    'Home & Living giảm 10% (đã tắt)',              'SHOP',     'PERCENTAGE',   10,      50000,  300000,  200,    1,     1, 'owner.home@genie.example',   60,  NULL,     'VENDOR',   NULL, 0,   FALSE),
    ('BEAUTY20K',    'Beauty Box giảm 20.000₫ (đã hết lượt)',        'SHOP',     'FIXED_AMOUNT', 20000,   NULL,   200000,  50,     1,     1, 'owner.beauty@genie.example', 60,  NULL,     'VENDOR',   NULL, 50,  TRUE)
) AS v(code, title, type, discount_type, discount_value, max_cap, min_order, usage_limit, per_user, priority, owner_email,
       valid_days, budget, sponsor, notification, used, active)
LEFT JOIN users u ON u.email = v.owner_email
WHERE v.owner_email IS NULL OR u.id IS NOT NULL
ON CONFLICT (code) DO NOTHING;

-- plan 2026-11-26 voucher V09.01 (D1, D10, D8): demo đủ tab trạng thái + phạm vi theo ngành + freeship của shop.
-- GENIESOON: voucher sàn SẮP DIỄN RA (bắt đầu sau 2 ngày) — tab "Sắp ra mắt" Admin, nhãn "Chưa tới giờ" ở Kho.
-- TECHAUDIO10: TechZone chỉ áp cho ngành "Thiết bị âm thanh", loại trừ "Phụ kiện điện tử" (2 bảng phạm vi tra theo slug).
-- TECHSHIP25: TechZone tài trợ phí ship (voucher SHOP + discount_type FREESHIP — lưu trần hỗ trợ vào discount_value).
INSERT INTO vouchers (code, title, scope, type, discount_type, discount_value, max_discount_cap, min_order_value,
                      usage_limit, usage_per_user, used_count, priority, vendor_id, valid_from, valid_to, is_active,
                      version, budget, total_discount_used, notification_body, sponsor_type, platform_share_percent)
SELECT v.code, v.title, v.scope, v.type, v.discount_type, v.discount_value, v.max_cap, v.min_order,
       v.usage_limit, v.per_user, 0, v.priority, u.id,
       now() + make_interval(days => v.start_in), now() + make_interval(days => v.start_in + v.valid_days), TRUE,
       0, v.budget, 0, v.notification, v.sponsor, v.share
FROM (VALUES
    ('GENIESOON',   'Sale cuối tuần — giảm 15% tối đa 80.000₫',          'ALL',      'PLATFORM', 'PERCENTAGE', 15,     80000, 300000, 2000, 1, 2, NULL::text,           2, 14, 30000000, 'CO_SPONSOR', 'Sale cuối tuần sắp bắt đầu — lưu mã trước nhé!', 60),
    ('TECHAUDIO10', 'Giảm 10% tối đa 150.000₫ đơn từ 500.000₫',          'CATEGORY', 'SHOP',     'PERCENTAGE', 10,     150000, 500000, 300, 1, 1, 'mai.tran@gmail.com', 0, 45, NULL,     'VENDOR',     NULL, NULL),
    ('TECHSHIP25',  'Freeship tối đa 25.000₫ đơn từ 150.000₫',           'ALL',      'SHOP',     'FREESHIP',   25000,  25000, 150000, 500, 2, 1, 'mai.tran@gmail.com', 0, 45, NULL,     'VENDOR',     NULL, NULL)
) AS v(code, title, scope, type, discount_type, discount_value, max_cap, min_order, usage_limit, per_user, priority,
       owner_email, start_in, valid_days, budget, sponsor, notification, share)
LEFT JOIN users u ON u.email = v.owner_email
WHERE v.owner_email IS NULL OR u.id IS NOT NULL
ON CONFLICT (code) DO NOTHING;

INSERT INTO voucher_categories (voucher_id, category_id, created_at)
SELECT v.id, c.id, now() FROM vouchers v JOIN categories c ON c.slug = 'thiet-bi-am-thanh'
WHERE v.code = 'TECHAUDIO10'
ON CONFLICT DO NOTHING;

INSERT INTO voucher_excluded_categories (voucher_id, category_id, created_at)
SELECT v.id, c.id, now() FROM vouchers v JOIN categories c ON c.slug = 'phu-kien-dien-tu'
WHERE v.code = 'TECHAUDIO10'
ON CONFLICT DO NOTHING;
