-- V19 — loại giao dịch Điểm hết hạn (UC-PRO-09 ràng buộc "lô quá 12 tháng bị trừ"; plan 2026-11-16 Điểm Genie D5, P06.02;
-- ENTITY Phụ lục A). Chỉ nới CHECK (tương thích ngược — NFR-12): giữ 6 giá trị của V4 + thêm EXPIRED.
ALTER TABLE coin_transactions DROP CONSTRAINT ck_coin_transactions_type;
ALTER TABLE coin_transactions ADD CONSTRAINT ck_coin_transactions_type CHECK (type IN (
    'CHECK_IN', 'ORDER_CASHBACK', 'REVIEW_BONUS', 'USED_AT_CHECKOUT', 'ORDER_REFUND', 'VOUCHER_REDEEM',
    'EXPIRED'
));
