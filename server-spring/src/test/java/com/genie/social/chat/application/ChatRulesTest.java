// plan 2026-11-26 chat M03.04, M03.06, D4, D6
package com.genie.social.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.common.error.InvalidRequestException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ChatRulesTest {

    @Test
    void cursorRoundTripsAndRejectsGarbage() {
        ChatCursor c = new ChatCursor(OffsetDateTime.parse("2026-06-08T07:00:00.123456Z"), 99);
        assertThat(ChatCursor.decode(c.encode(), "before")).isEqualTo(c);
        assertThatThrownBy(() -> ChatCursor.decode("!!!", "before")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> ChatCursor.decode("bm9wZQ", "cursor")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void limitRange() {
        assertThat(ChatCursor.limit(null, 30, 50)).isEqualTo(30);
        assertThat(ChatCursor.limit(50, 30, 50)).isEqualTo(50);
        assertThatThrownBy(() -> ChatCursor.limit(0, 30, 50)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> ChatCursor.limit(51, 30, 50)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void textIsTrimmedStrippedOfControlCharsAndBounded() {
        assertThat(ChatService.normalizeText("  Còn hàng\r\nkhông?\u0007  ")).isEqualTo("Còn hàng\nkhông?");
        assertThat(ChatService.normalizeText("a".repeat(2000))).hasSize(2000);
        assertThatThrownBy(() -> ChatService.normalizeText("   \u0000 ")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> ChatService.normalizeText(null)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> ChatService.normalizeText("a".repeat(2001))).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void clientMessageIdMustBeUuid() {
        assertThat(ChatIdempotency.normalize(null)).isNull();
        assertThat(ChatIdempotency.normalize(" ")).isNull();
        assertThat(ChatIdempotency.normalize("3F2504E0-4F89-11D3-9A0C-0305E82C3301"))
                .isEqualTo("3f2504e0-4f89-11d3-9a0c-0305e82c3301");
        assertThatThrownBy(() -> ChatIdempotency.normalize("abc")).isInstanceOf(InvalidRequestException.class);
    }
}
