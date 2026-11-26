package com.genie.order.checkout.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Mã đơn {@code TR-yyyyMMdd-######} (UC-ORD-07 bước 4; ENTITY 5.2; plan C2 D10): số thứ tự trong ngày lấy từ Redis
 * {@code INCR order:seq:{yyyyMMdd}} (hết hạn 48 giờ), ngày theo giờ Việt Nam. Redis lỗi → {@code 503 GNE-SYS-5030} (client
 * thử lại cùng {@code Idempotency-Key}). Mã phiên {@code CS-xxxxx} suy ra từ id phiên (Crockford base32) — không cần cột.
 */
@Component
public class OrderCodeGenerator {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    static final Duration SEQ_TTL = Duration.ofHours(48);
    private static final char[] BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** Số lần bỏ qua mã đã tồn tại liên tiếp trước khi báo lỗi. */
    static final int MAX_SKIPS = 20;

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public OrderCodeGenerator(StringRedisTemplate redis, JdbcTemplate jdbc, Clock clock) {
        this.redis = redis;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Mã kế tiếp. Khóa đếm của ngày mới tạo (lần đầu trong ngày, hoặc Redis đã mất dữ liệu) → nối tiếp số lớn nhất đã có
     * trong {@code orders} của ngày đó; mã vẫn trùng (nhiều luồng cùng khởi tạo) → lấy số kế tiếp.
     */
    public String nextOrderCode() {
        String day = clock.instant().atZone(VN).toLocalDate().format(DAY);
        String key = "order:seq:" + day;
        for (int i = 0; i < MAX_SKIPS; i++) {
            String code = format(day, increment(key, day));
            Boolean taken = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM orders WHERE order_code = ?)",
                    Boolean.class, code);
            if (!Boolean.TRUE.equals(taken)) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Không sinh được mã đơn, vui lòng thử lại.");
    }

    private long increment(String key, String day) {
        Long seq;
        try {
            seq = redis.opsForValue().increment(key);
            if (seq != null && seq == 1L) {
                redis.expire(key, SEQ_TTL);
                long max = maxSequenceInDb(day);
                if (max > 0) {
                    seq = redis.opsForValue().increment(key, max);
                }
            }
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Không sinh được mã đơn, vui lòng thử lại.");
        }
        if (seq == null) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Không sinh được mã đơn, vui lòng thử lại.");
        }
        return seq;
    }

    private long maxSequenceInDb(String day) {
        String max = jdbc.queryForObject("SELECT max(order_code) FROM orders WHERE order_code LIKE ?", String.class,
                "TR-" + day + "-%");
        if (max == null) {
            return 0;
        }
        try {
            return Long.parseLong(max.substring(max.lastIndexOf('-') + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String format(String day, long seq) {
        return "TR-" + day + "-" + String.format("%06d", seq);
    }

    /** {@code CS-} + id phiên ở Crockford base32, tối thiểu 5 ký tự (vd id 8821 → {@code CS-008KN}). */
    public static String sessionCode(long sessionId) {
        StringBuilder sb = new StringBuilder();
        long v = sessionId;
        do {
            sb.append(BASE32[(int) (v & 31)]);
            v >>>= 5;
        } while (v > 0);
        while (sb.length() < 5) {
            sb.append('0');
        }
        return "CS-" + sb.reverse();
    }
}
