// Lần hoạt động cuối của người dùng trên WebSocket (plan 2026-11-26 chat D13, M02.06; API_SPEC 8.1 last_active_at)
package com.genie.common.realtime;

import com.genie.common.security.StompAuthInterceptor.StompUser;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Mỗi lần một phiên STOMP ngắt, ghi Redis {@code presence:last:{userId}} = thời điểm ngắt (epoch giây, TTL 30 ngày) để
 * màn chat hiện "Hoạt động x phút trước". Đang có kết nối thì {@link OnlineUsers} trả "đang hoạt động" — khóa này chỉ có
 * nghĩa khi người đó offline. Redis lỗi → bỏ qua (chỉ là thông tin hiển thị).
 */
@Component
public class PresenceListener {

    static final String KEY_PREFIX = "presence:last:";
    static final Duration TTL = Duration.ofDays(30);

    private static final Logger log = LoggerFactory.getLogger(PresenceListener.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public PresenceListener(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user instanceof StompUser stomp && stomp.user() != null && stomp.user().id() != null) {
            touch(stomp.user().id());
        }
    }

    /** Ghi thời điểm hoạt động cuối = bây giờ. */
    public void touch(long userId) {
        try {
            redis.opsForValue().set(KEY_PREFIX + userId, String.valueOf(clock.instant().getEpochSecond()), TTL);
        } catch (RuntimeException e) {
            log.debug("Không ghi được presence của {}: {}", userId, e.getMessage());
        }
    }

    /** Thời điểm ngắt kết nối gần nhất của từng người (không có / Redis lỗi → không có khóa trong kết quả). */
    public Map<Long, OffsetDateTime> lastActive(Collection<Long> userIds) {
        Map<Long, OffsetDateTime> out = new HashMap<>();
        if (userIds.isEmpty()) {
            return out;
        }
        List<Long> ids = userIds.stream().distinct().toList();
        try {
            List<String> values = redis.opsForValue().multiGet(ids.stream().map(id -> KEY_PREFIX + id).toList());
            if (values == null) {
                return out;
            }
            for (int i = 0; i < ids.size(); i++) {
                String v = values.get(i);
                if (v != null) {
                    try {
                        out.put(ids.get(i), Instant.ofEpochSecond(Long.parseLong(v)).atOffset(ZoneOffset.UTC));
                    } catch (NumberFormatException ignored) {
                        // giá trị hỏng — coi như không biết
                    }
                }
            }
        } catch (RuntimeException e) {
            log.debug("Không đọc được presence: {}", e.getMessage());
        }
        return out;
    }
}
