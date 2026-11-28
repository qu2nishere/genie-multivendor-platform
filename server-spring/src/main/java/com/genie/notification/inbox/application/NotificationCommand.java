// Lệnh tạo một thông báo cá nhân — module khác gọi NotificationFacade.send (plan 2026-11-26 thông báo D10, D12, N03.02)
package com.genie.notification.inbox.application;

import java.util.Map;
import java.util.Objects;

/**
 * Tiêu đề / thân là chuỗi tiếng Việt đã ghép sẵn (snapshot, như email — D12); {@code metadata} lưu các giá trị gốc
 * (mã đơn, tên shop…) vào {@code notifications.metadata_json} để sau này client dịch lại được.
 *
 * @param imageUrl ảnh minh họa (URL công khai) — có thể {@code null}
 * @param link     đích bấm vào — có thể {@code null} (chỉ mở modal chi tiết)
 */
public record NotificationCommand(long userId, NotificationKind type, String title, String body, String imageUrl,
                                  NotificationLink link, Map<String, ?> metadata) {

    public NotificationCommand {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title rỗng");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static NotificationCommand of(long userId, NotificationKind type, String title, String body,
                                         NotificationLink link) {
        return new NotificationCommand(userId, type, title, body, null, link, Map.of());
    }

    public NotificationCommand withMetadata(Map<String, ?> values) {
        return new NotificationCommand(userId, type, title, body, imageUrl, link, values);
    }
}
