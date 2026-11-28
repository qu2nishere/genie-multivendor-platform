// Gói tin trên /user/queue/notifications và /topic/platform — API_SPEC 13 (plan 2026-11-26 thông báo D5)
package com.genie.notification.inbox.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * {@code NEW_NOTIFICATION} / {@code PLATFORM_NOTIFICATION}: có {@code notification} (+ {@code unread_count} khi gửi riêng
 * một người). {@code NOTIFICATIONS_READ}: {@code ids} (đánh dấu một) hoặc {@code category} (đánh dấu tất cả) + {@code unread_count}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationEvent(String type, NotificationView notification, Long unreadCount, List<String> ids,
                                String category) {

    public static final String NEW_NOTIFICATION = "NEW_NOTIFICATION";
    public static final String PLATFORM_NOTIFICATION = "PLATFORM_NOTIFICATION";
    public static final String NOTIFICATIONS_READ = "NOTIFICATIONS_READ";

    public static NotificationEvent created(NotificationView view, long unread) {
        return new NotificationEvent(NEW_NOTIFICATION, view, unread, null, null);
    }

    public static NotificationEvent platform(NotificationView view, Long unread) {
        return new NotificationEvent(PLATFORM_NOTIFICATION, view, unread, null, null);
    }

    public static NotificationEvent read(List<String> ids, String category, long unread) {
        return new NotificationEvent(NOTIFICATIONS_READ, null, unread, ids, category);
    }
}
