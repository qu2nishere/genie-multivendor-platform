-- Nhóm 5 — Đơn hàng
-- Bảng: checkout_sessions, orders, order_items, order_discounts, stock_reservations
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- CheckoutSession
CREATE TABLE checkout_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    cart_snapshot_json jsonb,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_checkout_sessions PRIMARY KEY (id),
    CONSTRAINT ck_checkout_sessions_status CHECK (status IN ('INITIATED', 'PAYMENT_PENDING', 'COMPLETED', 'FAILED', 'EXPIRED'))
);

-- Order
CREATE TABLE orders (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_code VARCHAR(32) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    checkout_session_id BIGINT,
    shipping_address_snapshot_json jsonb NOT NULL,
    sub_total NUMERIC(19,2) NOT NULL,
    shipping_fee NUMERIC(19,2) NOT NULL,
    discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    platform_discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    shipping_discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(19,2) NOT NULL,
    refunded_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(30),
    payment_status VARCHAR(30),
    funding_status VARCHAR(30) NOT NULL,
    payment_confirmed_at TIMESTAMPTZ,
    funded_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    customer_confirmed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL,
    ghn_order_code VARCHAR(100),
    shipping_expected_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    is_reviewed BOOLEAN,
    buyer_note VARCHAR(500),
    coin_used_count INT NOT NULL DEFAULT 0,
    coin_discount_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT ck_orders_payment_method CHECK (payment_method IN ('VNPAY', 'VIETQR', 'COD')),
    CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('PENDING', 'REQUIRES_ACTION', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'CANCELLED')),
    CONSTRAINT ck_orders_funding_status CHECK (funding_status IN ('UNFUNDED', 'PENDING', 'FUNDED', 'FAILED')),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CONFIRMED', 'PREPARING', 'READY_FOR_PICKUP', 'DELIVERING', 'DELIVERED', 'DELIVERY_FAILED', 'RETURNING', 'RETURNED_TO_VENDOR', 'COMPLETED', 'RETURN_REQUESTED', 'RETURN_APPROVED', 'RETURN_REJECTED', 'PARTIALLY_REFUNDED', 'REFUNDED', 'CANCELLED', 'CANCELLED_BY_VENDOR', 'AUTO_CANCELLED'))
);

-- OrderItem
CREATE TABLE order_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(255),
    sku_snapshot VARCHAR(100),
    quantity INT NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    line_total NUMERIC(19,2) NOT NULL,
    prorated_discount NUMERIC(19,2) NOT NULL DEFAULT 0,
    platform_discount NUMERIC(19,2) NOT NULL DEFAULT 0,
    shop_discount NUMERIC(19,2) NOT NULL DEFAULT 0,
    gross_amount NUMERIC(19,2) NOT NULL,
    commission_rate NUMERIC(5,4) NOT NULL,
    commission_amount NUMERIC(19,2) NOT NULL,
    vendor_receivable NUMERIC(19,2) NOT NULL,
    coin_discount NUMERIC(15,2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_order_items PRIMARY KEY (id)
);

-- OrderDiscount
CREATE TABLE order_discounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_id BIGINT NOT NULL,
    voucher_id BIGINT,
    voucher_code_snapshot VARCHAR(50),
    discount_amount NUMERIC(19,2) NOT NULL,
    voucher_type VARCHAR(20),
    funded_by VARCHAR(20),
    CONSTRAINT pk_order_discounts PRIMARY KEY (id),
    CONSTRAINT ck_order_discounts_voucher_type CHECK (voucher_type IN ('SHOP', 'PLATFORM', 'FREESHIP')),
    CONSTRAINT ck_order_discounts_funded_by CHECK (funded_by IN ('PLATFORM', 'VENDOR', 'CO_SPONSOR'))
);

-- StockReservation
CREATE TABLE stock_reservations (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    product_variant_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    reserved_quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ,
    CONSTRAINT pk_stock_reservations PRIMARY KEY (id),
    CONSTRAINT ck_stock_reservations_status CHECK (status IN ('ACTIVE', 'CONFIRMED', 'EXPIRED', 'RELEASED'))
);

-- ===== Khóa ngoại =====
ALTER TABLE voucher_usages ADD CONSTRAINT fk_voucher_usages_checkout_sessions FOREIGN KEY (checkout_session_id) REFERENCES checkout_sessions(id);
ALTER TABLE voucher_usages ADD CONSTRAINT fk_voucher_usages_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
ALTER TABLE checkout_sessions ADD CONSTRAINT fk_checkout_sessions_users FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_users FOREIGN KEY (customer_id) REFERENCES users(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_vendor_id FOREIGN KEY (vendor_id) REFERENCES users(id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_checkout_sessions FOREIGN KEY (checkout_session_id) REFERENCES checkout_sessions(id) ON DELETE SET NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product_variants FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);
ALTER TABLE order_discounts ADD CONSTRAINT fk_order_discounts_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
ALTER TABLE order_discounts ADD CONSTRAINT fk_order_discounts_vouchers FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL;
ALTER TABLE stock_reservations ADD CONSTRAINT fk_stock_reservations_product_variants FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);
ALTER TABLE stock_reservations ADD CONSTRAINT fk_stock_reservations_orders FOREIGN KEY (order_id) REFERENCES orders(id);


-- ===== Index đường nóng (ENTITY.md 13.1, 5.5 d) =====
CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at DESC);               -- UC-ORD-09 lịch sử đơn
CREATE INDEX idx_orders_vendor_status_created ON orders (vendor_id, status, created_at DESC);    -- kênh người bán
CREATE INDEX idx_orders_delivered ON orders (status, delivered_at) WHERE status = 'DELIVERED';   -- UC-ORD-12 cron tự hoàn tất
CREATE INDEX idx_stock_reservations_active_expires ON stock_reservations (status, expires_at) WHERE status = 'ACTIVE';  -- UC-ORD-08 sweeper
-- orders.order_code UNIQUE: đã có ở cột (unique = true)
