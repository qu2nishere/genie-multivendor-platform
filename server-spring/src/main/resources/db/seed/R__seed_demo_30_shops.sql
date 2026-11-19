-- Dữ liệu mẫu 30 — 6 shop đã KYC + chủ shop, ví, cấu hình vận chuyển, kho lấy hàng (plan seed 2026-11-16 S03; mock web SEED_SHOPS).
-- SQL thuần, chạy lại được, không ghi cứng id. Mật khẩu mọi tài khoản: Genie@123 (cùng chuỗi Argon2id với R__seed_demo.sql).
--
-- Kho lấy hàng: chỉ mã GHN của TP.HCM (202 · Quận 1 1442 · P. Bến Nghé 20308) lấy từ API_SPEC 3.8 / 3.9; mã của Hà Nội,
-- Bình Dương, Đà Nẵng là MÃ MẪU để isPickupConfigured() = true — đối chiếu master-data GHN trước khi tạo vận đơn thật.
-- ghn_shop_id cũng là số mẫu (19000x).
--
-- balance_hash của vendor_wallets = HMAC-SHA256(khóa, 'wallet:' || vendor_id || ':' || balance || ':' || pending_withdrawal),
-- hex chữ thường, số dạng '0.00'. Khóa = WALLET_HMAC_SECRET mặc định máy dev 'local-wallet-secret' (application-local.yml).
-- PostgreSQL không có hmac() khi chưa cài pgcrypto → tính bằng sha256() sẵn có:
--   HMAC = sha256(opad_key || sha256(ipad_key || message)), ipad_key / opad_key = khóa đệm 64 byte XOR 0x36 / 0x5c (hằng bên dưới).
-- Đổi WALLET_HMAC_SECRET → băm seed lệch: xem scripts/seed/README.md mục 4.

DROP TABLE IF EXISTS seed_shop;
CREATE TEMP TABLE seed_shop AS
SELECT * FROM (VALUES
    --  email                        tên chủ shop       shop_name             ngành        loại hình      mall   rating followers resp_rate resp_min năm  free_ship  tỉnh_id tỉnh               quận_id quận                phường_code phường            địa chỉ                       ghn     yêu thích  mô tả
    ('mai.tran@gmail.com',          'Trần Thị Mai',    'TechZone Official',  'dien-tu',    'BUSINESS',    TRUE,  4.9, 128000, 98.00,  5,   2021, 199000,    202, 'TP. Hồ Chí Minh', 1442, 'Quận 1',             '20308',    'P. Bến Nghé',     '25 Nguyễn Huệ',              190001, FALSE, 'Thiết bị công nghệ chính hãng — bảo hành 12 tháng.'),
    ('owner.shoe@genie.example',    'Lê Minh Tuấn',    'Shoe Republic',      'thoi-trang', 'HOUSEHOLD',   FALSE, 4.7, 42100,  95.00,  60,  2022, NULL,      205, 'Bình Dương',      1538, 'TP. Thủ Dầu Một',    '440101',   'P. Phú Cường',    '12 Yersin',                  190002, TRUE,  'Giày sneaker & thời trang chính hãng.'),
    ('owner.boutique@genie.example','Phạm Ngọc Anh',   'Boutique Sài Gòn',   'thoi-trang', 'BUSINESS',    TRUE,  4.8, 67000,  96.00,  5,   2020, NULL,      202, 'TP. Hồ Chí Minh', 1442, 'Quận 1',             '20308',    'P. Bến Nghé',     '88 Đồng Khởi',               190003, FALSE, 'Thời trang nam nữ phong cách Hàn Quốc.'),
    ('owner.home@genie.example',    'Vũ Thị Hằng',     'Home & Living',      'nha-cua',    'HOUSEHOLD',   FALSE, 4.6, 19800,  91.00,  180, 2023, NULL,      201, 'Hà Nội',          1489, 'Quận Hoàn Kiếm',     '1A0807',   'P. Hàng Bạc',     '36 Hàng Bạc',                190004, TRUE,  'Đồ gia dụng & trang trí nhà cửa.'),
    ('owner.beauty@genie.example',  'Đoàn Thu Trang',  'Beauty Box',         'lam-dep',    'BUSINESS',    TRUE,  4.9, 203000, 99.00,  5,   2019, 300000,    202, 'TP. Hồ Chí Minh', 1442, 'Quận 1',             '20308',    'P. Bến Nghé',     '120 Lê Lợi',                 190005, FALSE, 'Mỹ phẩm & chăm sóc da chính hãng.'),
    ('owner.travel@genie.example',  'Đỗ Minh Khang',   'TravelGear',         'phu-kien',   'INDIVIDUAL',  FALSE, 4.5, 8400,   88.00,  60,  2023, NULL,      203, 'Đà Nẵng',         1526, 'Quận Hải Châu',      '40101',    'P. Hải Châu 1',   '45 Bạch Đằng',               190006, FALSE, 'Balo, vali & phụ kiện du lịch.')
) AS v(email, owner_name, shop_name, category_slug, business_type, is_mall, rating, followers, response_rate, response_minutes,
       joined_year, free_ship_from, province_id, province, district_id, district, ward_code, ward, address_line, ghn_shop_id,
       is_favorite, description);

-- ===== S03.01 tài khoản chủ shop (5 tài khoản mới; mai.tran có sẵn từ R__seed_demo.sql) =====
INSERT INTO users (email, password, role, status, email_verified_at, failed_login_attempts, version, created_at, updated_at)
SELECT s.email, '$argon2id$v=19$m=19456,t=2,p=1$ZebNGoJ3c4LESBr+rpyuLQ$9pb9gk4E8gkfmpVrHDFU7QHxJBsSLf1b8ysu6AWoH4M',
       'VENDOR', 'ACTIVE', now(), 0, 0, make_timestamptz(s.joined_year, 3, 1, 9, 0, 0, 'Asia/Ho_Chi_Minh'), now()
FROM seed_shop s
WHERE s.email <> 'mai.tran@gmail.com'
ON CONFLICT (email) DO NOTHING;

INSERT INTO customer_profiles (user_id, full_name, preferred_language, created_at, updated_at)
SELECT u.id, s.owner_name, 'vi', now(), now()
FROM seed_shop s JOIN users u ON u.email = s.email
ON CONFLICT (user_id) DO NOTHING;

-- ===== S03.02 mai.tran vừa mua vừa bán: chủ shop TechZone (D5) =====
UPDATE users SET role = 'VENDOR', updated_at = now()
WHERE email = 'mai.tran@gmail.com' AND role = 'CUSTOMER';

-- ===== S03.03–S03.05, S03.08 vendor_profiles =====
INSERT INTO vendor_profiles (user_id, shop_name, category_id, business_type, representative, shop_description,
                             kyc_status, kyc_verification_method, kyc_verified_by, kyc_submitted_at, kyc_verified_at,
                             pickup_province_id, pickup_province, pickup_district_id, pickup_district, pickup_ward_code,
                             pickup_ward, pickup_address_line, ghn_shop_id,
                             created_at, is_mall, rating_avg, rating_count, follower_count, active_product_count,
                             chat_response_rate, avg_response_minutes, pre_order_enabled, is_favorite, auto_reply_enabled)
SELECT u.id, s.shop_name, c.id, s.business_type, s.owner_name, s.description,
       'VERIFIED', 'MANUAL_STAFF', a.id,
       make_timestamptz(s.joined_year, 3, 1, 9, 0, 0, 'Asia/Ho_Chi_Minh'),
       make_timestamptz(s.joined_year, 3, 2, 9, 0, 0, 'Asia/Ho_Chi_Minh'),
       s.province_id, s.province, s.district_id, s.district, s.ward_code, s.ward, s.address_line, s.ghn_shop_id,
       make_timestamptz(s.joined_year, 3, 1, 9, 0, 0, 'Asia/Ho_Chi_Minh'), s.is_mall, s.rating,
       (s.followers / 40)::int, s.followers, 0, s.response_rate, s.response_minutes, FALSE, s.is_favorite, FALSE
FROM seed_shop s
JOIN users u ON u.email = s.email
LEFT JOIN categories c ON c.slug = s.category_slug AND c.parent_id IS NULL
LEFT JOIN users a ON a.email = 'yen.dh@genie.example'
ON CONFLICT (user_id) DO NOTHING;

-- Tài khoản nhận giải ngân: chỉ TechZone (shop khác để trống — test luồng chưa có tài khoản, UC-VND-07)
UPDATE vendor_profiles vp
SET payout_bank_name = 'Vietcombank', payout_bank_account_number = '0071001234567',
    payout_bank_account_name = 'TRAN THI MAI', payout_account_verified_at = now() - interval '30 days'
FROM users u
WHERE vp.user_id = u.id AND u.email = 'mai.tran@gmail.com' AND vp.payout_bank_account_number IS NULL;

-- Shop đang nghỉ (mock TravelGear.vacation): mốc tương đối để luôn đang nghỉ khi demo
UPDATE vendor_profiles vp
SET vacation_starts_at = now() - interval '2 days', vacation_ends_at = now() + interval '30 days',
    vacation_message = 'Shop đang tạm nghỉ, sẽ trả lời bạn khi mở lại nhé!'
FROM users u
WHERE vp.user_id = u.id AND u.email = 'owner.travel@genie.example'
  AND (vp.vacation_ends_at IS NULL OR vp.vacation_ends_at < now());

-- ===== S03.06 vendor_wallets — số dư 0, balance_hash HMAC (khóa máy dev) =====
INSERT INTO vendor_wallets (vendor_id, balance, pending_withdrawal, balance_hash, version, created_at, updated_at)
SELECT u.id, 0, 0,
       encode(sha256(
           decode('30333f3d30712b3d30303928712f393f2e39285c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c5c', 'hex')
           || sha256(decode('5a5955575a1b41575a5a53421b455355445342363636363636363636363636363636363636363636363636363636363636363636363636363636363636363636', 'hex')
                     || convert_to('wallet:' || u.id || ':0.00:0.00', 'UTF8'))), 'hex'),
       0, now(), now()
FROM seed_shop s JOIN users u ON u.email = s.email
ON CONFLICT (vendor_id) DO NOTHING;

-- ===== S03.07 vendor_shipping_config — GHN bật, ngưỡng freeship theo mock free_ship_from =====
INSERT INTO vendor_shipping_config (vendor_id, ghn_enabled, ghtk_enabled, vtp_enabled, jt_enabled, genie_express_enabled, freeship_threshold)
SELECT u.id, TRUE, TRUE, FALSE, FALSE, FALSE, s.free_ship_from
FROM seed_shop s JOIN users u ON u.email = s.email
ON CONFLICT (vendor_id) DO NOTHING;

DROP TABLE seed_shop;
