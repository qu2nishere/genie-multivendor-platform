-- Dữ liệu mẫu 50 — voucher sàn / shop / freeship + 3 voucher đổi Điểm (plan seed 2026-11-16 S05.01–S05.02; implement 3.1; API 3.13).
-- SQL thuần, chạy lại được (vouchers.code UNIQUE), không ghi cứng id. Thời hạn tương đối để luôn còn hiệu lực khi demo.
-- Quy tắc entity Voucher (ENTITY): SHOP → có vendor_id + sponsor_type VENDOR; PLATFORM / FREESHIP → không gắn shop, sponsor PLATFORM.
-- Không seed voucher "hoàn Điểm": ck_vouchers_type chỉ có SHOP / PLATFORM / FREESHIP — hoàn Điểm thuộc luồng đơn hàng (ORDER_CASHBACK).
-- COIN500 / COIN1000 / COIN2000 = QUÀ ĐỔI ĐIỂM (plan Điểm Genie D3: genie.coin.rewards[] trỏ tới mã) — không phải voucher phát công khai;
-- bảng vouchers không có cờ ẩn nên danh sách voucher công khai phải loại các mã nằm trong genie.coin.rewards[].

INSERT INTO vouchers (code, title, scope, type, discount_type, discount_value, max_discount_cap, min_order_value,
                      usage_limit, usage_per_user, used_count, priority, vendor_id, valid_from, valid_to, is_active,
                      version, budget, total_discount_used, notification_body, sponsor_type, platform_share_percent)
SELECT v.code, v.title, 'ALL', v.type, v.discount_type, v.discount_value, v.max_cap, v.min_order,
       v.usage_limit, v.per_user, 0, v.priority, u.id,
       now() - interval '1 day', now() + make_interval(days => v.valid_days), TRUE,
       0, v.budget, 0, v.notification, v.sponsor, NULL
FROM (VALUES
    -- mã             tiêu đề                                        loại        kiểu giảm       giá trị  trần    đơn từ   lượt    /khách ưu tiên shop (email)          ngày ngân sách  nhà tài trợ thông báo
    ('GENIE10',      'Giảm 10% tối đa 100.000₫ đơn từ 500.000₫',     'PLATFORM', 'PERCENTAGE',   10,      100000, 500000,  1000,   1,     2, NULL,                 60,  50000000, 'PLATFORM', 'Genie tặng bạn mã giảm 10% cho mọi ngành hàng.'),
    ('GENIE50K',     'Giảm 50.000₫ đơn từ 300.000₫',                 'PLATFORM', 'FIXED_AMOUNT', 50000,   NULL,   300000,  500,    1,     2, NULL,                 60,  25000000, 'PLATFORM', NULL),
    ('TECHZONE5',    'TechZone giảm 5% tối đa 200.000₫',             'SHOP',     'PERCENTAGE',   5,       200000, 1000000, 200,    1,     1, 'mai.tran@gmail.com', 60,  NULL,     'VENDOR',   NULL),
    ('TECHZONE100K', 'TechZone giảm 100.000₫ đơn từ 2.000.000₫',     'SHOP',     'FIXED_AMOUNT', 100000,  NULL,   2000000, 100,    1,     1, 'mai.tran@gmail.com', 60,  NULL,     'VENDOR',   NULL),
    ('FREESHIP30K',  'Freeship tối đa 30.000₫ đơn từ 99.000₫',       'FREESHIP', 'FREESHIP',     30000,   30000,  99000,   2000,   3,     3, NULL,                 60,  NULL,     'PLATFORM', NULL),
    ('COIN500',      'Quà đổi Điểm: giảm 5.000₫ đơn từ 50.000₫',     'PLATFORM', 'FIXED_AMOUNT', 5000,    NULL,   50000,   100000, 10,    2, NULL,                 365, NULL,     'PLATFORM', NULL),
    ('COIN1000',     'Quà đổi Điểm: giảm 10.000₫ đơn từ 100.000₫',   'PLATFORM', 'FIXED_AMOUNT', 10000,   NULL,   100000,  100000, 10,    2, NULL,                 365, NULL,     'PLATFORM', NULL),
    ('COIN2000',     'Quà đổi Điểm: giảm 20.000₫ đơn từ 200.000₫',   'PLATFORM', 'FIXED_AMOUNT', 20000,   NULL,   200000,  100000, 10,    2, NULL,                 365, NULL,     'PLATFORM', NULL)
) AS v(code, title, type, discount_type, discount_value, max_cap, min_order, usage_limit, per_user, priority, owner_email,
       valid_days, budget, sponsor, notification)
LEFT JOIN users u ON u.email = v.owner_email
WHERE v.owner_email IS NULL OR u.id IS NOT NULL
ON CONFLICT (code) DO NOTHING;
