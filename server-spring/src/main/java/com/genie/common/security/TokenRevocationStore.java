package com.genie.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Vô hiệu access token trước hạn (plan AUTH D8, LUU_TRU 4.2):
 * <ul>
 *   <li>{@code auth:denylist:{jti}} — một token cụ thể (đăng xuất phiên hiện tại), TTL = thời gian sống còn lại;</li>
 *   <li>{@code auth:revoked_before:{userId}} — mọi token có {@code iat} trước mốc này (đổi / đặt lại mật khẩu, đình chỉ,
 *       phát hiện refresh token bị dùng lại), TTL = hạn access token.</li>
 * </ul>
 * Redis lỗi → <b>cho qua</b> (token đã thu hồi dùng được tối đa 15 phút; refresh token vẫn bị chặn vì nằm ở DB —
 * LUU_TRU mục 4 "khi Redis lỗi").
 */
@Component
public class TokenRevocationStore {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationStore.class);
    private static final String DENYLIST = "auth:denylist:";
    private static final String REVOKED_BEFORE = "auth:revoked_before:";

    private final StringRedisTemplate redis;
    private final Duration accessTtl;

    public TokenRevocationStore(StringRedisTemplate redis, SecurityProperties props) {
        this.redis = redis;
        this.accessTtl = props.accessTokenTtl();
    }

    public void denylist(String jti, Duration remaining) {
        if (jti == null || remaining == null || remaining.isNegative() || remaining.isZero()) {
            return;
        }
        try {
            redis.opsForValue().set(DENYLIST + jti, "1", remaining);
        } catch (DataAccessException e) {
            log.warn("Không ghi được denylist cho jti {}: {}", jti, e.getMessage());
        }
    }

    public boolean isDenylisted(String jti) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(DENYLIST + jti));
        } catch (DataAccessException e) {
            log.warn("Không đọc được denylist: {}", e.getMessage());
            return false;
        }
    }

    /** Mọi access token của {@code userId} phát trước {@code at} (theo giây) bị từ chối. */
    public void revokeAllIssuedBefore(long userId, Instant at) {
        try {
            redis.opsForValue().set(REVOKED_BEFORE + userId, Long.toString(at.getEpochSecond()),
                    accessTtl.plusMinutes(1));
        } catch (DataAccessException e) {
            log.warn("Không ghi được revoked_before cho user {}: {}", userId, e.getMessage());
        }
    }

    public Optional<Instant> revokedBefore(long userId) {
        try {
            String v = redis.opsForValue().get(REVOKED_BEFORE + userId);
            return v == null ? Optional.empty() : Optional.of(Instant.ofEpochSecond(Long.parseLong(v)));
        } catch (DataAccessException | NumberFormatException e) {
            log.warn("Không đọc được revoked_before: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Token còn dùng được không (không nằm trong denylist, phát sau mốc thu hồi của user). */
    public boolean isRevoked(AuthUser token) {
        if (isDenylisted(token.jti())) {
            return true;
        }
        return revokedBefore(token.id())
                .map(before -> token.issuedAt() != null && token.issuedAt().isBefore(before))
                .orElse(false);
    }
}
