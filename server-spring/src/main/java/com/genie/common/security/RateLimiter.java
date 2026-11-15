package com.genie.common.security;

import com.genie.common.error.TooManyRequestsException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Giới hạn tần suất bằng bộ đếm Redis {@code INCR} + {@code EXPIRE} (LLD 11.2, API_SPEC 0.11).
 * Vượt → {@link TooManyRequestsException} ({@code 429 GNE-RAT-4290} + {@code Retry-After} = TTL còn lại).
 * <p>Redis lỗi → <b>cho qua</b> và log WARN: rate limit là lớp phòng thủ phụ, không đáng làm sập đăng nhập.
 * (Khác Idempotency-Key — Redis lỗi thì API tiền phải từ chối {@code 503}.)
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Tăng bộ đếm {@code key} trong cửa sổ {@code window}; vượt {@code limit} lần thì ném 429.
     *
     * @param key ví dụ {@code rl:login:113.161.40.12}, {@code rl:otp:mai@x.com}
     */
    public void check(String key, int limit, Duration window) {
        Long count;
        try {
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, window);
            }
        } catch (DataAccessException e) {
            log.warn("Rate limit bỏ qua vì Redis lỗi (key={}): {}", key, e.getMessage());
            return;
        }
        if (count != null && count > limit) {
            throw new TooManyRequestsException(retryAfter(key, window));
        }
    }

    /** Chỉ đọc: đã chạm giới hạn chưa (không tăng bộ đếm). */
    public boolean isExhausted(String key, int limit) {
        try {
            String v = redis.opsForValue().get(key);
            return v != null && Long.parseLong(v) >= limit;
        } catch (DataAccessException | NumberFormatException e) {
            return false;
        }
    }

    /** Xóa bộ đếm (ví dụ sau khi đăng nhập thành công). */
    public void reset(String key) {
        try {
            redis.delete(key);
        } catch (DataAccessException e) {
            log.warn("Không xóa được bộ đếm {}: {}", key, e.getMessage());
        }
    }

    private long retryAfter(String key, Duration window) {
        try {
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                return ttl;
            }
        } catch (DataAccessException ignored) {
            // dùng cả cửa sổ
        }
        return window.toSeconds();
    }
}
