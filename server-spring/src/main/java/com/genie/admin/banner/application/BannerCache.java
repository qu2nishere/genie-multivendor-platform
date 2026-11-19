package com.genie.admin.banner.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.admin.banner.domain.BannerPosition;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cache banner đang hiển thị (plan banner D5, B04.01; LUU_TRU 4.3): {@code banner:live:{position}} và
 * {@code banner:live:CATEGORY_TOP:{categoryId}} — JSON danh sách đã sắp, TTL 60 giây. Redis lỗi → bỏ qua cache, người gọi
 * đọc PostgreSQL (N6): cache chỉ để tăng tốc, không bao giờ làm sai dữ liệu.
 */
@Component
public class BannerCache {

    static final Duration TTL = Duration.ofSeconds(60);
    static final String PREFIX = "banner:live:";
    private static final Logger log = LoggerFactory.getLogger(BannerCache.class);
    private static final TypeReference<List<PublicBannerView>> LIST = new TypeReference<>() {
    };

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public BannerCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public static String key(BannerPosition position, Long categoryId) {
        return position == BannerPosition.CATEGORY_TOP ? PREFIX + position + ":" + categoryId : PREFIX + position;
    }

    public Optional<List<PublicBannerView>> get(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, LIST));
        } catch (Exception e) {
            log.warn("Bỏ qua cache banner {} (Redis / JSON lỗi): {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String key, List<PublicBannerView> banners) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(banners), TTL);
        } catch (Exception e) {
            log.warn("Không ghi được cache banner {}: {}", key, e.getMessage());
        }
    }

    /** Xóa mọi cache banner: 5 khóa vị trí cố định + {@code CATEGORY_TOP:*} bằng SCAN (không dùng KEYS). */
    public void evictAll() {
        try {
            List<String> keys = new ArrayList<>();
            for (BannerPosition p : BannerPosition.values()) {
                if (p != BannerPosition.CATEGORY_TOP) {
                    keys.add(PREFIX + p);
                }
            }
            keys.add(PREFIX + "ids");
            try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions()
                    .match(PREFIX + BannerPosition.CATEGORY_TOP + ":*").count(200).build())) {
                cursor.forEachRemaining(keys::add);
            }
            redis.delete(keys);
        } catch (Exception e) {
            log.warn("Không xóa được cache banner (sẽ tự hết hạn sau {} giây): {}", TTL.toSeconds(), e.getMessage());
        }
    }
}
