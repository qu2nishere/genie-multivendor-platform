package com.genie.common.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cache đọc qua Redis, giá trị lưu dạng JSON (plan 2026-11-17 C01.02; API_SPEC 0.12; LUU_TRU 4).
 * <p>
 * Dùng thay cho {@code @Cacheable}: kiểu trả về là {@code record} bất biến nên cần chỉ rõ {@link JavaType} khi đọc lại,
 * và mọi lỗi Redis (mất kết nối, JSON hỏng) đều <b>bỏ qua cache, đọc thẳng nguồn</b> + log WARN — cache chỉ để nhanh,
 * không được làm sập trang công khai (plan N7). Khóa luôn có tiền tố {@code cache:} để xóa theo nhóm bằng {@code SCAN}.
 */
@Component
public class JsonCache {

    private static final Logger log = LoggerFactory.getLogger(JsonCache.class);
    static final String PREFIX = "cache:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JsonCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** Kiểu {@link JavaType} cho {@code List<T>} / kiểu lồng — dựng một lần, giữ trong hằng số của service. */
    public JavaType type(Class<?> raw) {
        return objectMapper.getTypeFactory().constructType(raw);
    }

    public JavaType listOf(Class<?> element) {
        return objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, element);
    }

    /**
     * Lấy {@code key} trong cache; không có thì gọi {@code loader}, lưu với {@code ttl} rồi trả.
     * {@code loader} trả {@code null} → không lưu (không cache "không tìm thấy").
     */
    public <T> T getOrLoad(String key, JavaType type, Duration ttl, Supplier<T> loader) {
        String fullKey = PREFIX + key;
        try {
            String cached = redis.opsForValue().get(fullKey);
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception e) {
            log.warn("Bỏ qua cache {} khi đọc: {}", fullKey, e.getMessage());
        }
        T value = loader.get();
        if (value != null) {
            try {
                redis.opsForValue().set(fullKey, objectMapper.writeValueAsString(value), ttl);
            } catch (Exception e) {
                log.warn("Bỏ qua cache {} khi ghi: {}", fullKey, e.getMessage());
            }
        }
        return value;
    }

    /** Xóa một khóa (vd {@code catalog:product:15}). */
    public void evict(String key) {
        try {
            redis.delete(PREFIX + key);
        } catch (Exception e) {
            log.warn("Không xóa được cache {}: {}", key, e.getMessage());
        }
    }

    /** Xóa mọi khóa bắt đầu bằng {@code keyPrefix} (vd {@code catalog:similar:15:}) — dùng SCAN, không dùng KEYS. */
    public void evictPrefix(String keyPrefix) {
        try {
            Set<String> keys = new java.util.HashSet<>();
            try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(PREFIX + keyPrefix + "*").count(500).build())) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Không xóa được nhóm cache {}: {}", keyPrefix, e.getMessage());
        }
    }
}
