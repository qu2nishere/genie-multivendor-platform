package com.genie.auth.otp.infra;

import com.genie.auth.otp.application.OtpChallenge;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.otp.application.OtpStore;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu challenge OTP trong Redis (LUU_TRU 4.2, plan AUTH D4):
 * <ul>
 *   <li>{@code otp:challenge:{id}} — hash {@code purpose, email, user_id, code_hash, attempts, resend_count, expires_at,
 *       d_<khóa>} (dữ liệu kèm), TTL 5 phút;</li>
 *   <li>{@code otp:active:{purpose}:{email}} — id challenge đang sống (mỗi email / mục đích một mã).</li>
 * </ul>
 * Redis lỗi → {@code 503 GNE-SYS-5030} (không có Redis thì không xác minh được OTP).
 */
@Component
public class RedisOtpStore implements OtpStore {

    private static final String KEY = "otp:challenge:";
    private static final String ACTIVE = "otp:active:";
    private static final String DATA_PREFIX = "d_";

    private final StringRedisTemplate redis;

    public RedisOtpStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(OtpChallenge c, Duration ttl) {
        Map<String, String> fields = new HashMap<>();
        fields.put("purpose", c.purpose().name());
        fields.put("email", c.email());
        fields.put("user_id", c.userId() == null ? "" : c.userId().toString());
        fields.put("code_hash", c.codeHash());
        fields.put("attempts", Integer.toString(c.attempts()));
        fields.put("resend_count", Integer.toString(c.resendCount()));
        fields.put("expires_at", Long.toString(c.expiresAt().toEpochMilli()));
        c.data().forEach((k, v) -> fields.put(DATA_PREFIX + k, v));
        run(() -> {
            redis.delete(KEY + c.id());
            hash().putAll(KEY + c.id(), fields);
            redis.expire(KEY + c.id(), ttl);
            return null;
        });
    }

    @Override
    public Optional<OtpChallenge> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> f = run(() -> hash().entries(KEY + id));
        if (f == null || f.isEmpty() || !f.containsKey("purpose")) {
            return Optional.empty();
        }
        Map<String, String> data = new HashMap<>();
        f.forEach((k, v) -> {
            if (k.startsWith(DATA_PREFIX)) {
                data.put(k.substring(DATA_PREFIX.length()), v);
            }
        });
        String userId = f.getOrDefault("user_id", "");
        return Optional.of(new OtpChallenge(id, OtpPurpose.valueOf(f.get("purpose")), f.get("email"),
                userId.isEmpty() ? null : Long.valueOf(userId), f.getOrDefault("code_hash", ""),
                Integer.parseInt(f.getOrDefault("attempts", "0")), Integer.parseInt(f.getOrDefault("resend_count", "0")),
                Instant.ofEpochMilli(Long.parseLong(f.getOrDefault("expires_at", "0"))), data));
    }

    @Override
    public int incrementAttempts(String id) {
        Long value = run(() -> {
            Long v = hash().increment(KEY + id, "attempts", 1);
            Long ttl = redis.getExpire(KEY + id);
            if (ttl != null && ttl < 0) {
                // khóa đã hết hạn giữa chừng và vừa bị HINCRBY tạo lại — không để rác không hạn
                redis.expire(KEY + id, Duration.ofMinutes(5));
            }
            return v;
        });
        return value == null ? 0 : value.intValue();
    }

    @Override
    public boolean delete(String id) {
        Boolean deleted = run(() -> redis.delete(KEY + id));
        return Boolean.TRUE.equals(deleted);
    }

    @Override
    public Optional<String> activeId(OtpPurpose purpose, String email) {
        return Optional.ofNullable(run(() -> redis.opsForValue().get(activeKey(purpose, email))));
    }

    @Override
    public void setActive(OtpPurpose purpose, String email, String id, Duration ttl) {
        run(() -> {
            redis.opsForValue().set(activeKey(purpose, email), id, ttl);
            return null;
        });
    }

    private static String activeKey(OtpPurpose purpose, String email) {
        return ACTIVE + purpose.name() + ":" + email;
    }

    private HashOperations<String, String, String> hash() {
        return redis.opsForHash();
    }

    private <T> T run(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
