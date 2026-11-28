// Loại thông báo nhân viên mà module khác được gửi — bản công khai của AdminNotificationType (plan 2026-11-26 thông báo D7, D10)
package com.genie.notification.admin.application;

import com.genie.notification.admin.domain.AdminNotificationType;

/** Cùng tên giá trị với {@link AdminNotificationType} ({@code NotificationKindTest} kiểm). */
public enum StaffAlertKind {
    SYSTEM_ALERT, NEW_ORDER, USER_APPROVAL, WITHDRAW_REQUEST, ESCROW_DISCREPANCY;

    public AdminNotificationType toType() {
        return AdminNotificationType.valueOf(name());
    }
}
