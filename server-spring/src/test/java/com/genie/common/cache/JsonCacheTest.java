package com.genie.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * "Redis lỗi → đọc thẳng CSDL" (UC-CAT-01 bước 3; API_SPEC 0.12; plan 2026-11-24 H06.03): mọi cache công khai
 * (cây danh mục, banner, sản phẩm, voucher shop) đi qua {@link JsonCache}, nên lỗi Redis không được làm hỏng phản hồi.
 */
class JsonCacheTest {

    private record Row(long id, String name) {
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);

    private JsonCache cacheWith(StringRedisTemplate redis) {
        return new JsonCache(redis, mapper);
    }

    private StringRedisTemplate redis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenReturn(ops);
        return redis;
    }

    @Test
    void hitReturnsCachedValueWithoutLoading() throws Exception {
        when(ops.get("cache:catalog:categories:all")).thenReturn(mapper.writeValueAsString(List.of(new Row(1, "Điện tử"))));
        JsonCache cache = cacheWith(redis());
        AtomicInteger loads = new AtomicInteger();

        List<Row> rows = cache.getOrLoad("catalog:categories:all", cache.listOf(Row.class), Duration.ofHours(1),
                () -> { loads.incrementAndGet(); return List.of(); });

        assertThat(rows).containsExactly(new Row(1, "Điện tử"));
        assertThat(loads).hasValue(0);
    }

    @Test
    void missLoadsAndStoresWithTtl() throws Exception {
        JsonCache cache = cacheWith(redis());

        List<Row> rows = cache.getOrLoad("catalog:categories:all", cache.listOf(Row.class), Duration.ofHours(1),
                () -> List.of(new Row(2, "Thời trang")));

        assertThat(rows).containsExactly(new Row(2, "Thời trang"));
        verify(ops).set(eq("cache:catalog:categories:all"), anyString(), eq(Duration.ofHours(1)));
    }

    @Test
    void redisDownOnReadAndWriteFallsBackToTheSource() {
        when(ops.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        doThrow(new RedisConnectionFailureException("down")).when(ops).set(anyString(), anyString(), any(Duration.class));
        JsonCache cache = cacheWith(redis());

        List<Row> rows = cache.getOrLoad("catalog:categories:all", cache.listOf(Row.class), Duration.ofHours(1),
                () -> List.of(new Row(3, "Nhà cửa")));

        assertThat(rows).containsExactly(new Row(3, "Nhà cửa"));
    }

    @Test
    void corruptedJsonIsIgnoredAndReloaded() {
        when(ops.get(anyString())).thenReturn("{không phải json");
        JsonCache cache = cacheWith(redis());

        List<Row> rows = cache.getOrLoad("catalog:categories:all", cache.listOf(Row.class), Duration.ofHours(1),
                () -> List.of(new Row(4, "Sách")));

        assertThat(rows).containsExactly(new Row(4, "Sách"));
    }

    @Test
    void nullFromTheLoaderIsNotCached() {
        JsonCache cache = cacheWith(redis());

        Row none = cache.getOrLoad("catalog:product:9", cache.type(Row.class), Duration.ofMinutes(5), () -> null);

        assertThat(none).isNull();
        verify(ops, org.mockito.Mockito.never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void evictNeverThrowsWhenRedisIsDown() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.delete(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        when(redis.scan(any())).thenThrow(new RedisConnectionFailureException("down"));
        JsonCache cache = cacheWith(redis);

        assertThatCode(() -> cache.evict("catalog:categories:all")).doesNotThrowAnyException();
        assertThatCode(() -> cache.evictPrefix("catalog:")).doesNotThrowAnyException();
    }
}
