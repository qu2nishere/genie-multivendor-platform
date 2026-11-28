package com.genie.notification.platform.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformNotificationRepository extends JpaRepository<PlatformNotification, Long> {

    /**
     * Thông báo hẹn giờ đã tới giờ mà chưa phát — cron (plan 2026-11-26 thông báo D16): khóa dòng {@code SKIP LOCKED} (chỉ lấy {@code id} — entity có trường {@code @Formula}, nạp lại bằng {@code findById} trong cùng transaction) để
     * hai lượt chạy chồng / nhiều instance không phát trùng.
     */
    @Query(value = "SELECT id FROM platform_notifications WHERE dispatched_at IS NULL AND send_at <= :now"
            + " ORDER BY send_at, id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Long> lockDueIds(@Param("now") OffsetDateTime now, @Param("limit") int limit);
}
