// Cron phát thông báo sàn hẹn giờ (UC-NOT-05 luồng thay thế; TS 10.3; plan 2026-11-26 thông báo D3, D16, N07.02)
package com.genie.notification.platform.application;

import com.genie.notification.platform.domain.PlatformNotification;
import com.genie.notification.platform.domain.PlatformNotificationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mỗi 30 giây: lấy tối đa {@value #BATCH_SIZE} thông báo {@code dispatched_at IS NULL AND send_at <= now}
 * ({@code FOR UPDATE SKIP LOCKED}), <b>mỗi dòng một transaction</b> — chốt {@code reach_count} tại lúc phát (D3), đặt
 * {@code dispatched_at}, đẩy realtime sau commit. Lỗi một dòng không dừng dòng khác (lượt sau làm lại dòng lỗi).
 */
@Component
public class PlatformNotificationDispatchJob {

    private static final Logger log = LoggerFactory.getLogger(PlatformNotificationDispatchJob.class);
    static final int BATCH_SIZE = 20;

    private final PlatformNotificationRepository notifications;
    private final PlatformNotificationAdminService admin;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PlatformNotificationDispatchJob(PlatformNotificationRepository notifications,
                                           PlatformNotificationAdminService admin,
                                           PlatformTransactionManager transactionManager, Clock clock) {
        this.notifications = notifications;
        this.admin = admin;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT30S")
    public void scheduled() {
        int n = run();
        if (n > 0) {
            log.info("Đã phát {} thông báo sàn hẹn giờ", n);
        }
    }

    /** Phát mọi thông báo tới giờ (tối đa một lô / dòng); trả số đã phát. */
    public int run() {
        int done = 0;
        for (int i = 0; i < BATCH_SIZE; i++) {
            Boolean one;
            try {
                one = tx.execute(status -> dispatchOne());
            } catch (RuntimeException e) {
                log.error("Phát thông báo sàn hẹn giờ lỗi: {}", e.toString());
                break;
            }
            if (!Boolean.TRUE.equals(one)) {
                break;
            }
            done++;
        }
        return done;
    }

    private boolean dispatchOne() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<Long> due = notifications.lockDueIds(now, 1);
        if (due.isEmpty()) {
            return false;
        }
        PlatformNotification n = notifications.findById(due.get(0)).orElse(null);
        if (n == null || n.isDispatched()) {
            return false;
        }
        admin.dispatch(n, admin.recipientIds(n), now);
        return true;
    }
}
