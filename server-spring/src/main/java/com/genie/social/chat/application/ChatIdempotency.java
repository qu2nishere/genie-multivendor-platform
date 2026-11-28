// Chống gửi trùng theo client_message_id (plan 2026-11-26 chat D4, M03.06; API_SPEC 8.4)
package com.genie.social.chat.application;

import com.genie.common.error.InvalidRequestException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis {@code chat:cmid:{userId}:{uuid}}: giữ chỗ bằng {@code SET NX "0"} trước khi ghi tin, sau commit đổi thành
 * {@code message_id} (24 giờ). Gửi lại cùng uuid (STOMP rồi REST khi mạng chập chờn) → trả lại tin đã lưu. Không lưu
 * CSDL (bảng không có cột). Redis lỗi → bỏ qua chống trùng, vẫn gửi (chỉ là lớp phụ).
 */
@Component
public class ChatIdempotency {

    static final Duration TTL = Duration.ofHours(24);
    private static final String PENDING = "0";
    private static final Logger log = LoggerFactory.getLogger(ChatIdempotency.class);

    private final StringRedisTemplate redis;

    public ChatIdempotency(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Kết quả giữ chỗ: {@code fresh} = được ghi tin mới; ngược lại {@code existingId} (có thể rỗng khi đang ghi dở). */
    public record Claim(boolean fresh, Optional<Long> existingId) {
        static final Claim FRESH = new Claim(true, Optional.empty());
    }

    /** Chuẩn hóa / kiểm định dạng UUID — sai → 422 {@code client_message_id}. {@code null} / rỗng → {@code null}. */
    public static String normalize(String cmid) {
        if (cmid == null || cmid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(cmid.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw InvalidRequestException.of("client_message_id", "INVALID_FORMAT", cmid, "client_message_id phải là UUID.");
        }
    }

    public Claim claim(long userId, String cmid) {
        if (cmid == null) {
            return Claim.FRESH;
        }
        String key = key(userId, cmid);
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(key, PENDING, TTL);
            if (Boolean.TRUE.equals(ok)) {
                return Claim.FRESH;
            }
            // đã có: chờ ngắn nếu lần gửi trước còn đang ghi (STOMP + REST gần như cùng lúc)
            for (int i = 0; i < 20; i++) {
                String v = redis.opsForValue().get(key);
                if (v == null) {
                    return claim(userId, cmid);   // lần trước rollback và đã xóa khóa
                }
                if (!PENDING.equals(v)) {
                    return new Claim(false, Optional.of(Long.parseLong(v)));
                }
                Thread.sleep(50);
            }
            return new Claim(false, Optional.empty());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Claim(false, Optional.empty());
        } catch (RuntimeException e) {
            log.warn("Bỏ qua chống trùng tin chat (Redis lỗi): {}", e.getMessage());
            return Claim.FRESH;
        }
    }

    /** Sau commit: gắn uuid với tin đã lưu. */
    public void complete(long userId, String cmid, long messageId) {
        if (cmid == null) {
            return;
        }
        try {
            redis.opsForValue().set(key(userId, cmid), String.valueOf(messageId), TTL);
        } catch (RuntimeException e) {
            log.warn("Không ghi được client_message_id: {}", e.getMessage());
        }
    }

    /** Rollback / lỗi nghiệp vụ: nhả chỗ để client gửi lại được. */
    public void release(long userId, String cmid) {
        if (cmid == null) {
            return;
        }
        try {
            redis.delete(key(userId, cmid));
        } catch (RuntimeException e) {
            log.debug("Không xóa được khóa chống trùng: {}", e.getMessage());
        }
    }

    private static String key(long userId, String cmid) {
        return "chat:cmid:" + userId + ":" + cmid;
    }
}
