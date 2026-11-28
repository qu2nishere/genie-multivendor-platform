// Đổi entity → phần tử hộp thư API 3.12 (plan 2026-11-26 thông báo N03.02)
package com.genie.notification.inbox.application;

import com.genie.notification.admin.application.AdminInboxRules;
import com.genie.notification.admin.domain.AdminNotification;
import com.genie.notification.inbox.domain.Notification;
import com.genie.notification.inbox.domain.NotificationCategory;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Thời gian trả theo giờ Việt Nam (+07:00) như các API khác. */
public final class NotificationViews {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private NotificationViews() {
    }

    public static OffsetDateTime vn(OffsetDateTime t) {
        return t == null ? null : t.atZoneSameInstant(VN).toOffsetDateTime();
    }

    public static NotificationView personal(Notification n) {
        return new NotificationView(NotificationView.PERSONAL_PREFIX + n.getId(), NotificationCategory.of(n.getType()),
                n.getType().name(), n.getTitle(), n.getBody(), n.getImageUrl(), null, vn(n.getCreatedAt()),
                Boolean.TRUE.equals(n.getIsRead()), linkOf(n.getReferenceType(), n.getReferenceId()));
    }

    public static NotificationView admin(AdminNotification a) {
        return new NotificationView(NotificationView.ADMIN_PREFIX + a.getId(), NotificationCategory.SYSTEM,
                a.getType().name(), a.getTitle(), a.getContent(), a.getImageUrl(), null, vn(a.getCreatedAt()),
                Boolean.TRUE.equals(a.getIsRead()), AdminInboxRules.linkOf(a.getType()));
    }

    static NotificationLink linkOf(String type, String id) {
        if (type == null || type.isBlank()) {
            return null;
        }
        Long parsed = null;
        if (id != null) {
            try {
                parsed = Long.parseLong(id);
            } catch (NumberFormatException ignored) {
                // id không phải số (vd mã) — chỉ giữ loại đích
            }
        }
        return new NotificationLink(type, parsed);
    }
}
