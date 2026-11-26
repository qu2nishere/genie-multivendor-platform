package com.genie.order.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.genie.common.error.BusinessException;
import com.genie.order.checkout.application.OrderCodeGenerator;
import com.genie.support.MutableClock;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** Mã đơn {@code TR-yyyyMMdd-######} theo giờ Việt Nam và mã phiên {@code CS-…} (plan C2 D10, O07.07). */
class CheckoutServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void orderCodeUsesVietnamDayAndDailySequence() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment("order:seq:20260609")).thenReturn(1L, 2L);
        // 2026-06-08T18:30Z = 01:30 ngày 09/06 giờ Việt Nam
        OrderCodeGenerator gen = new OrderCodeGenerator(redis, mock(org.springframework.jdbc.core.JdbcTemplate.class), MutableClock.startingAt("2026-06-08T18:30:00Z"));
        assertThat(gen.nextOrderCode()).isEqualTo("TR-20260609-000001");
        assertThat(gen.nextOrderCode()).isEqualTo("TR-20260609-000002");
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisDownMeansServiceUnavailable() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        OrderCodeGenerator gen = new OrderCodeGenerator(redis, mock(org.springframework.jdbc.core.JdbcTemplate.class), MutableClock.startingAt("2026-06-08T07:00:00Z"));
        assertThatThrownBy(gen::nextOrderCode).isInstanceOf(BusinessException.class).hasMessageContaining("mã đơn");
    }

    @Test
    void sessionCodeIsStableBase32() {
        assertThat(OrderCodeGenerator.sessionCode(1)).isEqualTo("CS-00001");
        assertThat(OrderCodeGenerator.sessionCode(8821)).isEqualTo("CS-008KN");
        assertThat(OrderCodeGenerator.sessionCode(8821)).isEqualTo(OrderCodeGenerator.sessionCode(8821));
    }
}
