-- V14 — Bổ sung schema cho phân hệ AUTH (plan/2026-11-14/auth.md D6, D7, D20, D21, D22; A02.01)
-- V1…V12 đã merge → KHÔNG sửa; mọi thay đổi đi vào file mới. Thứ tự an toàn: thêm cột cho phép NULL → điền → SET NOT NULL.
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>

-- ===== D6: phiên đăng nhập ổn định qua các lần xoay vòng refresh token =====
CREATE SEQUENCE refresh_token_family_seq;

ALTER TABLE refresh_tokens ADD COLUMN family_id BIGINT;
ALTER TABLE refresh_tokens ADD COLUMN last_used_at TIMESTAMPTZ;
UPDATE refresh_tokens SET family_id = id WHERE family_id IS NULL;
ALTER TABLE refresh_tokens ALTER COLUMN family_id SET NOT NULL;
SELECT setval('refresh_token_family_seq', COALESCE((SELECT MAX(id) FROM refresh_tokens), 0) + 1, false);

-- ===== D7: yêu cầu xóa tài khoản (UC-AUTH-10) =====
ALTER TABLE users ADD COLUMN deletion_requested_at TIMESTAMPTZ;

-- ===== D21: email không phân biệt hoa thường (ứng dụng luôn lưu chữ thường; index chặn dữ liệu nhập tay sai) =====
CREATE UNIQUE INDEX uk_users_email_lower ON users (lower(email));

-- ===== D22: index đường truy vấn của đăng nhập / thiết bị / sổ địa chỉ / Cron xóa tài khoản =====
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_created ON refresh_tokens (user_id, created_at DESC);
CREATE INDEX idx_addresses_user ON addresses (user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_deletion_requested_at ON users (deletion_requested_at) WHERE deletion_requested_at IS NOT NULL;

-- ===== D20: hành động kiểm toán của AUTH — giữ đủ giá trị cũ của V12 + 8 giá trị mới (khớp enum AuditAction) =====
ALTER TABLE system_logs DROP CONSTRAINT ck_system_logs_action_type;
ALTER TABLE system_logs ADD CONSTRAINT ck_system_logs_action_type CHECK (action_type IN (
    'LOGIN_SUCCESS', 'LOGIN_FAILED', 'ACCOUNT_LOCKED', 'ACCOUNT_SUSPENDED', 'ACCOUNT_REACTIVATED',
    'KYC_SUBMITTED', 'KYC_APPROVED', 'KYC_REJECTED', 'ORDER_STATUS_CHANGED', 'ESCROW_RELEASED',
    'PAYOUT_APPROVED', 'PAYOUT_REJECTED', 'REFUND_PROCESSED', 'REVIEW_HIDDEN', 'COMMISSION_UPDATED',
    'PERMISSION_GRANTED', 'PERMISSION_REVOKED', 'RBAC_ROLE_CHANGED', 'STAFF_INVITED', 'PII_ANONYMIZED',
    'ACCESS_DENIED', 'PRODUCT_MODERATED',
    'PASSWORD_CHANGED', 'PASSWORD_RESET', 'EMAIL_CHANGED', 'OAUTH_LINKED', 'OAUTH_UNLINKED', 'SESSION_REVOKED',
    'ACCOUNT_DELETION_REQUESTED', 'ACCOUNT_DELETION_CANCELLED'
));
