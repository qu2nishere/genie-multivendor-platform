// Luật phát thông báo toàn sàn (UC-NOT-05; plan 2026-11-26 thông báo D2, D3, N01.08)
package com.genie.notification.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PlatformNotificationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 26, 9, 0, 0, 0, ZoneOffset.ofHours(7));

    private static PlatformNotification draft() {
        return PlatformNotification.builder()
                .title("Siêu sale 12.12")
                .content("<p>Giảm tới 50%</p>")
                .target(PlatformNotificationTarget.ALL)
                .createdBy(1L)
                .sendAt(NOW.plusHours(2))
                .build();
    }

    @Test
    void newNotificationIsScheduledUntilDispatched() {
        PlatformNotification n = draft();
        assertThat(n.isScheduled()).isTrue();
        assertThat(n.isDispatched()).isFalse();
        assertThat(n.getReachCount()).isZero();
    }

    @Test
    void dispatchFreezesReachAndTime() {
        PlatformNotification n = draft();
        n.dispatch(1_204L, NOW);
        assertThat(n.isDispatched()).isTrue();
        assertThat(n.isScheduled()).isFalse();
        assertThat(n.getReachCount()).isEqualTo(1_204L);
        assertThat(n.getDispatchedAt()).isEqualTo(NOW);
    }

    @Test
    void zeroReachIsAllowed() {
        PlatformNotification n = draft();
        n.dispatch(0L, NOW);
        assertThat(n.isDispatched()).isTrue();
        assertThat(n.getReachCount()).isZero();
    }

    @Test
    void dispatchTwiceIsRejected() {
        PlatformNotification n = draft();
        n.dispatch(10L, NOW);
        assertThatThrownBy(() -> n.dispatch(20L, NOW.plusMinutes(1))).isInstanceOf(IllegalStateException.class);
        assertThat(n.getReachCount()).isEqualTo(10L);
        assertThat(n.getDispatchedAt()).isEqualTo(NOW);
    }

    @Test
    void negativeReachOrMissingTimeIsRejected() {
        assertThatThrownBy(() -> draft().dispatch(-1L, NOW)).isInstanceOf(IllegalArgumentException.class);
        PlatformNotification n = draft();
        assertThatThrownBy(() -> n.dispatch(1L, null)).isInstanceOf(NullPointerException.class);
        assertThat(n.isScheduled()).isTrue();
        assertThat(n.getReachCount()).isZero();
    }
}
