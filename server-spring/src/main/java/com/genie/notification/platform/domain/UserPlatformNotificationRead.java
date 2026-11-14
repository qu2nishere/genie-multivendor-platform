package com.genie.notification.platform.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * UserPlatformNotificationRead — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "user_platform_notification_reads")
@IdClass(UserPlatformNotificationReadId.class)
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserPlatformNotificationRead {

    @Id
    private Long userId;                      // FK → users.id — phần 1 khoá chính gộp

    @Id
    private Long platformNotificationId;      // FK → platform_notifications.id — phần 2 khoá chính gộp

    @Column(nullable = false)
    private OffsetDateTime readAt;             // Thời điểm user đọc
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu đã đọc thông báo toàn sàn — UC-NOT-05 */
    public static UserPlatformNotificationRead of(Long userId, Long notificationId, OffsetDateTime now) {
        UserPlatformNotificationRead r = new UserPlatformNotificationRead();
        r.userId = userId; r.platformNotificationId = notificationId; r.readAt = now;
        return r;
    }
}
