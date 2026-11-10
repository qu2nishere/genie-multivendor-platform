-- Nhóm 4 — Khuyến mãi, voucher, flash sale & Điểm
-- Bảng: vouchers, voucher_usages, user_vouchers, voucher_categories, voucher_products, voucher_excluded_categories, voucher_excluded_products, flash_sale_campaigns, flash_sale_items, user_coins, coin_transactions, daily_checkins
-- Nguồn: ENTITY.md → migration viết tay. KHÔNG sửa migration đã merge; thay đổi = V<n+1>__mo_ta.sql
-- PK: BIGINT GENERATED ALWAYS AS IDENTITY · tiền NUMERIC(19,2) · thời gian TIMESTAMPTZ · enum VARCHAR + CHECK
-- Đặt tên: idx_<bảng>_<cột>, uk_<bảng>_<ý nghĩa>, fk_<bảng>_<bảng đích>

-- TODO
