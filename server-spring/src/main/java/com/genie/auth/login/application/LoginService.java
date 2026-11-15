package com.genie.auth.login.application;

import com.genie.auth.login.event.AccountLockedEvent;
import com.genie.auth.login.event.NewDeviceLoginEvent;
import com.genie.auth.profile.application.AccountSummaryService;
import com.genie.auth.profile.domain.AccountStatus;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.application.OpenedSession;
import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.application.UserAgentParser;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditActor;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng nhập bằng email + mật khẩu (UC-AUTH-02, API_SPEC 2.5, LLD 7; plan AUTH A07.01–A07.03).
 * <ol>
 *   <li>Giới hạn 10 lần / phút / IP ({@code 429});</li>
 *   <li>email không tồn tại / tài khoản đã xóa → vẫn băm giả để thời gian đồng đều, {@code 401 GNE-AUTH-4011} (chống dò email);</li>
 *   <li>đang khóa tạm → {@code 423 GNE-AUTH-4230} {@code {locked_until, locked_by}} (không kiểm mật khẩu, không đếm thêm);</li>
 *   <li>sai mật khẩu → đếm, lần 5 khóa 15 phút; còn lại {@code 401} kèm {@code remaining_attempts};</li>
 *   <li>đúng mật khẩu nhưng bị Admin đình chỉ → {@code 403 GNE-AUTH-4031} {@code {reason, until}} (kiểm SAU mật khẩu để
 *       người không biết mật khẩu không dò được trạng thái tài khoản);</li>
 *   <li>thành công → đặt lại bộ đếm, ghi {@code LOGIN_SUCCESS}, báo thiết bị lạ, mở phiên.</li>
 * </ol>
 * Transaction không rollback khi ném {@link BusinessException} — bộ đếm sai và nhật ký phải được lưu.
 */
@Service
public class LoginService {

    public static final int MAX_LOGINS_PER_MINUTE_PER_IP = 10;
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessions;
    private final AccountSummaryService summaries;
    private final LockStatusStore lockStatus;
    private final NewDeviceDetector newDeviceDetector;
    private final RateLimiter rateLimiter;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final String dummyHash;

    public LoginService(UserRepository users, PasswordEncoder passwordEncoder, SessionService sessions,
                        AccountSummaryService summaries, LockStatusStore lockStatus, NewDeviceDetector newDeviceDetector,
                        RateLimiter rateLimiter, AuditLogger audit, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.summaries = summaries;
        this.lockStatus = lockStatus;
        this.newDeviceDetector = newDeviceDetector;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("genie-dummy-password-for-timing");
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult login(String rawEmail, String password, ClientInfo client) {
        if (client != null && client.ip() != null) {
            rateLimiter.check("rl:login:" + client.ip(), MAX_LOGINS_PER_MINUTE_PER_IP, Duration.ofMinutes(1));
        }
        String email = User.normalizeEmail(rawEmail);
        OffsetDateTime now = OffsetDateTime.now(clock);
        User user = users.findByEmail(email).flatMap(u -> users.findByIdForUpdate(u.getId())).orElse(null);

        if (user == null || user.getStatus() == AccountStatus.ANONYMIZED || user.getStatus() == AccountStatus.DELETED
                || user.getStatus() == AccountStatus.UNVERIFIED) {
            passwordEncoder.matches(password == null ? "" : password, dummyHash);
            audit.record(AuditAction.LOGIN_FAILED, AuditActor.anonymous(email), null,
                    Map.of("reason", "UNKNOWN_EMAIL"), client, false);
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS);
        }

        if (user.isLocked(now)) {
            throw lockedError(user);
        }

        boolean passwordOk = user.hasPassword() && password != null && passwordEncoder.matches(password, user.getPassword());
        if (!passwordOk) {
            user.recordFailedLogin(now);
            audit.record(AuditAction.LOGIN_FAILED, user.getId(), null,
                    Map.of("reason", user.hasPassword() ? "BAD_PASSWORD" : "NO_PASSWORD"), client, false);
            if (user.isLocked(now)) {
                lockStatus.markLocked(user.getId(), LockStatusStore.BY_PASSWORD, user.getLockedUntil());
                audit.record(AuditAction.ACCOUNT_LOCKED, user.getId(), null, Map.of("locked_by", "PASSWORD"), client, true);
                events.publishEvent(new AccountLockedEvent(user.getId(), LockStatusStore.BY_PASSWORD, user.getLockedUntil()));
                throw lockedError(user);
            }
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS,
                    Map.of("remaining_attempts", MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts()));
        }

        if (user.reactivateIfSuspensionExpired(now)) {
            audit.record(AuditAction.ACCOUNT_REACTIVATED, user.getId(), null, Map.of("reason", "SUSPENSION_EXPIRED"),
                    client, true);
        }
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw suspendedError(user);
        }

        if (passwordEncoder.upgradeEncoding(user.getPassword())) {
            user.changePassword(passwordEncoder.encode(password));
        }
        return completeLogin(user, client, "PASSWORD");
    }

    /**
     * Phần chung của mọi cách đăng nhập thành công (mật khẩu, OTP, mở khóa, Google): đặt lại bộ đếm, nhật ký,
     * thiết bị lạ, mở phiên. Gọi trong transaction của luồng.
     */
    public AuthResult completeLogin(User user, ClientInfo client, String method) {
        user.recordSuccessfulLogin();
        lockStatus.clear(user.getId());
        boolean newDevice = newDeviceDetector.isNewDevice(user.getId(), client);
        OpenedSession session = sessions.open(user, client);
        Map<String, Object> details = new HashMap<>();
        details.put("method", method);
        details.put("session_id", session.sessionId());
        audit.record(AuditAction.LOGIN_SUCCESS, user.getId(), null, details, client, true);
        if (newDevice) {
            events.publishEvent(new NewDeviceLoginEvent(user.getId(), user.getEmail(),
                    UserAgentParser.parse(client == null ? null : client.userAgent()).name(),
                    client == null ? null : client.ip(), OffsetDateTime.now(clock)));
        }
        return new AuthResult(session, summaries.summaryOf(user));
    }

    public BusinessException lockedError(User user) {
        Map<String, Object> details = new HashMap<>();
        details.put("locked_until", user.getLockedUntil());
        details.put("locked_by", lockStatus.lockedBy(user.getId()));
        return new BusinessException(ErrorCode.ACCOUNT_LOCKED, details);
    }

    public static BusinessException suspendedError(User user) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", user.getSuspensionReason());
        details.put("until", user.getSuspendedUntil());
        return new BusinessException(ErrorCode.ACCOUNT_SUSPENDED, details);
    }
}
