// Đích bấm vào của một thông báo — API_SPEC 3.12 `link` (plan 2026-11-26 thông báo N03.02)
package com.genie.notification.inbox.application;

import java.util.Objects;

/**
 * @param type {@code ORDER, VOUCHER_WALLET, SECURITY, HOME, PLATFORM_NOTIFICATION, PRODUCT, KYC, WITHDRAWAL,
 *             CONVERSATION, ADMIN_KYC_QUEUE, ADMIN_PRODUCT_QUEUE, ADMIN_WITHDRAWALS} — client đổi thành route
 * @param id   id đối tượng (đơn, sản phẩm, hội thoại…); {@code null} khi đích không cần id
 */
public record NotificationLink(String type, Long id) {

    public static final String ORDER = "ORDER";
    public static final String VOUCHER_WALLET = "VOUCHER_WALLET";
    public static final String SECURITY = "SECURITY";
    public static final String HOME = "HOME";
    public static final String PLATFORM_NOTIFICATION = "PLATFORM_NOTIFICATION";
    public static final String PRODUCT = "PRODUCT";
    public static final String KYC = "KYC";
    public static final String WITHDRAWAL = "WITHDRAWAL";
    public static final String CONVERSATION = "CONVERSATION";
    public static final String ADMIN_KYC_QUEUE = "ADMIN_KYC_QUEUE";
    public static final String ADMIN_PRODUCT_QUEUE = "ADMIN_PRODUCT_QUEUE";
    public static final String ADMIN_WITHDRAWALS = "ADMIN_WITHDRAWALS";

    public NotificationLink {
        Objects.requireNonNull(type, "type");
    }

    public static NotificationLink of(String type, Long id) {
        return new NotificationLink(type, id);
    }

    public static NotificationLink of(String type) {
        return new NotificationLink(type, null);
    }
}
