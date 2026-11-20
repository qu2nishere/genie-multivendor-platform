package com.genie.catalog.stats.application;

import com.genie.catalog.stats.domain.TrafficSource;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.Hashing;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Đếm lượt xem sản phẩm theo nguồn truy cập trên Redis (UC-CAT-06 bước 9, UC-CAT-18 bước 5; API_SPEC 5.7;
 * plan 2026-11-17 thuật toán A10). Không ghi DB trong request — {@link ProductViewFlushJob} gộp 5 phút.
 * <ul>
 *   <li>chống đếm lặp: cùng người xem + cùng sản phẩm chỉ tính 1 lần / {@value #DEDUPE_MINUTES} phút ({@code SET NX EX});</li>
 *   <li>{@code HINCRBY pv:pending {ngày}|{productId}|{nguồn}} — số lượt chờ ghi;</li>
 *   <li>{@code PFADD pv:uv:{ngày}:{productId}:{nguồn} visitorKey} — HyperLogLog đếm khách duy nhất
 *       (~12 KB / khóa, sai số ~0,81%), sống 2 ngày.</li>
 * </ul>
 * {@code visitorKey} = {@code u:{userId}} khi đăng nhập, ngược lại SHA-256(IP + User-Agent) cắt 16 ký tự (không lưu IP thô).
 */
@Component
public class ProductViewCounter {

    static final long DEDUPE_MINUTES = 30;
    public static final String PENDING = "pv:pending";
    public static final String FLUSHING = "pv:flushing";
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Logger log = LoggerFactory.getLogger(ProductViewCounter.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public ProductViewCounter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    public static String uniqueKey(LocalDate day, long productId, TrafficSource source) {
        return "pv:uv:" + day + ":" + productId + ":" + source;
    }

    public static String visitorKey(Long userId, ClientInfo client) {
        if (userId != null) {
            return "u:" + userId;
        }
        return Hashing.sha256Hex((client.ip() == null ? "" : client.ip()) + "|" + (client.userAgent() == null ? "" : client.userAgent()))
                .substring(0, 16);
    }

    /** @return {@code true} khi lượt này được tính (không trùng trong 30 phút) */
    public boolean record(long productId, TrafficSource source, String visitorKey) {
        try {
            Boolean first = redis.opsForValue().setIfAbsent("pv:seen:" + visitorKey + ":" + productId, "1",
                    Duration.ofMinutes(DEDUPE_MINUTES));
            if (!Boolean.TRUE.equals(first)) {
                return false;
            }
            LocalDate day = LocalDate.now(clock.withZone(VN));
            redis.opsForHash().increment(PENDING, day + "|" + productId + "|" + source, 1);
            String uv = uniqueKey(day, productId, source);
            redis.opsForHyperLogLog().add(uv, visitorKey);
            redis.expire(uv, Duration.ofDays(2));
            return true;
        } catch (RuntimeException e) {
            log.warn("Bỏ qua đếm lượt xem vì Redis lỗi: {}", e.getMessage());
            return false;
        }
    }
}
