package com.genie.admin.banner.application;

import com.genie.admin.banner.domain.HomeBannerRepository;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Ghi lượt hiển thị / bấm banner vào Redis (API_SPEC 5.15; plan banner D5, D6, B05.02): {@code INCRBY banner:{id}:imp|clk}
 * + đánh dấu {@code banner:stats:dirty}; {@link BannerStatsFlushJob} gom vào PostgreSQL mỗi 5 phút. Chỉ nhận banner đang
 * {@code LIVE}; tối đa 50 sự kiện / request, 60 request / phút / IP. Redis lỗi → bỏ qua (số liệu chỉ để tham khảo CTR).
 */
@Service
public class BannerEventService {

    public static final int MAX_EVENTS = 50;
    static final int PER_MINUTE = 60;
    static final String DIRTY = "banner:stats:dirty";
    static final String IDS_KEY = BannerCache.PREFIX + "ids";
    private static final Logger log = LoggerFactory.getLogger(BannerEventService.class);

    public enum EventType { IMPRESSION, CLICK }

    public record Event(Long bannerId, EventType type) {
    }

    private final StringRedisTemplate redis;
    private final RateLimiter rateLimiter;
    private final HomeBannerRepository banners;
    private final Clock clock;

    public BannerEventService(StringRedisTemplate redis, RateLimiter rateLimiter, HomeBannerRepository banners,
                              Clock clock) {
        this.redis = redis;
        this.rateLimiter = rateLimiter;
        this.banners = banners;
        this.clock = clock;
    }

    public static String counterKey(long bannerId, EventType type) {
        return "banner:" + bannerId + (type == EventType.CLICK ? ":clk" : ":imp");
    }

    public void record(List<Event> events, String clientIp) {
        if (events == null || events.isEmpty() || events.size() > MAX_EVENTS) {
            throw InvalidRequestException.of("events", "OUT_OF_RANGE", events == null ? 0 : events.size(),
                    "Mỗi lần gửi từ 1 đến 50 sự kiện.");
        }
        rateLimiter.check("rl:bannerevt:" + (clientIp == null ? "unknown" : clientIp), PER_MINUTE, Duration.ofMinutes(1));
        Set<Long> live = liveIds();
        Map<String, Long> increments = new HashMap<>();
        for (Event e : events) {
            if (e == null || e.bannerId() == null || e.type() == null || !live.contains(e.bannerId())) {
                continue;   // banner lạ / không còn hiển thị: bỏ qua, không báo lỗi (không lộ banner hợp lệ)
            }
            increments.merge(counterKey(e.bannerId(), e.type()), 1L, Long::sum);
        }
        if (increments.isEmpty()) {
            return;
        }
        try {
            redis.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) conn -> {
                var ser = redis.getStringSerializer();
                increments.forEach((key, n) -> conn.stringCommands().incrBy(ser.serialize(key), n));
                increments.keySet().forEach(key -> conn.setCommands().sAdd(ser.serialize(DIRTY),
                        ser.serialize(key.substring("banner:".length(), key.lastIndexOf(':')))));
                return null;
            });
        } catch (Exception ex) {
            log.warn("Bỏ qua {} sự kiện banner vì Redis lỗi: {}", increments.size(), ex.getMessage());
        }
    }

    /** Id banner đang LIVE — cache 60 giây cùng nhóm khóa {@code banner:live:*} (bị xóa khi Admin đổi banner). */
    private Set<Long> liveIds() {
        try {
            Set<String> cached = redis.opsForSet().members(IDS_KEY);
            if (cached != null && !cached.isEmpty()) {
                Set<Long> ids = new HashSet<>();
                cached.stream().filter(s -> !s.isEmpty()).forEach(s -> ids.add(Long.parseLong(s)));
                return ids;
            }
        } catch (Exception e) {
            log.warn("Không đọc được cache id banner: {}", e.getMessage());
        }
        Set<Long> ids = new HashSet<>(banners.findLiveIds(OffsetDateTime.now(clock)));
        try {
            // phần tử rỗng "" để đánh dấu "đã nạp" kể cả khi không có banner nào
            redis.opsForSet().add(IDS_KEY, ids.isEmpty() ? new String[] {""}
                    : ids.stream().map(String::valueOf).toArray(String[]::new));
            redis.expire(IDS_KEY, BannerCache.TTL);
        } catch (Exception e) {
            log.warn("Không ghi được cache id banner: {}", e.getMessage());
        }
        return ids;
    }
}
