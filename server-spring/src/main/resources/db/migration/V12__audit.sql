-- Nhóm 12 — Nhật ký hệ thống
-- Bảng: system_logs
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- SystemLog
CREATE TABLE system_logs (
    id VARCHAR(36) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    user_id BIGINT,
    user_email VARCHAR(255) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    status VARCHAR(20) NOT NULL,
    user_agent VARCHAR(500),
    CONSTRAINT pk_system_logs PRIMARY KEY (id),
    CONSTRAINT ck_system_logs_action_type CHECK (action_type IN ('LOGIN_SUCCESS', 'LOGIN_FAILED', 'ACCOUNT_LOCKED', 'ACCOUNT_SUSPENDED', 'ACCOUNT_REACTIVATED', 'KYC_SUBMITTED', 'KYC_APPROVED', 'KYC_REJECTED', 'ORDER_STATUS_CHANGED', 'ESCROW_RELEASED', 'PAYOUT_APPROVED', 'PAYOUT_REJECTED', 'REFUND_PROCESSED', 'REVIEW_HIDDEN', 'COMMISSION_UPDATED', 'PERMISSION_GRANTED', 'PERMISSION_REVOKED', 'RBAC_ROLE_CHANGED', 'STAFF_INVITED', 'PII_ANONYMIZED', 'ACCESS_DENIED', 'PRODUCT_MODERATED'))
);

-- ===== Khóa ngoại =====



-- ===== Index đường nóng (ENTITY.md 12.1, 13.1) =====
CREATE INDEX idx_system_logs_created_action ON system_logs (created_at, action_type);  -- UC-ADM-03 tra cứu kiểm toán
-- Bảng chỉ ghi thêm (TECH_STACK 7.2): ứng dụng không UPDATE / DELETE; id UUID sinh ở @PrePersist
