// Nhóm hiển thị (tab) của hộp thư — API_SPEC 3.12 `category`; plan 2026-11-26 thông báo D6, N03.01; ENTITY Phụ lục A
package com.genie.notification.inbox.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Không lưu CSDL — suy từ {@link NotificationType}. Thông báo sàn ({@code pn_}) và thông báo nhân viên ({@code an_})
 * luôn thuộc {@link #SYSTEM}.
 */
public enum NotificationCategory {
    ORDER, PROMOTION, SYSTEM, CHAT;

    /** {@code switch} không {@code default}: thêm loại thông báo mà quên gán nhóm là lỗi biên dịch. */
    public static NotificationCategory of(NotificationType type) {
        return switch (type) {
            case ORDER_STATUS_CHANGED, PAYMENT_SUCCESS, PAYMENT_FAILED, REFUND_PROCESSED, NEW_ORDER_FOR_VENDOR,
                 ESCROW_RELEASED, WALLET_CREDITED -> ORDER;
            case NEW_PLATFORM_VOUCHER, NEW_VENDOR_VOUCHER, FLASH_SALE_REMINDER -> PROMOTION;
            case SYSTEM_ALERT, KYC_APPROVED, KYC_REJECTED, NEW_DEVICE_LOGIN, PRODUCT_APPROVED, PRODUCT_REJECTED -> SYSTEM;
            case CHAT_MESSAGE -> CHAT;
        };
    }

    /** Các loại thông báo cá nhân thuộc nhóm này. */
    public Set<NotificationType> types() {
        Set<NotificationType> out = EnumSet.noneOf(NotificationType.class);
        for (NotificationType t : NotificationType.values()) {
            if (of(t) == this) {
                out.add(t);
            }
        }
        return out;
    }

    /** Nhóm này có chứa thông báo sàn / nhân viên không ({@code null} = mọi nhóm). */
    public static boolean includesSystemSources(NotificationCategory category) {
        return category == null || category == SYSTEM;
    }
}
