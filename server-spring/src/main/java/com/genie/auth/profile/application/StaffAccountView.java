// DTO của UserFacade cho module admin — tài khoản nội bộ STAFF / ADMIN (plan B1 D12; API 11.18)
package com.genie.auth.profile.application;

import com.genie.common.domain.UserRole;

/**
 * Tài khoản nội bộ cho danh sách nhân viên. {@code status} là tên {@code AccountStatus}
 * ({@code UNVERIFIED, ACTIVE, LOCKED, SUSPENDED}); module {@code admin} tự ánh xạ sang {@code StaffStatus} (D5).
 */
public record StaffAccountView(long id, String email, UserRole role, String status) {

    public boolean isPendingActivation() {
        return "UNVERIFIED".equals(status);
    }

    public boolean isLocked() {
        return "SUSPENDED".equals(status) || "LOCKED".equals(status);
    }

    public boolean isActiveAdmin() {
        return role == UserRole.ADMIN && "ACTIVE".equals(status);
    }
}
