package com.genie.admin.banner.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.admin.banner.domain.BannerPosition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis lỗi không làm hỏng việc đọc banner (plan banner N6, B04.05): cache trả rỗng, ghi / xóa không ném lỗi. */
class BannerCacheTest {

    @Test
    void redisFailureIsIgnored() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        when(redis.scan(any())).thenThrow(new RedisConnectionFailureException("down"));
        BannerCache cache = new BannerCache(redis, new ObjectMapper());

        assertThat(cache.get("banner:live:POPUP")).isEmpty();
        assertThatCode(() -> cache.put("banner:live:POPUP", List.of(new PublicBannerView(1, "a", "u", null, "/"))))
                .doesNotThrowAnyException();
        assertThatCode(cache::evictAll).doesNotThrowAnyException();
    }

    @Test
    void keysFollowPositionAndCategory() {
        assertThat(BannerCache.key(BannerPosition.POPUP, 5L)).isEqualTo("banner:live:POPUP");
        assertThat(BannerCache.key(BannerPosition.CATEGORY_TOP, 5L)).isEqualTo("banner:live:CATEGORY_TOP:5");
    }
}
