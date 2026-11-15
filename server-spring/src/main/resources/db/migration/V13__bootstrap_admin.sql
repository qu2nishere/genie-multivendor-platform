-- Tạo Admin đầu tiên khi triển khai (TS 16.5, plan AUTH A02.03) từ biến môi trường, qua placeholder Flyway:
--   BOOTSTRAP_ADMIN_EMAIL          → ${bootstrap_admin_email}
--   BOOTSTRAP_ADMIN_PASSWORD_HASH  → ${bootstrap_admin_password_hash}   (sinh bằng: python3 scripts/hash-password.py '<mật khẩu>')
-- Hash rỗng → bỏ qua (máy dev dùng tài khoản của R__seed_demo.sql). Không dùng DO $$ … $$ vì hash Argon2 chứa ký tự '$'.

INSERT INTO users (email, password, role, status, email_verified_at, failed_login_attempts, version, created_at, updated_at)
SELECT lower('${bootstrap_admin_email}'), '${bootstrap_admin_password_hash}', 'ADMIN', 'ACTIVE', now(), 0, 0, now(), now()
WHERE '${bootstrap_admin_password_hash}' <> ''
  AND NOT EXISTS (SELECT 1 FROM users WHERE lower(email) = lower('${bootstrap_admin_email}'));

INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, created_at, updated_at)
SELECT u.id, 'Quản trị viên', FALSE, now(), now()
FROM users u
WHERE u.email = lower('${bootstrap_admin_email}')
  AND u.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM admin_profiles a WHERE a.user_id = u.id);
