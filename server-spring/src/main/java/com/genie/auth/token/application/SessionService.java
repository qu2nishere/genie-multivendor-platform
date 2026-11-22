package com.genie.auth.token.application;

import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.auth.token.domain.RefreshToken;
import com.genie.auth.token.domain.RefreshTokenRepository;
import com.genie.auth.token.domain.RevokeReason;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditLogger;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.realtime.UserChannel;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.Hashing;
import com.genie.common.security.JwtService;
import com.genie.common.security.SecurityProperties;
import com.genie.common.security.TokenRevocationStore;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiên đăng nhập (UC-AUTH-02, UC-AUTH-11; LLD 11.2; plan AUTH A07.04–A07.09):
 * cấp cặp token, xoay vòng refresh token trong cùng {@code family_id}, phát hiện dùng lại, đăng xuất, thiết bị đăng nhập.
 * Refresh token 48 byte ngẫu nhiên, chỉ lưu SHA-256; sống 7 ngày và gia hạn mỗi lần xoay vòng.
 */
@Service
public class SessionService {

    /** Token vừa xoay vòng mà bị dùng lại trong khoảng này → coi là 2 tab cùng làm mới, không phải đánh cắp. */
    static final Duration ROTATION_GRACE = Duration.ofSeconds(10);
    private static final int REFRESH_TOKEN_BYTES = 48;

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final JwtService jwtService;
    private final TokenRevocationStore revocations;
    private final AuditLogger audit;
    private final Clock clock;
    private final Duration refreshTtl;
    private final UserChannel realtime;

    public SessionService(RefreshTokenRepository tokens, UserRepository users, JwtService jwtService,
                          TokenRevocationStore revocations, AuditLogger audit, Clock clock, SecurityProperties props,
                          UserChannel realtime) {
        this.tokens = tokens;
        this.users = users;
        this.jwtService = jwtService;
        this.revocations = revocations;
        this.audit = audit;
        this.clock = clock;
        this.refreshTtl = props.refreshTokenTtl();
        this.realtime = realtime;
    }

    /** Mở phiên mới cho user vừa xác thực (mật khẩu, OTP, Google, đăng ký, mở khóa). */
    @Transactional
    public OpenedSession open(User user, ClientInfo client) {
        long familyId = tokens.nextFamilyId();
        String raw = Hashing.randomToken(REFRESH_TOKEN_BYTES);
        OffsetDateTime now = now();
        ClientInfo c = client == null ? ClientInfo.system() : client;
        RefreshToken token = RefreshToken.issue(user.getId(), familyId, Hashing.sha256Hex(raw), c.userAgent(), c.ip(),
                now.plus(refreshTtl));
        token.touch(now);
        tokens.save(token);
        return new OpenedSession(jwtService.issue(user.getId(), user.getRole(), familyId), raw, refreshTtl, familyId, user.getId());
    }

    /**
     * Làm mới access token bằng cookie refresh (API_SPEC 2.6). Token đã thu hồi mà bị dùng lại (quá 10 giây ân hạn)
     * → thu hồi MỌI phiên của user + chặn mọi access token đang có ({@code SUSPICIOUS_REUSE}).
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public OpenedSession refresh(String rawRefreshToken, ClientInfo client) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new RefreshRejectedException(false);
        }
        OffsetDateTime now = now();
        RefreshToken current = tokens.findByTokenHashForUpdate(Hashing.sha256Hex(rawRefreshToken))
                .orElseThrow(() -> new RefreshRejectedException(true));
        Long userId = current.getUserId();
        if (!current.isUsable(now)) {
            if (current.isRevokedFor(RevokeReason.ROTATED)) {
                if (current.getRevokedAt().isAfter(now.minus(ROTATION_GRACE))) {
                    throw new RefreshRejectedException(false);
                }
                tokens.revokeAllOfUser(userId, RevokeReason.REUSE_DETECTED, now);
                revocations.revokeAllIssuedBefore(userId, clock.instant());
                audit.record(AuditAction.SESSION_REVOKED, userId, String.valueOf(current.getFamilyId()),
                        Map.of("reason", RevokeReason.REUSE_DETECTED), client, false);
            }
            throw new RefreshRejectedException(true);
        }
        User user = users.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            tokens.revokeFamily(current.getFamilyId(), RevokeReason.SUSPENDED, now);
            throw new RefreshRejectedException(true);
        }
        current.revoke(RevokeReason.ROTATED, now);
        String raw = Hashing.randomToken(REFRESH_TOKEN_BYTES);
        String ip = client != null && client.ip() != null ? client.ip() : current.getIpAddress();
        RefreshToken next = RefreshToken.issue(userId, current.getFamilyId(), Hashing.sha256Hex(raw),
                current.getDeviceInfo(), ip, now.plus(refreshTtl));
        next.touch(now);
        tokens.save(next);
        return new OpenedSession(jwtService.issue(userId, user.getRole(), current.getFamilyId()), raw, refreshTtl,
                current.getFamilyId(), userId);
    }

    /**
     * Đăng xuất phiên hiện tại (API_SPEC 2.7): thu hồi cả family theo claim {@code sid} của access token, hoặc theo
     * cookie khi access token đã hết hạn; đưa access token vào denylist tới khi hết hạn.
     */
    @Transactional
    public void logoutCurrent(AuthUser me, String rawRefreshToken) {
        OffsetDateTime now = now();
        Long familyId = me != null ? me.sessionId() : null;
        if (familyId == null && rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            familyId = tokens.findByTokenHash(Hashing.sha256Hex(rawRefreshToken)).map(RefreshToken::getFamilyId)
                    .orElse(null);
        }
        if (familyId != null) {
            tokens.revokeFamily(familyId, RevokeReason.USER_LOGOUT, now);
        }
        if (me != null && me.expiresAt() != null) {
            revocations.denylist(me.jti(), Duration.between(clock.instant(), me.expiresAt()));
        }
    }

    /** Thiết bị đăng nhập: mỗi family còn hiệu lực một dòng, phiên hiện tại đứng đầu (API_SPEC 3.6). */
    @Transactional(readOnly = true)
    public List<SessionView> listSessions(long userId, Long currentSessionId) {
        Map<Long, RefreshToken> latestByFamily = new LinkedHashMap<>();
        for (RefreshToken t : tokens.findActiveTokens(userId, now())) {
            latestByFamily.putIfAbsent(t.getFamilyId(), t);
        }
        List<SessionView> result = new ArrayList<>();
        for (RefreshToken t : latestByFamily.values()) {
            UserAgentParser.Device device = UserAgentParser.parse(t.getDeviceInfo());
            OffsetDateTime lastActive = t.getLastUsedAt() != null ? t.getLastUsedAt() : t.getCreatedAt();
            result.add(new SessionView(t.getFamilyId(), device.name(), device.type(), null, t.getIpAddress(),
                    lastActive, t.getFamilyId().equals(currentSessionId)));
        }
        result.sort(Comparator.<SessionView, Boolean>comparing(SessionView::isCurrent).reversed()
                .thenComparing(SessionView::lastActiveAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    /** Đăng xuất một thiết bị khác (API_SPEC 3.6). */
    @Transactional
    public void revokeSession(long userId, long sessionId, Long currentSessionId, ClientInfo client) {
        if (currentSessionId != null && currentSessionId == sessionId) {
            throw new BusinessException(ErrorCode.CURRENT_SESSION_NOT_REVOCABLE);
        }
        if (!tokens.existsByUserIdAndFamilyIdAndRevokedAtIsNull(userId, sessionId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy phiên đăng nhập.");
        }
        tokens.revokeFamily(sessionId, RevokeReason.USER_LOGOUT, now());
        audit.record(AuditAction.SESSION_REVOKED, userId, String.valueOf(sessionId), Map.of("scope", "one"), client, true);
    }

    /** Đăng xuất mọi thiết bị khác (refresh token bị thu hồi; access token của máy khác tự hết hạn ≤ 15 phút). */
    @Transactional
    public int revokeOtherSessions(long userId, long currentSessionId, String reason, ClientInfo client) {
        int count = tokens.revokeOtherFamilies(userId, currentSessionId, reason, now());
        audit.record(AuditAction.SESSION_REVOKED, userId, String.valueOf(currentSessionId),
                Map.of("scope", "others", "reason", reason, "count", count), client, true);
        return count;
    }

    /** Thu hồi mọi phiên + chặn ngay mọi access token đang có (đặt lại mật khẩu, đình chỉ, xóa tài khoản). */
    @Transactional
    public void revokeAllSessions(long userId, String reason) {
        tokens.revokeAllOfUser(userId, reason, now());
        revocations.revokeAllIssuedBefore(userId, clock.instant());
        // Báo ngay cho mọi thiết bị đang mở (WebSocket /user/queue/account) — client tự đăng xuất, không phải tải lại trang
        realtime.sendAfterCommit(userId, UserChannel.ACCOUNT, new AccountSignal(AccountSignal.SESSION_REVOKED, reason));
    }

    /** Lịch sử đăng nhập gần đây (kể cả đã thu hồi) — nhận diện thiết bị lạ. */
    @Transactional(readOnly = true)
    public List<RefreshToken> recentLogins(long userId, Duration within) {
        return tokens.findTop50ByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, now().minus(within));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
