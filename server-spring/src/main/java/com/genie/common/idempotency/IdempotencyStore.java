package com.genie.common.idempotency;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Chống gửi trùng cho API tạo tiền ({@code Idempotency-Key} — TECH_STACK 8, API_SPEC 0.2; plan C2 D11). Redis
 * {@code SET NX}: lần đầu giữ khóa ở trạng thái "đang xử lý" ({@value #IN_FLIGHT_SECONDS} giây), xong việc ghi kết quả
 * (vd id phiên checkout) và giữ 24 giờ; gọi lại cùng khóa → trả kết quả cũ thay vì làm lại.
 * <ul>
 *   <li>Khóa đang xử lý (yêu cầu song song) hoặc Redis lỗi → {@code 503 GNE-SYS-5030} — client thử lại với CÙNG khóa;</li>
 *   <li>xử lý lỗi → {@link #abort} xóa khóa để lần thử sau chạy lại từ đầu.</li>
 * </ul>
 * Khóa gắn theo người dùng + phạm vi để hai người không đụng khóa của nhau.
 */
@Component
public class IdempotencyStore {

    static final String PREFIX = "idem:";
    static final String IN_FLIGHT = "IN_FLIGHT";
    static final long IN_FLIGHT_SECONDS = 120;
    static final Duration DONE_TTL = Duration.ofHours(24);
    static final int MAX_KEY_LENGTH = 100;

    private final StringRedisTemplate redis;

    public IdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Bắt đầu xử lý khóa. Trả {@code Optional.empty()} = lần đầu, được làm; có giá trị = đã làm xong trước đó (kết quả cũ).
     *
     * @throws InvalidRequestException thiếu / quá dài header {@code Idempotency-Key}
     * @throws BusinessException       {@code 503 GNE-SYS-5030} khi đang xử lý dở hoặc không kiểm được Redis
     */
    public Optional<String> begin(String scope, long userId, String key) {
        String k = redisKey(scope, userId, key);
        try {
            Boolean fresh = redis.opsForValue().setIfAbsent(k, IN_FLIGHT, Duration.ofSeconds(IN_FLIGHT_SECONDS));
            if (Boolean.TRUE.equals(fresh)) {
                return Optional.empty();
            }
            String v = redis.opsForValue().get(k);
            if (v == null || IN_FLIGHT.equals(v)) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Yêu cầu đang được xử lý, vui lòng thử lại.");
            }
            return Optional.of(v);
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Không kiểm được Idempotency-Key, vui lòng thử lại.");
        }
    }

    /** Ghi kết quả (giữ 24 giờ). Redis lỗi lúc này chỉ làm khóa hết hạn sớm — không ném lỗi sau khi việc đã xong. */
    public void complete(String scope, long userId, String key, String result) {
        try {
            redis.opsForValue().set(redisKey(scope, userId, key), result, DONE_TTL);
        } catch (RuntimeException ignored) {
            // khóa IN_FLIGHT tự hết sau 2 phút; lần gửi lại sau đó sẽ bị coi như lần đầu
        }
    }

    /** Bỏ khóa khi xử lý lỗi để client gửi lại được. */
    public void abort(String scope, long userId, String key) {
        try {
            redis.delete(redisKey(scope, userId, key));
        } catch (RuntimeException ignored) {
            // khóa tự hết hạn
        }
    }

    static String redisKey(String scope, long userId, String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw InvalidRequestException.of("Idempotency-Key", key == null || key.isBlank() ? "REQUIRED" : "TOO_LONG",
                    key, "Cần header Idempotency-Key (tối đa 100 ký tự).");
        }
        return PREFIX + scope + ":" + userId + ":" + key.trim();
    }
}
