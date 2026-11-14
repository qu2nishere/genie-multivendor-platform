package com.genie.notification.platform.domain;

import java.io.Serializable;
import java.util.Objects;
import lombok.*;

/**
 * Khóa gộp của UserPlatformNotificationRead (@IdClass) — trường trùng tên & kiểu với các trường @Id của entity (implement-entity.md D4).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserPlatformNotificationReadId implements Serializable {

    private Long userId;
    private Long platformNotificationId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPlatformNotificationReadId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(platformNotificationId, that.platformNotificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, platformNotificationId);
    }
}
