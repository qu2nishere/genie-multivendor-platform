-- Dữ liệu demo cho máy dev / môi trường demo (ENTITY 13.2, plan AUTH A02.04). Chỉ chạy khi placeholder seed_demo = true
-- (profile local đặt true; mặc định false — xem application.yml). Chạy lại được nhiều lần (ON CONFLICT / NOT EXISTS).
--
-- Tài khoản (mật khẩu chung: Genie@123 — hash Argon2id sinh bằng scripts/hash-password.py), trùng dữ liệu mock của web:
--   mai.tran@gmail.com     CUSTOMER  — có hồ sơ + địa chỉ mặc định (shop TechZone thêm ở plan VENDOR)
--   yen.dh@genie.example   ADMIN     — đủ 13 quyền
--   phat.nt@genie.example  STAFF MODERATOR · linh.tm@genie.example STAFF FINANCE_OFFICER · truc.ht@genie.example STAFF CS_SPECIALIST

-- ===== users =====
INSERT INTO users (email, password, role, status, email_verified_at, failed_login_attempts, version, created_at, updated_at)
SELECT v.email, '$argon2id$v=19$m=19456,t=2,p=1$ZebNGoJ3c4LESBr+rpyuLQ$9pb9gk4E8gkfmpVrHDFU7QHxJBsSLf1b8ysu6AWoH4M', v.role, 'ACTIVE', now(), 0, 0, now(), now()
FROM (VALUES
    ('mai.tran@gmail.com', 'CUSTOMER'),
    ('yen.dh@genie.example', 'ADMIN'),
    ('phat.nt@genie.example', 'STAFF'),
    ('linh.tm@genie.example', 'STAFF'),
    ('truc.ht@genie.example', 'STAFF')
) AS v(email, role)
WHERE '${seed_demo}' = 'true'
ON CONFLICT (email) DO NOTHING;

-- ===== customer_profiles =====
INSERT INTO customer_profiles (user_id, full_name, phone_number, gender, date_of_birth, bio, preferred_language, created_at, updated_at)
SELECT u.id, 'Trần Thị Mai', '0901234567', 'FEMALE', DATE '1996-08-12', 'Thích săn deal đồ công nghệ & mỹ phẩm', 'vi', now(), now()
FROM users u
WHERE '${seed_demo}' = 'true' AND u.email = 'mai.tran@gmail.com'
ON CONFLICT (user_id) DO NOTHING;

-- ===== addresses — địa giới 2 cấp (V16; plan AUTH D25), mã theo regions.json / API_SPEC 3.8–3.9:
--       Thành phố Hồ Chí Minh 79 · Phường Sài Gòn 26740 (trước sáp nhập: P. Bến Nghé, Quận 1) =====
INSERT INTO addresses (user_id, recipient_name, phone_number, address_line, ward, ward_code, district, district_id,
                       province, province_id, is_default, label, is_deleted, created_at, updated_at)
SELECT u.id, 'Trần Thị Mai', '0901234567', '25 Nguyễn Huệ', 'Phường Sài Gòn', '26740', NULL, NULL,
       'Thành phố Hồ Chí Minh', 79, TRUE, 'HOME', FALSE, now(), now()
FROM users u
WHERE '${seed_demo}' = 'true' AND u.email = 'mai.tran@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.user_id = u.id AND a.is_deleted = FALSE);

-- địa chỉ mẫu đã seed theo địa giới cũ (máy dev chạy seed trước V16) → đổi sang địa giới mới
UPDATE addresses a
SET ward = 'Phường Sài Gòn', ward_code = '26740', district = NULL, district_id = NULL,
    province = 'Thành phố Hồ Chí Minh', province_id = 79, updated_at = now()
FROM users u
WHERE '${seed_demo}' = 'true' AND u.email = 'mai.tran@gmail.com' AND a.user_id = u.id
  AND a.ward_code = '20308' AND a.province_id = 202;

UPDATE customer_profiles p
SET default_address_id = a.id
FROM users u JOIN addresses a ON a.user_id = u.id AND a.is_default = TRUE AND a.is_deleted = FALSE
WHERE '${seed_demo}' = 'true' AND u.email = 'mai.tran@gmail.com' AND p.user_id = u.id AND p.default_address_id IS NULL;

-- ===== admin_profiles =====
INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at)
SELECT u.id, v.full_name, v.moderator, v.title, now(), now()
FROM (VALUES
    ('yen.dh@genie.example', 'Đặng Hải Yến', FALSE, NULL),
    ('phat.nt@genie.example', 'Ngô Tấn Phát', TRUE, 'MODERATOR'),
    ('linh.tm@genie.example', 'Trịnh Mỹ Linh', FALSE, 'FINANCE_OFFICER'),
    ('truc.ht@genie.example', 'Hồ Thanh Trúc', FALSE, 'CS_SPECIALIST')
) AS v(email, full_name, moderator, title)
JOIN users u ON u.email = v.email
WHERE '${seed_demo}' = 'true'
ON CONFLICT (user_id) DO NOTHING;

-- ===== staff_permissions — bộ quyền mặc định theo chức danh (StaffTitle.defaultPermissions(), LLD 11.1) =====
INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at)
SELECT s.id, v.permission, a.id, now()
FROM (VALUES
    ('phat.nt@genie.example', 'VIEW_DASHBOARD'), ('phat.nt@genie.example', 'REVIEW_KYC'),
    ('phat.nt@genie.example', 'MANAGE_CATALOG'), ('phat.nt@genie.example', 'MODERATE_REVIEW'),
    ('linh.tm@genie.example', 'VIEW_DASHBOARD'), ('linh.tm@genie.example', 'PROCESS_PAYOUT'),
    ('linh.tm@genie.example', 'UPDATE_COMMISSION'), ('linh.tm@genie.example', 'VIEW_FINANCE'),
    ('linh.tm@genie.example', 'MANAGE_VOUCHER'),
    ('truc.ht@genie.example', 'VIEW_DASHBOARD'), ('truc.ht@genie.example', 'HANDLE_DISPUTE'),
    ('truc.ht@genie.example', 'MODERATE_REVIEW')
) AS v(email, permission)
JOIN users s ON s.email = v.email
JOIN users a ON a.email = 'yen.dh@genie.example'
WHERE '${seed_demo}' = 'true'
  AND NOT EXISTS (SELECT 1 FROM staff_permissions x
                  WHERE x.staff_user_id = s.id AND x.permission = v.permission AND x.revoked_at IS NULL);
