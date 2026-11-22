// Mã trong liên kết mời nhân viên — Redis (plan B1 D1, mục 3.1–3.3; LUU_TRU cùng kiểu auth:reset:*)
package com.genie.auth.staff.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.Hashing;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu mã lời mời kích hoạt tài khoản nhân viên (UC-ADM-01). Chỉ lưu <b>bản băm SHA-256</b>, hạn 48 giờ, dùng một lần:
 * <ul>
 *   <li>{@code auth:staff_invite:{sha256(token)}} → {@code userId} — tra khi nhân viên mở liên kết;</li>
 *   <li>{@code auth:staff_invite:user:{userId}} → {@code sha256} — để "Gửi lại" vô hiệu mã cũ và để biết lời mời còn
 *       hạn hay đã hết (trạng thái {@code INVITATION_EXPIRED} — plan B1 D5).</li>
 * </ul>
 * Thông tin lời mời (ai, chức danh, quyền, ai mời) nằm ở PostgreSQL; mất khóa Redis chỉ cần Admin gửi lại.
 */
@Component
public class StaffInvitationStore {

    public static final Duration TTL = Duration.ofHours(48);
    public static final String TOKEN_PREFIX = "inv_";
    static final String TOKEN_KEY = "auth:staff_invite:";
    static final String USER_KEY = "auth:staff_invite:user:";

    private final StringRedisTemplate redis;

    public StaffInvitationStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Sinh mã mới cho nhân viên, vô hiệu mã cũ (nếu có). Trả mã thô để ghép vào liên kết email — không lưu mã thô. */
    public String issue(long userId) {
        String token = TOKEN_PREFIX + Hashing.randomToken(32);
        String hash = Hashing.sha256Hex(token);
        try {
            String old = redis.opsForValue().get(USER_KEY + userId);
            if (old != null) {
                redis.delete(TOKEN_KEY + old);
            }
            redis.opsForValue().set(TOKEN_KEY + hash, Long.toString(userId), TTL);
            redis.opsForValue().set(USER_KEY + userId, hash, TTL);
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return token;
    }

    /** Nhân viên của mã còn hạn; mã sai / hết hạn → rỗng. */
    public Optional<Long> resolve(String rawToken) {
        if (rawToken == null || !rawToken.startsWith(TOKEN_PREFIX) || rawToken.length() > 200) {
            return Optional.empty();
        }
        String value;
        try {
            value = redis.opsForValue().get(TOKEN_KEY + Hashing.sha256Hex(rawToken));
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Hủy lời mời của nhân viên (đã kích hoạt / bị xóa). Lỗi Redis bỏ qua — khóa tự hết hạn sau 48 giờ. */
    public void cancel(long userId) {
        try {
            String hash = redis.opsForValue().get(USER_KEY + userId);
            if (hash != null) {
                redis.delete(TOKEN_KEY + hash);
            }
            redis.delete(USER_KEY + userId);
        } catch (DataAccessException e) {
            // khóa tự hết hạn; không chặn thao tác chính
        }
    }

    /**
     * Thời gian còn lại của lời mời theo từng nhân viên — chỉ chứa người còn mã; không có trong kết quả = đã hết hạn.
     * Redis lỗi → ném {@code 503} (không đoán trạng thái).
     */
    public Map<Long, Duration> remaining(Collection<Long> userIds) {
        Map<Long, Duration> result = new HashMap<>();
        try {
            for (Long id : userIds) {
                Long seconds = redis.getExpire(USER_KEY + id);
                if (seconds != null && seconds > 0) {
                    result.put(id, Duration.ofSeconds(seconds));
                }
            }
        } catch (DataAccessException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return result;
    }
}
