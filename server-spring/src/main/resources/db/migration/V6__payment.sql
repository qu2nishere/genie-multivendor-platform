-- Nhóm 6 — Thanh toán & ký quỹ
-- Bảng: payment_transactions, payment_refunds, escrow_ledgers, vendor_wallets, wallet_transactions, withdrawal_requests, financial_ledger, cod_remittances
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>

-- TODO
