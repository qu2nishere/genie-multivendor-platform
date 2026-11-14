-- Nhóm 10 — Thông báo & nội dung sàn
-- Bảng: notifications, platform_notifications, user_platform_notification_reads, home_banners, admin_notifications
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- Notification
CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    image_url VARCHAR(500),
    reference_id VARCHAR(255),
    reference_type VARCHAR(50),
    metadata_json jsonb,
    channel VARCHAR(50) NOT NULL,
    delivery_status VARCHAR(50) NOT NULL,
    retry_count INT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT ck_notifications_type CHECK (type IN ('ORDER_STATUS_CHANGED', 'KYC_APPROVED', 'KYC_REJECTED', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'ESCROW_RELEASED', 'WALLET_CREDITED', 'SYSTEM_ALERT', 'REFUND_PROCESSED', 'NEW_PLATFORM_VOUCHER', 'NEW_VENDOR_VOUCHER', 'NEW_ORDER_FOR_VENDOR', 'FLASH_SALE_REMINDER', 'NEW_DEVICE_LOGIN', 'PRODUCT_APPROVED', 'PRODUCT_REJECTED')),
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT ck_notifications_delivery_status CHECK (delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED'))
);

-- PlatformNotification
CREATE TABLE platform_notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    target VARCHAR(30) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    read_count BIGINT,
    reach_count BIGINT NOT NULL DEFAULT 0,
    send_at TIMESTAMPTZ,
    recipient_user_ids_json jsonb,
    CONSTRAINT pk_platform_notifications PRIMARY KEY (id),
    CONSTRAINT ck_platform_notifications_target CHECK (target IN ('ALL', 'CUSTOMER', 'VENDOR', 'SPECIFIC_USERS'))
);

-- UserPlatformNotificationRead
CREATE TABLE user_platform_notification_reads (
    user_id BIGINT NOT NULL,
    platform_notification_id BIGINT NOT NULL,
    read_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_platform_notification_reads PRIMARY KEY (user_id, platform_notification_id)
);

-- HomeBanner
CREATE TABLE home_banners (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    title VARCHAR(200) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    mobile_image_url VARCHAR(500),
    target_url VARCHAR(500) NOT NULL,
    position VARCHAR(30) NOT NULL,
    category_id BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    target_audience_json jsonb,
    created_by_admin_id BIGINT NOT NULL,
    impression_count BIGINT NOT NULL DEFAULT 0,
    click_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_home_banners PRIMARY KEY (id),
    CONSTRAINT ck_home_banners_position CHECK (position IN ('HOME_TOP_CAROUSEL', 'HOME_TOP_SIDE', 'HOME_MIDDLE', 'CATEGORY_TOP', 'FLASH_SALE_TOP', 'POPUP'))
);
CREATE INDEX idx_banner_position_time ON home_banners (position, is_active, starts_at, ends_at);

-- AdminNotification
CREATE TABLE admin_notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    reference_url VARCHAR(500),
    image_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_admin_notifications PRIMARY KEY (id),
    CONSTRAINT ck_admin_notifications_type CHECK (type IN ('SYSTEM_ALERT', 'NEW_ORDER', 'USER_APPROVAL', 'WITHDRAW_REQUEST', 'ESCROW_DISCREPANCY'))
);

-- ===== Khóa ngoại =====
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_platform_notification_reads ADD CONSTRAINT fk_user_platform_notification_reads_users FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_platform_notification_reads ADD CONSTRAINT fk_user_platform_notification_reads_platform_notifications FOREIGN KEY (platform_notification_id) REFERENCES platform_notifications(id) ON DELETE CASCADE;
ALTER TABLE home_banners ADD CONSTRAINT fk_home_banners_categories FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;


-- ===== Index đường nóng (ENTITY.md 13.1) =====
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, is_read, created_at DESC);  -- UC-NOT-01 hộp thông báo
-- home_banners.created_by_admin_id: dữ liệu kiểm toán, KHÔNG đặt FK (ENTITY.md 10.4)
