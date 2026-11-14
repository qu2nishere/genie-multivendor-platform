-- Nhóm 4 — Khuyến mãi, voucher, flash sale & Điểm
-- Bảng: vouchers, voucher_usages, user_vouchers, voucher_categories, voucher_products, voucher_excluded_categories, voucher_excluded_products, flash_sale_campaigns, flash_sale_items, user_coins, coin_transactions, daily_checkins
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- Voucher
CREATE TABLE vouchers (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value NUMERIC(19,2) NOT NULL,
    max_discount_cap NUMERIC(19,2),
    min_order_value NUMERIC(19,2) NOT NULL,
    usage_limit INT NOT NULL,
    usage_per_user INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    priority INT NOT NULL,
    vendor_id BIGINT,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    budget NUMERIC(19,2),
    total_discount_used NUMERIC(19,2) NOT NULL DEFAULT 0,
    notification_body TEXT,
    sponsor_type VARCHAR(30) NOT NULL,
    platform_share_percent NUMERIC(5,2),
    CONSTRAINT pk_vouchers PRIMARY KEY (id),
    CONSTRAINT ck_vouchers_scope CHECK (scope IN ('ALL', 'CATEGORY', 'PRODUCT')),
    CONSTRAINT ck_vouchers_type CHECK (type IN ('SHOP', 'PLATFORM', 'FREESHIP')),
    CONSTRAINT ck_vouchers_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREESHIP')),
    CONSTRAINT ck_vouchers_sponsor_type CHECK (sponsor_type IN ('PLATFORM', 'VENDOR', 'CO_SPONSOR'))
);

-- VoucherUsage
CREATE TABLE voucher_usages (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    voucher_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    checkout_session_id BIGINT NOT NULL,
    order_id BIGINT,
    used_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_voucher_usages PRIMARY KEY (id),
    CONSTRAINT uq_voucher_usage_session UNIQUE (voucher_id, checkout_session_id)
);

-- UserVoucher
CREATE TABLE user_vouchers (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    is_used BOOLEAN,
    CONSTRAINT pk_user_vouchers PRIMARY KEY (id),
    CONSTRAINT uk_user_vouchers_user_id_voucher_id UNIQUE (user_id, voucher_id)
);

-- VoucherCategory
CREATE TABLE voucher_categories (
    voucher_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_voucher_categories PRIMARY KEY (voucher_id, category_id)
);

-- VoucherProduct
CREATE TABLE voucher_products (
    voucher_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_voucher_products PRIMARY KEY (voucher_id, product_id)
);

-- VoucherExcludedCategory
CREATE TABLE voucher_excluded_categories (
    voucher_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_voucher_excluded_categories PRIMARY KEY (voucher_id, category_id)
);

-- VoucherExcludedProduct
CREATE TABLE voucher_excluded_products (
    voucher_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_voucher_excluded_products PRIMARY KEY (voucher_id, product_id)
);

-- FlashSaleCampaign
CREATE TABLE flash_sale_campaigns (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    vendor_id BIGINT,
    title VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    max_products INT NOT NULL DEFAULT 20,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_flash_sale_campaigns PRIMARY KEY (id),
    CONSTRAINT ck_flash_sale_campaigns_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'ENDED'))
);

-- FlashSaleItem
CREATE TABLE flash_sale_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    campaign_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    flash_price NUMERIC(15,2) NOT NULL,
    flash_stock INT NOT NULL,
    reserved_count INT DEFAULT 0 NOT NULL,
    sold_count INT DEFAULT 0 NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_flash_sale_items PRIMARY KEY (id),
    CONSTRAINT ck_flash_sale_items_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- UserCoin
CREATE TABLE user_coins (
    user_id BIGINT NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    balance_hash VARCHAR(255) NOT NULL,
    last_updated TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_coins PRIMARY KEY (user_id)
);

-- CoinTransaction
CREATE TABLE coin_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(50),
    label VARCHAR(200),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_coin_transactions PRIMARY KEY (id),
    CONSTRAINT ck_coin_transactions_type CHECK (type IN ('CHECK_IN', 'ORDER_CASHBACK', 'REVIEW_BONUS', 'USED_AT_CHECKOUT', 'ORDER_REFUND', 'VOUCHER_REDEEM'))
);

-- DailyCheckin
CREATE TABLE daily_checkins (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    customer_id BIGINT NOT NULL UNIQUE,
    last_checkin_at TIMESTAMPTZ NOT NULL,
    streak_count INT NOT NULL,
    CONSTRAINT pk_daily_checkins PRIMARY KEY (id),
    CONSTRAINT uk_daily_checkin_customer UNIQUE (customer_id)
);

-- ===== Khóa ngoại =====
ALTER TABLE vouchers ADD CONSTRAINT fk_vouchers_users FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE voucher_usages ADD CONSTRAINT fk_voucher_usages_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id);
ALTER TABLE voucher_usages ADD CONSTRAINT fk_voucher_usages_users FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_vouchers ADD CONSTRAINT fk_user_vouchers_users FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_vouchers ADD CONSTRAINT fk_user_vouchers_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE;
ALTER TABLE voucher_categories ADD CONSTRAINT fk_voucher_categories_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE;
ALTER TABLE voucher_categories ADD CONSTRAINT fk_voucher_categories_categories FOREIGN KEY (category_id) REFERENCES categories(id);
ALTER TABLE voucher_products ADD CONSTRAINT fk_voucher_products_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE;
ALTER TABLE voucher_products ADD CONSTRAINT fk_voucher_products_products FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE voucher_excluded_categories ADD CONSTRAINT fk_voucher_excluded_categories_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE;
ALTER TABLE voucher_excluded_categories ADD CONSTRAINT fk_voucher_excluded_categories_categories FOREIGN KEY (category_id) REFERENCES categories(id);
ALTER TABLE voucher_excluded_products ADD CONSTRAINT fk_voucher_excluded_products_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE;
ALTER TABLE voucher_excluded_products ADD CONSTRAINT fk_voucher_excluded_products_products FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE flash_sale_campaigns ADD CONSTRAINT fk_flash_sale_campaigns_users FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE flash_sale_items ADD CONSTRAINT fk_flash_sale_items_flash_sale_campaigns FOREIGN KEY (campaign_id) REFERENCES flash_sale_campaigns(id) ON DELETE CASCADE;
ALTER TABLE flash_sale_items ADD CONSTRAINT fk_flash_sale_items_products FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE user_coins ADD CONSTRAINT fk_user_coins_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE coin_transactions ADD CONSTRAINT fk_coin_transactions_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE daily_checkins ADD CONSTRAINT fk_daily_checkins_users FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE;
-- FK nợ: voucher_usages.checkout_session_id -> checkout_sessions(id) (tạo ở V5)
-- FK nợ: voucher_usages.order_id -> orders(id) (tạo ở V5)
