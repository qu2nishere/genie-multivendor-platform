package com.genie.auth.token.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T07:00:00Z");

    @Test
    void issueTruncatesUserAgentAndFillsMissingValues() {
        RefreshToken t = RefreshToken.issue(1L, 10L, "hash", "u".repeat(600), null, NOW.plusDays(7));

        assertThat(t.getDeviceInfo()).hasSize(RefreshToken.MAX_DEVICE_INFO);
        assertThat(t.getIpAddress()).isEqualTo("0.0.0.0");
        assertThat(t.getFamilyId()).isEqualTo(10L);
        assertThat(t.isUsable(NOW)).isTrue();
    }

    @Test
    void revokedOrExpiredTokenIsNotUsable() {
        RefreshToken t = RefreshToken.issue(1L, 10L, "hash", "ua", "1.1.1.1", NOW.plusDays(7));
        assertThat(t.isUsable(NOW.plusDays(7))).isFalse();

        t.revoke(RevokeReason.ROTATED, NOW);
        assertThat(t.isUsable(NOW)).isFalse();
        assertThat(t.isRevokedFor(RevokeReason.ROTATED)).isTrue();
    }
}
