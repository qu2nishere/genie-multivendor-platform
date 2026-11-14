package com.genie.notification.inbox.domain;

/**
 * Enum NotificationType — ENTITY.md Phụ lục A. ← Notification.type (mở rộng: PRICE_DROP_ALERT, B2B_RFQ_QUOTED)
 */
public enum NotificationType {                                                                 // ← Notification.type (mở rộng: PRICE_DROP_ALERT, B2B_RFQ_QUOTED)
    ORDER_STATUS_CHANGED, KYC_APPROVED, KYC_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, ESCROW_RELEASED,
    WALLET_CREDITED, SYSTEM_ALERT, REFUND_PROCESSED, NEW_PLATFORM_VOUCHER, NEW_VENDOR_VOUCHER, NEW_ORDER_FOR_VENDOR,
    FLASH_SALE_REMINDER, NEW_DEVICE_LOGIN, PRODUCT_APPROVED, PRODUCT_REJECTED   // cảnh báo đăng nhập thiết bị lạ (UC-AUTH-02), kết quả duyệt SP (UC-CAT-12/13)
}
