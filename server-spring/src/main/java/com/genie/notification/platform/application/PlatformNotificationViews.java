// Lệnh và dữ liệu trả về của màn Admin › Thông báo sàn — API_SPEC 11.17 (plan 2026-11-26 thông báo N06)
package com.genie.notification.platform.application;

import java.time.OffsetDateTime;
import java.util.List;

public final class PlatformNotificationViews {

    private PlatformNotificationViews() {
    }

    /**
     * Thân {@code POST} / {@code PUT}.
     *
     * @param imageObjectKey khóa {@code tmp/…} vừa tải ({@code NOTIFICATION_IMAGE}); khi sửa: {@code null} = giữ ảnh,
     *                       chuỗi rỗng = bỏ ảnh
     * @param sendAt         {@code null} = phát ngay
     */
    public record Command(String title, String contentHtml, String imageObjectKey, String target,
                          List<String> recipientEmails, OffsetDateTime sendAt) {
    }

    /** Một dòng của bảng (API 11.17). */
    public record Row(long id, String code, String title, String contentPreview, String imageUrl, String target,
                      String createdBy, OffsetDateTime sendAt, boolean isScheduled, long readCount, long reachCount) {
    }

    /** Chi tiết: thêm HTML nội dung + email người nhận cụ thể. */
    public record Detail(long id, String code, String title, String contentPreview, String imageUrl, String target,
                         String createdBy, OffsetDateTime sendAt, boolean isScheduled, long readCount, long reachCount,
                         String contentHtml, List<String> recipientEmails) {
    }

    public static String code(long id) {
        return "PN-" + id;
    }
}
