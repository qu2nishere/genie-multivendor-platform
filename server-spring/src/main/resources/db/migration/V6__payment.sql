-- Nhóm 6 — Thanh toán & ký quỹ
-- Bảng: payment_transactions, payment_refunds, escrow_ledgers, vendor_wallets, wallet_transactions, withdrawal_requests, financial_ledger, cod_remittances
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>


-- PaymentTransaction
CREATE TABLE payment_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_id BIGINT,
    checkout_session_id BIGINT,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    amount NUMERIC(19,2) NOT NULL,
    refunded_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    gateway_transaction_id VARCHAR(255),
    payment_method VARCHAR(30),
    status VARCHAR(50) NOT NULL,
    webhook_payload TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT ck_payment_transactions_payment_method CHECK (payment_method IN ('VNPAY', 'VIETQR', 'COD')),
    CONSTRAINT ck_payment_transactions_status CHECK (status IN ('PENDING', 'REQUIRES_ACTION', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'CANCELLED'))
);

-- PaymentRefund
CREATE TABLE payment_refunds (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    payment_transaction_id BIGINT NOT NULL,
    refund_request_id BIGINT NOT NULL,
    gateway_refund_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT pk_payment_refunds PRIMARY KEY (id),
    CONSTRAINT ck_payment_refunds_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

-- EscrowLedger
CREATE TABLE escrow_ledgers (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    vendor_id BIGINT NOT NULL,
    gross_amount NUMERIC(19,2) NOT NULL,
    commission_fee NUMERIC(19,2) NOT NULL,
    platform_shipping_fee NUMERIC(19,2) NOT NULL,
    shop_shipping_subsidy NUMERIC(19,2) NOT NULL,
    net_amount NUMERIC(19,2) NOT NULL,
    refunded_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    funding_source VARCHAR(30),
    gateway_funded_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    cod_funded_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    funded_at TIMESTAMPTZ,
    cod_remitted_at TIMESTAMPTZ,
    release_eligible_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_escrow_ledgers PRIMARY KEY (id),
    CONSTRAINT ck_escrow_ledgers_funding_source CHECK (funding_source IN ('ONLINE_GATEWAY', 'COD_CASH')),
    CONSTRAINT ck_escrow_ledgers_status CHECK (status IN ('PENDING_FUNDING', 'HELD', 'FROZEN', 'RELEASED', 'REFUNDED', 'DISPUTED'))
);

-- VendorWallet
CREATE TABLE vendor_wallets (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    vendor_id BIGINT NOT NULL UNIQUE,
    balance NUMERIC(19,2) NOT NULL DEFAULT 0,
    pending_withdrawal NUMERIC(19,2) NOT NULL DEFAULT 0,
    balance_hash VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_vendor_wallets PRIMARY KEY (id)
);

-- WalletTransaction
CREATE TABLE wallet_transactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    wallet_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    balance_before NUMERIC(19,2) NOT NULL,
    balance_after NUMERIC(19,2) NOT NULL,
    reference_id VARCHAR(255),
    reference_type VARCHAR(30),
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_wallet_transactions PRIMARY KEY (id),
    CONSTRAINT ck_wallet_transactions_type CHECK (type IN ('CREDIT_ESCROW_RELEASE', 'DEBIT_REFUND', 'DEBIT_WITHDRAWAL', 'CREDIT_ADJUSTMENT', 'DEBIT_ADJUSTMENT')),
    CONSTRAINT ck_wallet_transactions_reference_type CHECK (reference_type IN ('ORDER', 'WITHDRAWAL', 'REFUND', 'ADJUSTMENT'))
);

-- WithdrawalRequest
CREATE TABLE withdrawal_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    wallet_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    bank_account_name VARCHAR(255) NOT NULL,
    bank_account_number VARCHAR(50) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    rejection_reason VARCHAR(500),
    processed_by_admin_id BIGINT,
    wallet_transaction_id BIGINT,
    requested_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    CONSTRAINT pk_withdrawal_requests PRIMARY KEY (id),
    CONSTRAINT ck_withdrawal_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED'))
);

-- FinancialLedger
CREATE TABLE financial_ledger (
    seq BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    order_id BIGINT,
    vendor_id BIGINT,
    account VARCHAR(100) NOT NULL,
    debit NUMERIC(19,2) NOT NULL,
    credit NUMERIC(19,2) NOT NULL,
    balance_after NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_financial_ledger PRIMARY KEY (seq),
    CONSTRAINT ck_financial_ledger_event_type CHECK (event_type IN ('ESCROW_IN', 'COMMISSION', 'PLATFORM_VOUCHER', 'FREESHIP_COST', 'COIN_COST', 'ESCROW_RELEASE', 'PAYOUT_ACCRUE', 'PAYOUT_SETTLE', 'REFUND', 'GATEWAY_FEE'))
);

-- CodRemittance
CREATE TABLE cod_remittances (
    id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    remittance_code VARCHAR(30) NOT NULL UNIQUE,
    carrier_type VARCHAR(20) NOT NULL,
    received_amount NUMERIC(15,2) NOT NULL,
    bank_reference VARCHAR(100) NOT NULL UNIQUE,
    received_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    statement_file_name VARCHAR(255),
    lines_json jsonb,
    reconciled_by_staff_id BIGINT,
    reconciled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_cod_remittances PRIMARY KEY (id),
    CONSTRAINT ck_cod_remittances_carrier_type CHECK (carrier_type IN ('GHN', 'GHTK', 'VIETTEL_POST', 'JNT_EXPRESS', 'GENIE_EXPRESS')),
    CONSTRAINT ck_cod_remittances_status CHECK (status IN ('WAITING', 'DONE'))
);

-- ===== Khóa ngoại =====
ALTER TABLE payment_transactions ADD CONSTRAINT fk_payment_transactions_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
ALTER TABLE payment_transactions ADD CONSTRAINT fk_payment_transactions_checkout_sessions FOREIGN KEY (checkout_session_id) REFERENCES checkout_sessions(id) ON DELETE SET NULL;
ALTER TABLE payment_refunds ADD CONSTRAINT fk_payment_refunds_payment_transactions FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id);
ALTER TABLE escrow_ledgers ADD CONSTRAINT fk_escrow_ledgers_orders FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE escrow_ledgers ADD CONSTRAINT fk_escrow_ledgers_users FOREIGN KEY (vendor_id) REFERENCES users(id);
ALTER TABLE vendor_wallets ADD CONSTRAINT fk_vendor_wallets_users FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE wallet_transactions ADD CONSTRAINT fk_wallet_transactions_vendor_wallets FOREIGN KEY (wallet_id) REFERENCES vendor_wallets(id) ON DELETE CASCADE;
ALTER TABLE withdrawal_requests ADD CONSTRAINT fk_withdrawal_requests_wallet_transactions FOREIGN KEY (wallet_transaction_id) REFERENCES wallet_transactions(id) ON DELETE SET NULL;
ALTER TABLE financial_ledger ADD CONSTRAINT fk_financial_ledger_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL;
ALTER TABLE financial_ledger ADD CONSTRAINT fk_financial_ledger_users FOREIGN KEY (vendor_id) REFERENCES users(id) ON DELETE SET NULL;
-- FK nợ: payment_refunds.refund_request_id -> refund_requests(id) (tạo ở V8)

-- ===== Bổ sung viết tay (ENTITY.md 6.1 b, 13.1) =====
-- Một giao dịch gắn với đúng MỘT trong hai: đơn lẻ hoặc phiên thanh toán gộp (phép XOR)
ALTER TABLE payment_transactions ADD CONSTRAINT ck_payment_transactions_target
    CHECK ((order_id IS NULL) <> (checkout_session_id IS NULL));
-- IPN VNPay / webhook SePay idempotent: mỗi mã giao dịch cổng chỉ ghi một lần (NULL với COD — UNIQUE cho phép nhiều NULL)
CREATE UNIQUE INDEX uk_payment_transactions_gateway_tx ON payment_transactions (gateway_transaction_id);
-- payment_transactions.idempotency_key UNIQUE: đã có ở cột
-- UC-PAY-07 cron giải ngân
CREATE INDEX idx_escrow_ledgers_held_release ON escrow_ledgers (status, release_eligible_at) WHERE status = 'HELD';
