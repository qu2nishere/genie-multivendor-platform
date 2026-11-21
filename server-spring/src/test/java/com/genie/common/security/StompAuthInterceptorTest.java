package com.genie.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Kênh STOMP được phép đăng ký (API_SPEC mục 13). */
class StompAuthInterceptorTest {

    @Test
    void onlyOwnQueuesAndPlatformTopic() {
        assertThat(StompAuthInterceptor.allowed("/user/queue/account")).isTrue();
        assertThat(StompAuthInterceptor.allowed("/user/queue/notifications")).isTrue();
        assertThat(StompAuthInterceptor.allowed("/topic/platform")).isTrue();
        assertThat(StompAuthInterceptor.allowed("/queue/account-user123")).isFalse();
        assertThat(StompAuthInterceptor.allowed("/topic/anything")).isFalse();
        assertThat(StompAuthInterceptor.allowed(null)).isFalse();
    }
}
