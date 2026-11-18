-- V17 — Mở shop & KYC người bán (UC-VND-01/02/03/04/05/07/21; plan 2026-11-16 Mở shop & KYC D2, D3, K01.03;
-- ENTITY 1.4, 13.1, Phụ lục A; ADR 0011). Chỉ THÊM (tương thích ngược): cột cho phép NULL / có mặc định, index, giá trị CHECK.

-- ===== 1. Cột mới của vendor_profiles (D2, D15) =====
ALTER TABLE vendor_profiles
    ADD COLUMN shop_name_changed_at          TIMESTAMPTZ,
    ADD COLUMN contact_phone                 VARCHAR(20),
    ADD COLUMN contact_email                 VARCHAR(255),
    ADD COLUMN kyc_documents_meta_json       JSONB,
    ADD COLUMN kyc_biometric_consent         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN kyc_supplement_documents_json JSONB,
    ADD COLUMN kyc_supplement_deadline_at    TIMESTAMPTZ,
    ADD COLUMN kyc_manual_review_requested_at TIMESTAMPTZ,
    ADD COLUMN pickup_phone                  VARCHAR(20);

-- ===== 2. Index (ENTITY 13.1) =====
-- Tên shop không trùng, không phân biệt hoa thường (UC-VND-01, UC-VND-21). Dữ liệu mẫu đã kiểm không trùng
-- (hồ sơ KYC mẫu dùng "TravelGear Outdoor" thay cho "TravelGear").
CREATE UNIQUE INDEX uk_vendor_profiles_shop_name_lower ON vendor_profiles (lower(shop_name));
-- Hàng chờ KYC của Admin lọc theo trạng thái, sắp theo lúc nộp (UC-VND-04).
CREATE INDEX idx_vendor_profiles_kyc_status ON vendor_profiles (kyc_status, kyc_submitted_at);
-- Job hằng ngày tìm yêu cầu bổ sung quá hạn 7 ngày (UC-VND-04, LLD 10).
CREATE INDEX idx_vendor_profiles_supplement_deadline ON vendor_profiles (kyc_supplement_deadline_at)
    WHERE kyc_status = 'SUPPLEMENT_REQUESTED';

-- ===== 3. Hành động kiểm toán (D3) — giữ NGUYÊN mọi giá trị của V14 + V15, thêm 4 giá trị =====
ALTER TABLE system_logs DROP CONSTRAINT ck_system_logs_action_type;
ALTER TABLE system_logs ADD CONSTRAINT ck_system_logs_action_type CHECK (action_type IN (
    'LOGIN_SUCCESS', 'LOGIN_FAILED', 'ACCOUNT_LOCKED', 'ACCOUNT_SUSPENDED', 'ACCOUNT_REACTIVATED',
    'KYC_SUBMITTED', 'KYC_APPROVED', 'KYC_REJECTED', 'ORDER_STATUS_CHANGED', 'ESCROW_RELEASED',
    'PAYOUT_APPROVED', 'PAYOUT_REJECTED', 'REFUND_PROCESSED', 'REVIEW_HIDDEN', 'COMMISSION_UPDATED',
    'PERMISSION_GRANTED', 'PERMISSION_REVOKED', 'RBAC_ROLE_CHANGED', 'STAFF_INVITED', 'PII_ANONYMIZED',
    'ACCESS_DENIED', 'PRODUCT_MODERATED',
    'PASSWORD_CHANGED', 'PASSWORD_RESET', 'EMAIL_CHANGED', 'OAUTH_LINKED', 'OAUTH_UNLINKED', 'SESSION_REVOKED',
    'ACCOUNT_DELETION_REQUESTED', 'ACCOUNT_DELETION_CANCELLED',
    'BANNER_CHANGED',
    'KYC_SUPPLEMENT_REQUESTED', 'KYC_REQUEUED', 'KYC_DOCUMENT_VIEWED', 'PAYOUT_ACCOUNT_CHANGED'
));
