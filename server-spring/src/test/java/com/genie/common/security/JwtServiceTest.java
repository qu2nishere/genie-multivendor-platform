package com.genie.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.common.domain.UserRole;
import com.genie.support.MutableClock;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-unit-test-secret";

    private final MutableClock clock = MutableClock.startingAt("2026-06-08T07:00:00Z");

    private static SecurityProperties props(String secret) {
        return new SecurityProperties(secret, Duration.ofMinutes(15), Duration.ofDays(7), false,
                List.of("http://localhost:4321"), List.of("127.0.0.1"), "pepper", "internal");
    }

    @Test
    void issuedTokenParsesBackToSameUser() {
        JwtService jwt = new JwtService(props(SECRET), clock);

        JwtService.IssuedAccessToken issued = jwt.issue(20411L, UserRole.VENDOR, 501L);
        AuthUser user = jwt.parse(issued.token());

        assertThat(user.id()).isEqualTo(20411L);
        assertThat(user.role()).isEqualTo(UserRole.VENDOR);
        assertThat(user.sessionId()).isEqualTo(501L);
        assertThat(user.jti()).isEqualTo(issued.jti());
        assertThat(issued.expiresIn()).isEqualTo(900);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(props(SECRET), clock);
        String token = jwt.issue(1L, UserRole.CUSTOMER, 1L).token();

        clock.advance(Duration.ofMinutes(16));

        assertThatThrownBy(() -> jwt.parse(token)).isInstanceOf(JwtService.InvalidAccessTokenException.class);
    }

    @Test
    void tokenSignedWithOtherSecretIsRejected() {
        String token = new JwtService(props(SECRET), clock).issue(1L, UserRole.CUSTOMER, 1L).token();
        JwtService other = new JwtService(props("another-secret-0123456789-another-secret"), clock);

        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(JwtService.InvalidAccessTokenException.class);
    }

    @Test
    void garbageIsRejected() {
        JwtService jwt = new JwtService(props(SECRET), clock);
        assertThatThrownBy(() -> jwt.parse("not-a-jwt")).isInstanceOf(JwtService.InvalidAccessTokenException.class);
    }

    @Test
    void shortSecretFailsAtStartup() {
        assertThatThrownBy(() -> new JwtService(props("too-short"), clock)).isInstanceOf(IllegalStateException.class);
    }
}
