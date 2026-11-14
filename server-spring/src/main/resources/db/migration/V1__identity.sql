-- Nhóm 1 — Người dùng & định danh
-- Bảng: users, customer_profiles, addresses, vendor_profiles, admin_profiles, kyc_results, refresh_tokens, staff_permissions
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>

CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;


-- User
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    failed_login_attempts INT DEFAULT 0 NOT NULL,
    locked_until TIMESTAMPTZ,
    suspended_until TIMESTAMPTZ,
    suspension_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    oauth_provider VARCHAR(30),
    oauth_provider_id VARCHAR(255),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_oauth UNIQUE (oauth_provider, oauth_provider_id),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'VENDOR', 'STAFF', 'ADMIN')),
    CONSTRAINT ck_users_status CHECK (status IN ('UNVERIFIED', 'ACTIVE', 'LOCKED', 'SUSPENDED', 'ANONYMIZED', 'DELETED'))
);

-- CustomerProfile
CREATE TABLE customer_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    avatar_url VARCHAR(500),
    gender VARCHAR(50),
    date_of_birth DATE,
    bio TEXT,
    preferred_language VARCHAR(5),
    default_address_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    anonymized_at TIMESTAMPTZ,
    CONSTRAINT pk_customer_profiles PRIMARY KEY (id),
    CONSTRAINT ck_customer_profiles_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'))
);

-- Address
CREATE TABLE addresses (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    ward_code VARCHAR(50) NOT NULL,
    district VARCHAR(100) NOT NULL,
    district_id INT NOT NULL,
    province VARCHAR(100) NOT NULL,
    province_id INT NOT NULL,
    is_default BOOLEAN,
    label VARCHAR(20) NOT NULL DEFAULT 'HOME',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_addresses PRIMARY KEY (id),
    CONSTRAINT ck_addresses_label CHECK (label IN ('HOME', 'OFFICE'))
);

-- RefreshToken
CREATE TABLE refresh_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

-- VendorProfile
CREATE TABLE vendor_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    shop_name VARCHAR(255) NOT NULL,
    category_id BIGINT,
    business_type VARCHAR(50),
    identity_code VARCHAR(255),
    tax_code VARCHAR(255),
    representative VARCHAR(255),
    cover_url VARCHAR(255),
    shop_description TEXT,
    logo_url VARCHAR(500),
    id_front_url VARCHAR(500),
    id_back_url VARCHAR(500),
    license_url VARCHAR(500),
    selfie_url VARCHAR(500),
    kyc_status VARCHAR(50) NOT NULL,
    kyc_rejection_reason TEXT,
    kyc_verification_method VARCHAR(50),
    kyc_verified_by BIGINT,
    commission_rate NUMERIC(5,4),
    kyc_submitted_at TIMESTAMPTZ,
    kyc_verified_at TIMESTAMPTZ,
    pickup_province_id INT,
    pickup_province VARCHAR(100),
    pickup_district_id INT,
    pickup_district VARCHAR(100),
    pickup_ward_code VARCHAR(50),
    pickup_ward VARCHAR(100),
    pickup_address_line VARCHAR(500),
    ghn_shop_id INT,
    payout_bank_name VARCHAR(100),
    payout_bank_account_number VARCHAR(50),
    payout_bank_account_name VARCHAR(255),
    payout_account_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    is_mall BOOLEAN NOT NULL DEFAULT FALSE,
    rating_avg NUMERIC(2,1) NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    follower_count INT NOT NULL DEFAULT 0,
    active_product_count INT NOT NULL DEFAULT 0,
    chat_response_rate NUMERIC(5,2),
    avg_response_minutes INT,
    pre_order_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    auto_reply_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    auto_reply_greeting TEXT,
    auto_reply_offline TEXT,
    vacation_starts_at TIMESTAMPTZ,
    vacation_ends_at TIMESTAMPTZ,
    vacation_message VARCHAR(500),
    CONSTRAINT pk_vendor_profiles PRIMARY KEY (id),
    CONSTRAINT ck_vendor_profiles_business_type CHECK (business_type IN ('INDIVIDUAL', 'HOUSEHOLD', 'BUSINESS')),
    CONSTRAINT ck_vendor_profiles_kyc_status CHECK (kyc_status IN ('NOT_SUBMITTED', 'PENDING', 'AI_PROCESSING', 'MANUAL_REVIEW', 'VERIFIED', 'REJECTED', 'SUPPLEMENT_REQUESTED')),
    CONSTRAINT ck_vendor_profiles_kyc_verification_method CHECK (kyc_verification_method IN ('OCR_AUTO', 'MANUAL_STAFF', 'BIOMETRIC_FACIAL'))
);

-- KycResult
CREATE TABLE kyc_results (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    vendor_profile_id BIGINT NOT NULL UNIQUE,
    extracted_id_number VARCHAR(20),
    extracted_full_name VARCHAR(255),
    extracted_dob VARCHAR(20),
    extracted_gender VARCHAR(10),
    extracted_nationality VARCHAR(100),
    extracted_address TEXT,
    extracted_issue_date VARCHAR(20),
    extracted_expiry_date VARCHAR(20),
    extracted_issuer VARCHAR(255),
    qr_code_detected BOOLEAN,
    mrz_valid BOOLEAN,
    mrz_qr_match BOOLEAN,
    cccd_number_valid BOOLEAN,
    expiry_rule_valid BOOLEAN,
    duplicate_id_found BOOLEAN,
    face_match_result BOOLEAN,
    face_match_score NUMERIC(5,4),
    liveness_score NUMERIC(5,4),
    face_duplicate_found BOOLEAN,
    risk_score INT,
    risk_signals_json TEXT,
    sampled_for_review BOOLEAN NOT NULL DEFAULT FALSE,
    errors TEXT,
    overall_status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_kyc_results PRIMARY KEY (id),
    CONSTRAINT ck_kyc_results_overall_status CHECK (overall_status IN ('PASS', 'PARTIAL', 'FAIL'))
);

-- AdminProfile
CREATE TABLE admin_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    is_active_moderator BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    staff_title VARCHAR(30),
    CONSTRAINT pk_admin_profiles PRIMARY KEY (id),
    CONSTRAINT ck_admin_profiles_staff_title CHECK (staff_title IN ('MODERATOR', 'CS_SPECIALIST', 'FINANCE_OFFICER'))
);

-- StaffPermission
CREATE TABLE staff_permissions (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    staff_user_id BIGINT NOT NULL,
    permission VARCHAR(50) NOT NULL,
    granted_by_admin_id BIGINT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT pk_staff_permissions PRIMARY KEY (id),
    CONSTRAINT ck_staff_permissions_permission CHECK (permission IN ('VIEW_DASHBOARD', 'REVIEW_KYC', 'MANAGE_CATALOG', 'MODERATE_REVIEW', 'HANDLE_DISPUTE', 'PROCESS_PAYOUT', 'UPDATE_COMMISSION', 'VIEW_FINANCE', 'MANAGE_VOUCHER', 'MANAGE_PLATFORM_NOTIFICATION', 'VIEW_AUDIT_LOG', 'MANAGE_USER', 'MANAGE_RBAC'))
);

-- ===== Khóa ngoại =====
ALTER TABLE customer_profiles ADD CONSTRAINT fk_customer_profiles_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE addresses ADD CONSTRAINT fk_addresses_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE vendor_profiles ADD CONSTRAINT fk_vendor_profiles_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE kyc_results ADD CONSTRAINT fk_kyc_results_vendor_profiles FOREIGN KEY (vendor_profile_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE;
ALTER TABLE admin_profiles ADD CONSTRAINT fk_admin_profiles_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE staff_permissions ADD CONSTRAINT fk_staff_permissions_users FOREIGN KEY (staff_user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE staff_permissions ADD CONSTRAINT fk_staff_permissions_granted_by_admin_id FOREIGN KEY (granted_by_admin_id) REFERENCES users(id);
-- FK nợ: vendor_profiles.category_id -> categories(id) (tạo ở V2)
