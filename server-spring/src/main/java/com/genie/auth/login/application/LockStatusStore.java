package com.genie.auth.login.application;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Lý do khóa tạm ({@code locked_by} ∈ PASSWORD, OTP — API_SPEC 2.5) lưu Redis {@code auth:locked_by:{userId}} cùng
 * hạn với {@code users.locked_until} (plan AUTH D9 — không thêm cột vào entity). Redis lỗi → mặc định PASSWORD.
 */
@Component
public class LockStatusStore {

    public static final String BY_PASSWORD = "PASSWORD";
    public static final String BY_OTP = "OTP";
    private static final String KEY = "auth:locked_by:";
    private static final Logger log = LoggerFactory.getLogger(LockStatusStore.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public LockStatusStore(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    public void markLocked(long userId, String lockedBy, OffsetDateTime until) {
        if (until == null) {
            return;
        }
        Duration ttl = Duration.between(OffsetDateTime.now(clock), until);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            redis.opsForValue().set(KEY + userId, lockedBy, ttl);
        } catch (DataAccessException e) {
            log.warn("Không ghi được locked_by: {}", e.getMessage());
        }
    }

    public String lockedBy(long userId) {
        try {
            String v = redis.opsForValue().get(KEY + userId);
            return v == null ? BY_PASSWORD : v;
        } catch (DataAccessException e) {
            return BY_PASSWORD;
        }
    }

    public void clear(long userId) {
        try {
            redis.delete(KEY + userId);
        } catch (DataAccessException e) {
            log.warn("Không xóa được locked_by: {}", e.getMessage());
        }
    }
}
