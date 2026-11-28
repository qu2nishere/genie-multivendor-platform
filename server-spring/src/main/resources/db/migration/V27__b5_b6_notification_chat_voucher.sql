-- V27 — migration chung của 3 plan 2026-11-26 (Nguyễn Ngọc Thiện; plan/2026-11-26/server-spring/README mục 5):
--   [1] Thông báo thời gian thực + Thông báo toàn sàn (UC-NOT-01, UC-NOT-05) — plan_thong_bao_realtime.md N01, D2, D6
--   [2] Chat Khách – Shop + tự trả lời (UC-SOC-01, 02, UC-VND-06) — plan_chat_khach_shop.md D11
--   [3] Voucher shop / sàn / Kho voucher (UC-PRO-01, 02, 03) — plan_voucher_b5.md D16
-- Chỉ THÊM cột / index / giá trị CHECK (tương thích ngược, NFR-12); ENTITY.md v1.6 sửa trước (mục 10.2, 11.2, Phụ lục A, 13.1).
-- Merge xong không sửa file này — cần thêm thì số kế tiếp theo thứ tự merge.

-- ===== (a) Thông báo toàn sàn: đánh dấu đã phát (fan-out on read) =====
-- NULL = đang hẹn giờ (cron phát rồi mới đặt); hộp thư người dùng chỉ đọc dòng đã phát.
ALTER TABLE platform_notifications ADD COLUMN dispatched_at TIMESTAMPTZ;
-- Dữ liệu cũ (nếu có): phát ngay hoặc đã tới giờ hẹn → coi như đã phát
UPDATE platform_notifications SET dispatched_at = COALESCE(send_at, created_at)
WHERE send_at IS NULL OR send_at <= now();
-- Hộp thư: thông báo sàn đã phát, mới nhất trước
CREATE INDEX idx_platform_notifications_dispatched ON platform_notifications (dispatched_at DESC)
    WHERE dispatched_at IS NOT NULL;
-- Cron phát hẹn giờ: chỉ dòng chưa phát
CREATE INDEX idx_platform_notifications_pending ON platform_notifications (send_at)
    WHERE dispatched_at IS NULL;
-- Đếm lượt đọc theo thông báo (readCount @Formula) — PK (user_id, platform_notification_id) không phục vụ được
CREATE INDEX idx_user_platform_notification_reads_notification ON user_platform_notification_reads (platform_notification_id);

-- ===== (b) Hộp thư cá nhân: loại thông báo tin nhắn chat =====
ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (type IN (
    'ORDER_STATUS_CHANGED', 'KYC_APPROVED', 'KYC_REJECTED', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'ESCROW_RELEASED',
    'WALLET_CREDITED', 'SYSTEM_ALERT', 'REFUND_PROCESSED', 'NEW_PLATFORM_VOUCHER', 'NEW_VENDOR_VOUCHER',
    'NEW_ORDER_FOR_VENDOR', 'FLASH_SALE_REMINDER', 'NEW_DEVICE_LOGIN', 'PRODUCT_APPROVED', 'PRODUCT_REJECTED',
    'CHAT_MESSAGE'
));
-- Một thông báo chat CHƯA ĐỌC cho mỗi (người nhận, hội thoại) — gộp bằng INSERT … ON CONFLICT (UC-SOC-01)
CREATE UNIQUE INDEX uk_notifications_chat_unread ON notifications (user_id, reference_id)
    WHERE type = 'CHAT_MESSAGE' AND is_read = FALSE;
-- Chuông Admin Console: chưa đọc + mới nhất
CREATE INDEX idx_admin_notifications_read_created ON admin_notifications (is_read, created_at DESC);

-- ===== (c) Chat: tin tự động của shop + hộp thư hai phía =====
ALTER TABLE messages ADD COLUMN is_auto BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_conversations_customer_last ON conversations (customer_id, last_message_at DESC);
CREATE INDEX idx_conversations_vendor_last ON conversations (vendor_id, last_message_at DESC);

-- ===== (d) Voucher: danh sách quản lý theo shop / theo loại, voucher công khai =====
CREATE INDEX idx_vouchers_vendor_valid_to ON vouchers (vendor_id, valid_to DESC) WHERE vendor_id IS NOT NULL;
CREATE INDEX idx_vouchers_type_valid_to ON vouchers (type, valid_to DESC);

-- ===== (e) Nhật ký kiểm toán: thông báo sàn + voucher sàn =====
-- Chép NGUYÊN danh sách của V24 (V25 không đổi ràng buộc này) rồi thêm 7 giá trị. Migration sau sửa lại phải chép đủ.
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
    'KYC_SUPPLEMENT_REQUESTED', 'KYC_REQUEUED', 'KYC_DOCUMENT_VIEWED', 'PAYOUT_ACCOUNT_CHANGED',
    'BANNER_CREATED', 'BANNER_UPDATED', 'BANNER_ACTIVATED', 'BANNER_DEACTIVATED', 'BANNER_DELETED',
    'PLATFORM_NOTIFICATION_PUBLISHED', 'PLATFORM_NOTIFICATION_UPDATED', 'PLATFORM_NOTIFICATION_DELETED',
    'VOUCHER_CREATED', 'VOUCHER_UPDATED', 'VOUCHER_DISABLED', 'VOUCHER_DELETED'
));
