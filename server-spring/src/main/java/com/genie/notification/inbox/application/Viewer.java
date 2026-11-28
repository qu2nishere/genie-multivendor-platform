// Người xem hộp thư — vai trò, lúc tạo tài khoản, quyền nhân viên (plan 2026-11-26 thông báo D1, D3, D7)
package com.genie.notification.inbox.application;

import com.genie.common.domain.UserRole;
import com.genie.notification.admin.application.AdminInboxRules;
import com.genie.notification.admin.domain.AdminNotificationType;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @param createdAt   thông báo sàn phát trước thời điểm này không hiện (D3)
 * @param permissions mã quyền nhân viên đang có hiệu lực (rỗng với khách / shop)
 */
public record Viewer(long userId, UserRole role, OffsetDateTime createdAt, Set<String> permissions) {

    public Viewer {
        permissions = Set.copyOf(permissions);
    }

    /** Khách / shop nhận thông báo sàn; nhân viên / Admin dùng hộp {@code admin_notifications}. */
    public boolean receivesPlatform() {
        return role == UserRole.CUSTOMER || role == UserRole.VENDOR;
    }

    public boolean isStaff() {
        return role == UserRole.STAFF || role == UserRole.ADMIN;
    }

    public Set<AdminNotificationType> adminTypes() {
        return isStaff() ? AdminInboxRules.visibleTypes(role == UserRole.ADMIN, permissions) : Set.of();
    }
}
