// Tài khoản bị đình chỉ / khóa vừa được mở lại (UC-ADM-03; API_SPEC 11.13; plan 2026-11-24 D1–D2)
package com.genie.auth.profile.event;

import java.time.OffsetDateTime;

/**
 * Phát trong transaction mở khóa, listener gửi email {@code account-reactivated} SAU commit.
 * {@code reason}: {@code SUSPENSION_EXPIRED} (cron hết hạn đình chỉ) hoặc {@code ADMIN} (Admin mở thủ công) —
 * lý do chữ Admin nhập chỉ nằm trong nhật ký, không gửi cho người dùng.
 */
public record AccountReactivatedEvent(Long userId, String email, String reason, OffsetDateTime reactivatedAt) {

    public static final String SUSPENSION_EXPIRED = "SUSPENSION_EXPIRED";
    public static final String ADMIN = "ADMIN";
}
