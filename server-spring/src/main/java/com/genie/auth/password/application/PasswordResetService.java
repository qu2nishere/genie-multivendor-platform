package com.genie.auth.password.application;

import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpChallenge;
import com.genie.auth.otp.application.OtpChallengeService;
import com.genie.auth.otp.application.OtpExhaustedException;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.password.event.PasswordChangedEvent;
import com.genie.auth.profile.domain.AccountStatus;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.Hashing;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quên mật khẩu (UC-AUTH-04, API_SPEC 2.2 / 2.4 / 2.8; plan AUTH A08).
 * <ol>
 *   <li>Xin mã: luôn {@code 202} — email không tồn tại, tài khoản chỉ có Google, đang bị khóa khôi phục thì không gửi thư;</li>
 *   <li>mã đúng → {@code reset_token} ({@code rst_…}, 10 phút, dùng một lần, Redis {@code auth:reset:{sha256}});
 *       sai 5 lần → khóa khôi phục email này 30 phút ({@code auth:pwreset_lock:{email}});</li>
 *   <li>đặt mật khẩu mới: khác mật khẩu hiện tại ({@code 422 GNE-AUTH-4223}), mở khóa đăng nhập, thu hồi mọi phiên,
 *       ghi {@code PASSWORD_RESET}, gửi email cảnh báo.</li>
 * </ol>
 */
@Service
public class PasswordResetService {

    public static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
    public static final Duration RECOVERY_LOCK = Duration.ofMinutes(30);
    private static final String RESET_KEY = "auth:reset:";
    private static final String LOCK_KEY = "auth:pwreset_lock:";

    private final OtpChallengeService otps;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessions;
    private final StringRedisTemplate redis;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordResetService(OtpChallengeService otps, UserRepository users, PasswordEncoder passwordEncoder,
                                SessionService sessions, StringRedisTemplate redis, AuditLogger audit,
                                ApplicationEventPublisher events, Clock clock) {
        this.otps = otps;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.redis = redis;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    /** Kết quả xác minh OTP: {@code { reset_token, expires_in }}. */
    public record ResetTicket(String resetToken, long expiresIn) {
    }

    @Transactional(readOnly = true)
    public IssuedOtp request(String rawEmail) {
        String email = User.normalizeEmail(rawEmail);
        if (isRecoveryLocked(email)) {
            return otps.issueSilently(OtpPurpose.PASSWORD_RESET, email);
        }
        return users.findByEmail(email)
                .filter(u -> u.getStatus() == AccountStatus.ACTIVE && u.hasPassword())
                .map(u -> otps.issue(OtpPurpose.PASSWORD_RESET, email, u.getId(), Map.of()))
                .orElseGet(() -> otps.issueSilently(OtpPurpose.PASSWORD_RESET, email));
    }

    public ResetTicket verify(String challengeId, String code) {
        OtpChallenge challenge;
        try {
            challenge = otps.verify(challengeId, code, OtpPurpose.PASSWORD_RESET);
        } catch (OtpExhaustedException e) {
            OffsetDateTime until = OffsetDateTime.now(clock).plus(RECOVERY_LOCK);
            write(LOCK_KEY + e.getChallenge().email(), "1", RECOVERY_LOCK);
            Map<String, Object> details = new HashMap<>();
            details.put("remaining_attempts", 0);
            details.put("locked_until", until);
            throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS, details);
        }
        if (challenge.userId() == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        String token = "rst_" + Hashing.randomToken(32);
        write(RESET_KEY + Hashing.sha256Hex(token), challenge.userId().toString(), RESET_TOKEN_TTL);
        return new ResetTicket(token, RESET_TOKEN_TTL.toSeconds());
    }

    @Transactional
    public void reset(String resetToken, String newPassword, ClientInfo client) {
        String key = RESET_KEY + Hashing.sha256Hex(resetToken == null ? "" : resetToken);
        String userId = read(key);
        if (userId == null) {
            throw new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED);
        }
        User user = users.findByIdForUpdate(Long.valueOf(userId))
                .filter(u -> u.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_EXPIRED));
        if (user.hasPassword() && passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSED);
        }
        delete(key);
        OffsetDateTime now = OffsetDateTime.now(clock);
        user.changePassword(passwordEncoder.encode(newPassword));
        user.clearLock();
        sessions.revokeAllSessions(user.getId(), RevokeReason.PASSWORD_CHANGED);
        audit.record(AuditAction.PASSWORD_RESET, user.getId(), null, null, client, true);
        events.publishEvent(PasswordChangedEvent.of(user.getId(), user.getEmail(), "RESET", now, client));
    }

    private boolean isRecoveryLocked(String email) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(LOCK_KEY + email));
        } catch (DataAccessException e) {
            return false;
        }
    }

    private void write(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private String read(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private void delete(String key) {
        try {
            redis.delete(key);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
