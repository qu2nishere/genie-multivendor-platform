// Cron phát thông báo sàn hẹn giờ (plan 2026-11-26 thông báo D3, D16, N07.03)
package com.genie.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.notification.platform.application.PlatformNotificationDispatchJob;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PlatformNotificationDispatchJobIT extends NotificationTestSupport {

    @Autowired
    PlatformNotificationDispatchJob job;

    private long scheduled(String title, Duration fromNow) {
        return jdbc.queryForObject("INSERT INTO platform_notifications (title, content, target, created_by, created_at, "
                + "updated_at, reach_count, send_at) VALUES (?, '<p>Hẹn giờ</p>', 'CUSTOMER', 1, now(), now(), 0, ?) "
                + "RETURNING id", Long.class, title, Timestamp.from(clock.instant().plus(fromNow)));
    }

    private Map<String, Object> row(long id) {
        return jdbc.queryForMap("SELECT dispatched_at, reach_count FROM platform_notifications WHERE id = ?", id);
    }

    private long customersAt() {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE status = 'ACTIVE' AND role = 'CUSTOMER' "
                + "AND created_at <= ?", Long.class, Timestamp.from(clock.instant()));
    }

    @Test
    void dispatchesDueNotificationsExactlyOnceAndCountsReachAtDispatchTime() {
        long due = scheduled("Tới giờ", Duration.ofMinutes(-1));
        long later = scheduled("Chưa tới giờ", Duration.ofHours(2));
        // người dùng có trước lúc phát — kể cả tạo SAU lúc hẹn giờ — đều được tính
        customerCreatedAt("2026-01-01T00:00:00Z");
        long expected = customersAt();

        assertThat(job.run()).isEqualTo(1);
        assertThat(row(due).get("dispatched_at")).isNotNull();
        assertThat(((Number) row(due).get("reach_count")).longValue()).isEqualTo(expected);
        assertThat(row(later).get("dispatched_at")).isNull();

        assertThat(job.run()).isZero();   // chạy lại không phát trùng

        clock.advance(Duration.ofHours(3));
        assertThat(job.run()).isEqualTo(1);
        assertThat(row(later).get("dispatched_at")).isNotNull();
    }

    @Test
    void concurrentRunsNeverDispatchTwice() throws Exception {
        for (int i = 0; i < 6; i++) {
            scheduled("Song song " + i, Duration.ofMinutes(-5));
        }
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Callable<Integer> run = job::run;
            List<Future<Integer>> results = pool.invokeAll(List.of(run, run, run));
            int total = 0;
            for (Future<Integer> f : results) {
                total += f.get();
            }
            assertThat(total).isEqualTo(6);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM platform_notifications WHERE dispatched_at IS NULL",
                Integer.class)).isZero();
    }
}
