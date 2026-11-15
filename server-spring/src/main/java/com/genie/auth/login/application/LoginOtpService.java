package com.genie.auth.login.application;

import com.genie.auth.login.event.AccountLockedEvent;
import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpChallenge;
import com.genie.auth.otp.application.OtpChallengeService;
import com.genie.auth.otp.application.OtpExhaustedException;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.profile.domain.AccountStatus;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đăng nhập bằng mã OTP email (UC-AUTH-02 luồng 1a, API_SPEC 2.2 / 2.4 purpose LOGIN) và mở khóa tài khoản qua email
 * (purpose ACCOUNT_UNLOCK — màn "Tài khoản tạm khóa").
 * <ul>
 *   <li>LOGIN: email không tồn tại / không hoạt động → vẫn {@code 202} nhưng không gửi thư (chống dò email);
 *       sai mã 5 lần → khóa 15 phút ({@code locked_by = OTP}), {@code 422 GNE-AUTH-4221 {locked_until}};</li>
 *   <li>ACCOUNT_UNLOCK: chỉ khi đang khóa tạm ({@code 409 GNE-AUTH-4096} nếu không), bị đình chỉ → {@code 403};
 *       mã đúng → xóa khóa, ghi {@code ACCOUNT_REACTIVATED}, đăng nhập luôn.</li>
 * </ul>
 */
@Service
public class LoginOtpService {

    private static final Duration OTP_LOCK = Duration.ofMinutes(15);

    private final OtpChallengeService otps;
    private final UserRepository users;
    private final LoginService loginService;
    private final LockStatusStore lockStatus;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoginOtpService(OtpChallengeService otps, UserRepository users, LoginService loginService,
                           LockStatusStore lockStatus, AuditLogger audit, ApplicationEventPublisher events, Clock clock) {
        this.otps = otps;
        this.users = users;
        this.loginService = loginService;
        this.lockStatus = lockStatus;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    // ===== LOGIN =====

    @Transactional(readOnly = true)
    public IssuedOtp requestLogin(String rawEmail) {
        String email = User.normalizeEmail(rawEmail);
        Optional<User> user = users.findByEmail(email)
                .filter(u -> u.getStatus() == AccountStatus.ACTIVE || u.getStatus() == AccountStatus.SUSPENDED);
        return user.isPresent()
                ? otps.issue(OtpPurpose.LOGIN, email, user.get().getId(), Map.of())
                : otps.issueSilently(OtpPurpose.LOGIN, email);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult verifyLogin(String challengeId, String code, ClientInfo client) {
        OtpChallenge challenge;
        try {
            challenge = otps.verify(challengeId, code, OtpPurpose.LOGIN);
        } catch (OtpExhaustedException e) {
            throw lockAfterOtpFailures(e.getChallenge(), client);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        User user = loadForUpdate(challenge);
        if (user.isLocked(now)) {
            throw loginService.lockedError(user);
        }
        user.reactivateIfSuspensionExpired(now);
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw LoginService.suspendedError(user);
        }
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        return loginService.completeLogin(user, client, "OTP");
    }

    private BusinessException lockAfterOtpFailures(OtpChallenge challenge, ClientInfo client) {
        Map<String, Object> details = new HashMap<>();
        details.put("remaining_attempts", 0);
        if (challenge.userId() != null) {
            users.findByIdForUpdate(challenge.userId()).ifPresent(user -> {
                OffsetDateTime now = OffsetDateTime.now(clock);
                user.lockFor(OTP_LOCK, now);
                lockStatus.markLocked(user.getId(), LockStatusStore.BY_OTP, user.getLockedUntil());
                audit.record(AuditAction.ACCOUNT_LOCKED, user.getId(), null, Map.of("locked_by", "OTP"), client, true);
                events.publishEvent(new AccountLockedEvent(user.getId(), LockStatusStore.BY_OTP, user.getLockedUntil()));
                details.put("locked_until", user.getLockedUntil());
                details.put("locked_by", LockStatusStore.BY_OTP);
            });
        } else {
            details.put("locked_until", OffsetDateTime.now(clock).plus(OTP_LOCK));
            details.put("locked_by", LockStatusStore.BY_OTP);
        }
        return new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS, details);
    }

    // ===== ACCOUNT_UNLOCK =====

    @Transactional(readOnly = true)
    public IssuedOtp requestUnlock(String rawEmail) {
        String email = User.normalizeEmail(rawEmail);
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !(user.isActive() || user.getStatus() == AccountStatus.SUSPENDED)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LOCKED);
        }
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw LoginService.suspendedError(user);
        }
        if (!user.isLocked(OffsetDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_LOCKED);
        }
        return otps.issue(OtpPurpose.ACCOUNT_UNLOCK, email, user.getId(), Map.of());
    }

    @Transactional
    public AuthResult verifyUnlock(String challengeId, String code, ClientInfo client) {
        OtpChallenge challenge = otps.verify(challengeId, code, OtpPurpose.ACCOUNT_UNLOCK);
        User user = loadForUpdate(challenge);
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw LoginService.suspendedError(user);
        }
        user.clearLock();
        lockStatus.clear(user.getId());
        audit.record(AuditAction.ACCOUNT_REACTIVATED, user.getId(), null, Map.of("reason", "EMAIL_UNLOCK"), client, true);
        return loginService.completeLogin(user, client, "UNLOCK");
    }

    private User loadForUpdate(OtpChallenge challenge) {
        if (challenge.userId() == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        return users.findByIdForUpdate(challenge.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED));
    }
}
