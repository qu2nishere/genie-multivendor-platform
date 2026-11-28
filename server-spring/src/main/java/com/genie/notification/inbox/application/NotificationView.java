// Một phần tử hộp thư — API_SPEC 3.12 (plan 2026-11-26 thông báo N03.02); dùng chung cho REST và gói tin WebSocket
package com.genie.notification.inbox.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.genie.notification.inbox.domain.NotificationCategory;
import java.time.OffsetDateTime;

/**
 * @param id          có tiền tố nguồn: {@code ntf_} (cá nhân), {@code pn_} (thông báo sàn), {@code an_} (nhân viên)
 * @param type        {@code NotificationType} / {@code PLATFORM_ANNOUNCEMENT} / {@code AdminNotificationType}
 * @param contentHtml chỉ có với {@code pn_} (HTML đã lọc ở server) — không có thì bỏ khỏi JSON
 */
public record NotificationView(String id, NotificationCategory category, String type, String title, String body,
                               String imageUrl, @JsonInclude(JsonInclude.Include.NON_NULL) String contentHtml,
                               OffsetDateTime createdAt, boolean isRead, NotificationLink link) {

    public static final String PERSONAL_PREFIX = "ntf_";
    public static final String PLATFORM_PREFIX = "pn_";
    public static final String ADMIN_PREFIX = "an_";
    public static final String PLATFORM_ANNOUNCEMENT = "PLATFORM_ANNOUNCEMENT";
}
