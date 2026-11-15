package com.genie.auth.password.application;

import com.genie.auth.password.event.PasswordChangedEvent;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đổi / thiết lập mật khẩu khi đang đăng nhập (UC-AUTH-06, API_SPEC 3.4; plan AUTH A09). OTP bước 2 do
 * {@code @RequireStepUp(PASSWORD_CHANGE)} kiểm ở controller trước khi vào đây.
 * <ul>
 *   <li>Tài khoản có mật khẩu: bắt buộc {@code current_password} đúng ({@code 401 GNE-AUTH-4011});</li>
 *   <li>tài khoản chỉ có Google / Apple ({@code password = NULL}): thiết lập lần đầu, không hỏi mật khẩu cũ;</li>
 *   <li>mật khẩu mới trùng hiện tại → {@code 422 GNE-AUTH-4223}; thành công → thu hồi mọi phiên KHÁC, giữ phiên hiện tại.</li>
 * </ul>
 */
@Service
public class PasswordChangeService {

    public static final int MAX_VERIFY_PER_MINUTE = 10;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessions;
    private final RateLimiter rateLimiter;
    private final AuditLogger audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordChangeService(UserRepository users, PasswordEncoder passwordEncoder, SessionService sessions,
                                 RateLimiter rateLimiter, AuditLogger audit, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.rateLimiter = rateLimiter;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    /** "Chính xác / Không chính xác" khi đang gõ mật khẩu hiện tại — 10 lần / phút / tài khoản. */
    @Transactional(readOnly = true)
    public boolean verifyCurrent(long userId, String password) {
        rateLimiter.check("rl:pwverify:" + userId, MAX_VERIFY_PER_MINUTE, Duration.ofMinutes(1));
        return users.findById(userId)
                .filter(User::hasPassword)
                .map(u -> password != null && passwordEncoder.matches(password, u.getPassword()))
                .orElse(false);
    }

    @Transactional
    public void change(AuthUser me, String currentPassword, String newPassword, ClientInfo client) {
        User user = users.findByIdForUpdate(me.id()).filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        boolean setup = !user.hasPassword();
        if (!setup) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "Mật khẩu hiện tại không chính xác.");
            }
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new BusinessException(ErrorCode.PASSWORD_REUSED);
            }
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        user.changePassword(passwordEncoder.encode(newPassword));
        if (me.sessionId() != null) {
            sessions.revokeOtherSessions(user.getId(), me.sessionId(), RevokeReason.PASSWORD_CHANGED, client);
        }
        audit.record(AuditAction.PASSWORD_CHANGED, user.getId(), null, Map.of("method", setup ? "SETUP" : "CHANGE"),
                client, true);
        events.publishEvent(PasswordChangedEvent.of(user.getId(), user.getEmail(), setup ? "SETUP" : "CHANGE", now,
                client));
    }
}
