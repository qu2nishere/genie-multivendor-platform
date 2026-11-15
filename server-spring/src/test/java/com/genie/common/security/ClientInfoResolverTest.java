package com.genie.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientInfoResolverTest {

    private final ClientInfoResolver resolver = new ClientInfoResolver(new SecurityProperties("x".repeat(40),
            Duration.ofMinutes(15), Duration.ofDays(7), false, List.of(), List.of("127.0.0.1", "172.16.0.0/12"),
            "p", "i"));

    @Test
    void ignoresForwardedHeaderFromUntrustedClient() {
        var req = new MockHttpServletRequest();
        req.setRemoteAddr("113.161.40.12");
        req.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(resolver.resolve(req).ip()).isEqualTo("113.161.40.12");
    }

    @Test
    void takesRightMostUntrustedAddressBehindProxy() {
        var req = new MockHttpServletRequest();
        req.setRemoteAddr("172.18.0.5");                       // Nginx trong mạng Docker
        req.addHeader("X-Forwarded-For", "6.6.6.6, 113.161.40.12"); // 6.6.6.6 do client tự gửi (giả)

        assertThat(resolver.resolve(req).ip()).isEqualTo("113.161.40.12");
    }

    @Test
    void truncatesUserAgent() {
        var req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        req.addHeader("User-Agent", "a".repeat(800));

        assertThat(resolver.resolve(req).userAgent()).hasSize(ClientInfo.MAX_USER_AGENT);
    }
}
