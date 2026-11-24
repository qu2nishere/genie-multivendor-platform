// Cron mở lại tài khoản khi hết hạn đình chỉ (UC-ADM-03 luồng thay thế; LLD 10; TS 10.3; plan 2026-11-24 H03, D1–D4)
package com.genie.auth.profile.application;

import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.profile.event.AccountReactivatedEvent;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mỗi 5 phút mở mọi tài khoản {@code SUSPENDED} có {@code suspended_until <= now}: {@code ACTIVE}, xóa lý do / hạn
 * ({@link User#reactivateIfSuspensionExpired}), nhật ký {@code ACCOUNT_REACTIVATED}
 * ({@code reason = SUSPENSION_EXPIRED}, {@code source = CRON}, người làm = hệ thống) và email sau commit.
 * <ul>
 *   <li>Lô {@value #BATCH_SIZE} tài khoản, mỗi lô một transaction, khóa dòng {@code FOR UPDATE SKIP LOCKED} — chạy nhiều
 *       instance hoặc trùng lúc người dùng đăng nhập (luồng đăng nhập cũng tự mở, plan D4) không mở / ghi log hai lần.</li>
 *   <li>Tối đa {@value #MAX_PER_RUN} tài khoản / lượt, phần còn lại để lượt sau. Lô lỗi → ghi log, dừng lượt này
 *       (dữ liệu lô đó rollback nguyên vẹn, lượt 5 phút sau làm lại).</li>
 *   <li>Không đụng {@code LOCKED} (khóa nhân viên / khóa vô thời hạn), đình chỉ vô thời hạn ({@code suspended_until}
 *       NULL), {@code ANONYMIZED} / {@code DELETED} — câu truy vấn chỉ lấy {@code SUSPENDED} có hạn.</li>
 * </ul>
 * Không khôi phục phiên đã thu hồi lúc khóa: người dùng đăng nhập lại như thường.
 */
@Component
public class SuspensionExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(SuspensionExpiryJob.class);
    static final int BATCH_SIZE = 100;
    static final int MAX_PER_RUN = 1000;

    private final UserRepository users;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;

    public SuspensionExpiryJob(UserRepository users, AuditLogger audit, ApplicationEventPublisher events,
                               PlatformTransactionManager transactionManager, Clock clock) {
        this.users = users;
        this.audit = audit;
        this.events = events;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void scheduled() {
        int done = run();
        if (done > 0) {
            log.info("Đã mở khóa {} tài khoản hết hạn đình chỉ", done);
        }
    }

    /** Mở mọi tài khoản đã hết hạn đình chỉ (tối đa {@value #MAX_PER_RUN}); trả số tài khoản đã mở. */
    public int run() {
        int done = 0;
        while (done < MAX_PER_RUN) {
            int limit = Math.min(BATCH_SIZE, MAX_PER_RUN - done);
            Integer opened;
            try {
                opened = tx.execute(status -> reactivateBatch(limit));
            } catch (RuntimeException e) {
                log.error("Mở khóa tài khoản hết hạn đình chỉ lỗi, dừng lượt chạy: {}", e.toString());
                break;
            }
            if (opened == null || opened == 0) {
                break;
            }
            done += opened;
            if (opened < limit) {
                break;   // lô chưa đầy = đã hết tài khoản tới hạn (dòng bị khóa bởi luồng khác để lượt sau)
            }
        }
        return done;
    }

    private int reactivateBatch(int limit) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<User> due = users.findSuspensionExpired(now, limit);
        int opened = 0;
        for (User user : due) {
            OffsetDateTime until = user.getSuspendedUntil();
            if (!user.reactivateIfSuspensionExpired(now)) {
                continue;
            }
            Map<String, Object> details = new HashMap<>();
            details.put("reason", AccountReactivatedEvent.SUSPENSION_EXPIRED);
            details.put("source", "CRON");
            details.put("suspended_until", until);
            audit.record(AuditAction.ACCOUNT_REACTIVATED, (Long) null, String.valueOf(user.getId()), details,
                    ClientInfo.system(), true);
            events.publishEvent(new AccountReactivatedEvent(user.getId(), user.getEmail(),
                    AccountReactivatedEvent.SUSPENSION_EXPIRED, now));
            opened++;
        }
        return opened;
    }
}
