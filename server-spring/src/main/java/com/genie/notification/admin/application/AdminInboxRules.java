// Ai thấy loại thông báo nhân viên nào — API_SPEC 3.12 dòng `an_` (plan 2026-11-26 thông báo D7)
package com.genie.notification.admin.application;

import com.genie.notification.admin.domain.AdminNotificationType;
import com.genie.notification.inbox.application.NotificationLink;
import java.util.EnumSet;
import java.util.Set;

/**
 * {@code USER_APPROVAL} ↔ {@code REVIEW_KYC}, {@code WITHDRAW_REQUEST} ↔ {@code PROCESS_PAYOUT},
 * {@code NEW_ORDER} / {@code ESCROW_DISCREPANCY} ↔ {@code VIEW_FINANCE}, {@code SYSTEM_ALERT} ↔ mọi nhân viên.
 * ADMIN có đủ mọi quyền.
 */
public final class AdminInboxRules {

    private AdminInboxRules() {
    }

    /** Mã quyền cần để thấy loại này; {@code null} = mọi nhân viên. */
    public static String requiredPermission(AdminNotificationType type) {
        return switch (type) {
            case USER_APPROVAL -> "REVIEW_KYC";
            case WITHDRAW_REQUEST -> "PROCESS_PAYOUT";
            case NEW_ORDER, ESCROW_DISCREPANCY -> "VIEW_FINANCE";
            case SYSTEM_ALERT -> null;
        };
    }

    /** Các loại nhân viên có {@code permissions} được thấy ({@code admin = true} → tất cả). */
    public static Set<AdminNotificationType> visibleTypes(boolean admin, Set<String> permissions) {
        Set<AdminNotificationType> out = EnumSet.noneOf(AdminNotificationType.class);
        for (AdminNotificationType t : AdminNotificationType.values()) {
            String p = requiredPermission(t);
            if (admin || p == null || permissions.contains(p)) {
                out.add(t);
            }
        }
        return out;
    }

    /** Đích bấm vào trong Admin Console theo loại. */
    public static NotificationLink linkOf(AdminNotificationType type) {
        return switch (type) {
            case USER_APPROVAL -> NotificationLink.of(NotificationLink.ADMIN_KYC_QUEUE);
            case WITHDRAW_REQUEST -> NotificationLink.of(NotificationLink.ADMIN_WITHDRAWALS);
            case NEW_ORDER, ESCROW_DISCREPANCY, SYSTEM_ALERT -> null;
        };
    }
}
