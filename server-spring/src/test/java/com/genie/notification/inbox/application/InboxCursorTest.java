// Con trỏ hộp thư gộp (plan 2026-11-26 thông báo D8, N04.04)
package com.genie.notification.inbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.common.error.InvalidRequestException;
import org.junit.jupiter.api.Test;

class InboxCursorTest {

    @Test
    void roundTrips() {
        InboxCursor c = new InboxCursor(1_780_000_000_123_456L, InboxCursor.PLATFORM, 1047L);
        assertThat(InboxCursor.decode(c.encode())).isEqualTo(c);
        assertThat(c.encode()).doesNotContain("=").doesNotContain("+").doesNotContain("/");
    }

    @Test
    void brokenCursorIsA422OnCursorField() {
        for (String bad : new String[] {"???", "YWJj", InboxCursorTestHelper.b64("1:9:3"), InboxCursorTestHelper.b64("x:0:1")}) {
            assertThatThrownBy(() -> InboxCursor.decode(bad)).isInstanceOf(InvalidRequestException.class);
        }
    }

    @Test
    void orderingIsTotalAcrossSources() {
        InboxCursor c = new InboxCursor(100, InboxCursor.PLATFORM, 50);
        assertThat(c.isAfter(99, InboxCursor.ADMIN, 999)).isTrue();      // thời điểm nhỏ hơn
        assertThat(c.isAfter(101, InboxCursor.PERSONAL, 1)).isFalse();   // thời điểm lớn hơn
        assertThat(c.isAfter(100, InboxCursor.PERSONAL, 999)).isTrue();  // cùng thời điểm, nguồn hạng nhỏ hơn
        assertThat(c.isAfter(100, InboxCursor.ADMIN, 1)).isFalse();      // cùng thời điểm, nguồn hạng lớn hơn
        assertThat(c.isAfter(100, InboxCursor.PLATFORM, 49)).isTrue();
        assertThat(c.isAfter(100, InboxCursor.PLATFORM, 50)).isFalse();  // chính nó
    }

    static final class InboxCursorTestHelper {
        static String b64(String raw) {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
        }
    }
}
