package com.genie.catalog.stats.application;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Danh mục khách đã đăng nhập xem gần đây — nguồn cá nhân hóa cho "Gợi ý cho bạn / Gợi ý hôm nay"
 * (UC-CAT-07 bước 5; API_SPEC 5.5; plan 2026-11-17 A9). Redis LIST {@code recent:cat:{userId}}: {@code LPUSH} mỗi lượt xem,
 * giữ 20 phần tử mới nhất, sống 30 ngày.
 */
@Component
public class RecentCategoryTracker {

    static final int KEEP = 20;
    static final Duration TTL = Duration.ofDays(30);

    private static final Logger log = LoggerFactory.getLogger(RecentCategoryTracker.class);

    private final StringRedisTemplate redis;

    public RecentCategoryTracker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    static String key(long userId) {
        return "recent:cat:" + userId;
    }

    public void viewed(long userId, long categoryId) {
        try {
            String k = key(userId);
            redis.opsForList().leftPush(k, String.valueOf(categoryId));
            redis.opsForList().trim(k, 0, KEEP - 1);
            redis.expire(k, TTL);
        } catch (RuntimeException e) {
            log.warn("Bỏ qua ghi danh mục xem gần đây: {}", e.getMessage());
        }
    }

    /** Tối đa {@code n} danh mục xem nhiều nhất trong 20 lượt gần nhất (bằng nhau thì mới xem trước). */
    public List<Long> topCategories(long userId, int n) {
        try {
            List<String> recent = redis.opsForList().range(key(userId), 0, KEEP - 1);
            if (recent == null || recent.isEmpty()) {
                return List.of();
            }
            Map<Long, Integer> counts = new LinkedHashMap<>();
            for (String s : recent) {
                counts.merge(Long.parseLong(s), 1, Integer::sum);
            }
            return counts.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())   // sort ổn định: giữ thứ tự mới → cũ
                    .limit(n).map(Map.Entry::getKey).toList();
        } catch (RuntimeException e) {
            log.warn("Không đọc được danh mục xem gần đây: {}", e.getMessage());
            return List.of();
        }
    }
}
