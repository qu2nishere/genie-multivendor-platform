package com.genie.auth.deletion.application;

import com.genie.auth.address.domain.Address;
import com.genie.auth.address.domain.AddressRepository;
import com.genie.auth.deletion.event.AccountAnonymizedEvent;
import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.CustomerProfileRepository;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cron ẩn danh tài khoản đã yêu cầu xóa quá 30 ngày — UC-AUTH-10, FR-AUTH-07, LLD 10. Chạy 02:30 hằng ngày (giờ
 * Việt Nam). Mỗi tài khoản một transaction, khóa dòng {@code FOR UPDATE SKIP LOCKED} nên chạy nhiều instance không
 * xử lý trùng; lỗi một tài khoản không làm dừng các tài khoản khác.
 * <p>Ẩn danh: email → {@code deleted_user_{id}@anonymized.local}, bỏ mật khẩu và liên kết Google (đăng nhập không
 * được nữa, email cũ đăng ký mới được), hồ sơ → "Người dùng đã xóa" (bỏ số điện thoại, ngày sinh, ảnh, giới thiệu),
 * xóa mềm sổ địa chỉ, thu hồi mọi phiên, nhật ký {@code PII_ANONYMIZED}. <b>Không</b> đụng đơn hàng, thanh toán,
 * sổ cái (giữ theo nghĩa vụ kế toán). Ảnh đại diện trên Silo xóa sau commit.
 */
@Component
public class AnonymizeAccountsJob {

    private static final Logger log = LoggerFactory.getLogger(AnonymizeAccountsJob.class);
    /** Giới hạn mỗi lần chạy — phần còn lại để ngày hôm sau. */
    static final int MAX_PER_RUN = 1000;

    private final UserRepository users;
    private final CustomerProfileRepository profiles;
    private final AddressRepository addresses;
    private final SessionService sessions;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final Clock clock;

    public AnonymizeAccountsJob(UserRepository users, CustomerProfileRepository profiles, AddressRepository addresses,
                                SessionService sessions, AuditLogger audit, ApplicationEventPublisher events,
                                PlatformTransactionManager transactionManager, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.addresses = addresses;
        this.sessions = sessions;
        this.audit = audit;
        this.events = events;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduled() {
        int done = run();
        if (done > 0) {
            log.info("Đã ẩn danh {} tài khoản quá hạn xóa", done);
        }
    }

    /** Ẩn danh mọi tài khoản đến hạn; trả số tài khoản đã xử lý. */
    public int run() {
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minus(AccountDeletionService.GRACE_PERIOD);
        int done = 0;
        while (done < MAX_PER_RUN) {
            Boolean processed;
            try {
                processed = tx.execute(status -> anonymizeNext(cutoff));
            } catch (RuntimeException e) {
                log.error("Ẩn danh tài khoản lỗi, dừng lượt chạy: {}", e.toString());
                break;
            }
            if (!Boolean.TRUE.equals(processed)) {
                break;
            }
            done++;
        }
        return done;
    }

    private boolean anonymizeNext(OffsetDateTime cutoff) {
        List<User> due = users.findDueForAnonymization(cutoff, 1);
        if (due.isEmpty()) {
            return false;
        }
        User user = due.get(0);
        OffsetDateTime now = OffsetDateTime.now(clock);

        Optional<CustomerProfile> profile = profiles.findByUserIdForUpdate(user.getId());
        String avatarUrl = profile.map(CustomerProfile::getAvatarUrl).orElse(null);
        profile.ifPresent(p -> p.anonymize(now));
        for (Address address : addresses.findActiveByUserId(user.getId())) {
            address.softDelete();
        }
        user.anonymize();
        sessions.revokeAllSessions(user.getId(), RevokeReason.ACCOUNT_DELETED);
        audit.record(AuditAction.PII_ANONYMIZED, user.getId(), null, null, ClientInfo.system(), true);
        events.publishEvent(new AccountAnonymizedEvent(user.getId(), avatarUrl, now));
        return true;
    }
}
