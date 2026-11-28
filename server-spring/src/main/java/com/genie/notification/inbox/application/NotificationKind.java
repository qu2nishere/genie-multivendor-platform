// Loại thông báo cá nhân mà module khác được gửi — bản công khai của NotificationType (plan 2026-11-26 thông báo D10)
package com.genie.notification.inbox.application;

import com.genie.notification.inbox.domain.NotificationType;

/**
 * {@code ModuleBoundaryTest} cấm module khác chạm {@code notification..domain..}, nên lệnh gửi dùng enum này (cùng tên giá
 * trị với {@link NotificationType} — {@code NotificationKindTest} bảo đảm hai enum luôn khớp).
 */
public enum NotificationKind {
    ORDER_STATUS_CHANGED, KYC_APPROVED, KYC_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, ESCROW_RELEASED,
    WALLET_CREDITED, SYSTEM_ALERT, REFUND_PROCESSED, NEW_PLATFORM_VOUCHER, NEW_VENDOR_VOUCHER, NEW_ORDER_FOR_VENDOR,
    FLASH_SALE_REMINDER, NEW_DEVICE_LOGIN, PRODUCT_APPROVED, PRODUCT_REJECTED, CHAT_MESSAGE;

    public NotificationType toType() {
        return NotificationType.valueOf(name());
    }
}
