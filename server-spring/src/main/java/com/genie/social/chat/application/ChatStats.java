// Tỉ lệ & thời gian phản hồi chat của shop (plan 2026-11-26 chat D12, M08.03; API_SPEC 8.2 shop_stats)
package com.genie.social.chat.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Một hội thoại "được trả lời" nếu sau tin đầu tiên của khách trong cửa sổ có tin <b>không tự động</b> của shop trong
 * 24 giờ (N7). {@code chatResponseRate} = % được trả lời (2 chữ số lẻ); {@code medianMinutes} = trung vị phút tới tin
 * trả lời đầu. Dưới {@value #MIN_CONVERSATIONS} hội thoại → {@code null} (chưa đủ dữ liệu). Một truy vấn cho nhiều shop.
 */
@Component
public class ChatStats {

    public static final int MIN_CONVERSATIONS = 5;
    static final Duration WINDOW = Duration.ofDays(30);
    static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String CACHE_PREFIX = "chat:stats:";
    private static final String NONE = "-";
    private static final Logger log = LoggerFactory.getLogger(ChatStats.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final Clock clock;

    public ChatStats(NamedParameterJdbcTemplate jdbc, StringRedisTemplate redis, Clock clock) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.clock = clock;
    }

    /** Số liệu một shop trong cửa sổ. */
    public record ResponseStats(int conversations, int answered, Integer medianMinutes) {

        /** % được trả lời, {@code null} khi chưa đủ dữ liệu. */
        public BigDecimal rate() {
            if (conversations < MIN_CONVERSATIONS) {
                return null;
            }
            return BigDecimal.valueOf(answered * 100L).divide(BigDecimal.valueOf(conversations), 2, RoundingMode.HALF_UP);
        }

        public Integer minutes() {
            return conversations < MIN_CONVERSATIONS ? null : medianMinutes;
        }
    }

    /** Thống kê cho các shop (theo {@code users.id} chủ shop) trong {@code [from, to)}; shop không có hội thoại → 0 / 0. */
    public Map<Long, ResponseStats> responseStats(Collection<Long> vendorUserIds, OffsetDateTime from, OffsetDateTime to) {
        Map<Long, ResponseStats> out = new HashMap<>();
        if (vendorUserIds.isEmpty()) {
            return out;
        }
        MapSqlParameterSource p = new MapSqlParameterSource("vids", List.copyOf(vendorUserIds))
                .addValue("from", Timestamp.from(from.toInstant())).addValue("to", Timestamp.from(to.toInstant()));
        jdbc.query("""
                WITH firsts AS (
                    SELECT c.id AS cid, c.vendor_id, MIN(m.created_at) AS asked
                    FROM conversations c
                    JOIN messages m ON m.conversation_id = c.id AND m.sender_id = c.customer_id
                    WHERE c.vendor_id IN (:vids) AND m.created_at >= :from AND m.created_at < :to
                    GROUP BY c.id, c.vendor_id),
                replies AS (
                    SELECT f.vendor_id, f.asked,
                           (SELECT MIN(r.created_at) FROM messages r
                            WHERE r.conversation_id = f.cid AND r.sender_id = f.vendor_id AND r.is_auto = FALSE
                              AND r.created_at >= f.asked) AS replied
                    FROM firsts f)
                SELECT vendor_id, COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE replied <= asked + INTERVAL '24 hours') AS answered,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (replied - asked)) / 60)
                           FILTER (WHERE replied <= asked + INTERVAL '24 hours') AS median_minutes
                FROM replies GROUP BY vendor_id""", p, rs -> {
            double median = rs.getDouble("median_minutes");
            Integer minutes = rs.wasNull() ? null : (int) Math.ceil(median);
            out.put(rs.getLong("vendor_id"), new ResponseStats(rs.getInt("total"), rs.getInt("answered"), minutes));
        });
        for (Long id : vendorUserIds) {
            out.putIfAbsent(id, new ResponseStats(0, 0, null));
        }
        return out;
    }

    /** Trung vị phút phản hồi 30 ngày gần nhất — cache Redis 1 giờ ({@code chat:stats:{vendorUserId}}). */
    public Integer avgResponseMinutes(long vendorUserId) {
        String key = CACHE_PREFIX + vendorUserId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return NONE.equals(cached) ? null : Integer.valueOf(cached);
            }
        } catch (RuntimeException e) {
            log.debug("Bỏ qua cache thống kê chat: {}", e.getMessage());
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        Integer minutes = responseStats(List.of(vendorUserId), now.minus(WINDOW), now).get(vendorUserId).minutes();
        try {
            redis.opsForValue().set(key, minutes == null ? NONE : String.valueOf(minutes), CACHE_TTL);
        } catch (RuntimeException e) {
            log.debug("Không ghi cache thống kê chat: {}", e.getMessage());
        }
        return minutes;
    }
}
