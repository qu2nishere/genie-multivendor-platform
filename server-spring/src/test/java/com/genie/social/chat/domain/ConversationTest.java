// plan 2026-11-26 chat M03.01–M03.02
package com.genie.social.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.social.chat.domain.Conversation.Side;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class ConversationTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-06-08T07:00:00Z");

    private static Conversation conv() {
        return Conversation.builder().id(1L).customerId(10L).vendorId(20L).build();
    }

    @Test
    void sideOfAndOtherParty() {
        Conversation c = conv();
        assertThat(c.sideOf(10)).isEqualTo(Side.CUSTOMER);
        assertThat(c.sideOf(20)).isEqualTo(Side.VENDOR);
        assertThat(c.sideOf(30)).isEqualTo(Side.NONE);
        assertThat(c.otherThan(10)).isEqualTo(20);
        assertThat(c.otherThan(20)).isEqualTo(10);
    }

    @Test
    void newMessageRaisesOnlyTheRecipientsCounter() {
        Conversation c = conv();
        c.onNewMessage(Message.text(1, 10, "Còn hàng không?", AT), true);
        c.onNewMessage(Message.text(1, 10, "Shop ơi", AT), true);
        c.onNewMessage(Message.text(1, 20, "Dạ còn", AT.plusMinutes(1)), false);
        assertThat(c.unreadFor(Side.VENDOR)).isEqualTo(2);
        assertThat(c.unreadFor(Side.CUSTOMER)).isEqualTo(1);
        assertThat(c.getLastMessageSnippet()).isEqualTo("Dạ còn");
        assertThat(c.getLastMessageAt()).isEqualTo(AT.plusMinutes(1));

        c.markReadBy(false);
        assertThat(c.unreadFor(Side.VENDOR)).isZero();
        assertThat(c.unreadFor(Side.CUSTOMER)).isEqualTo(1);
        assertThat(c.unreadFor(Side.NONE)).isZero();
    }

    @Test
    void snippetByTypeAndTruncated() {
        assertThat(Conversation.snippetOf(Message.image(1, 10, "conversation/1/a.png", AT))).isEqualTo("[Hình ảnh]");
        assertThat(Conversation.snippetOf(Message.productCard(1, 10, 5, AT))).isEqualTo("[Sản phẩm]");
        assertThat(Conversation.snippetOf(Message.orderCard(1, 10, 44, AT))).isEqualTo("[Đơn hàng]");
        String longText = "a".repeat(150);
        assertThat(Conversation.snippetOf(Message.text(1, 10, longText, AT))).hasSize(100);
        assertThat(Conversation.snippetOf(Message.text(1, 10, "dòng 1\n\n  dòng 2", AT))).isEqualTo("dòng 1 dòng 2");
    }

    @Test
    void messageFactoriesAndStatus() {
        Message auto = Message.auto(1, 20, "Xin chào", AT);
        assertThat(auto.getIsAuto()).isTrue();
        assertThat(auto.getType()).isEqualTo(MessageType.TEXT);
        assertThat(auto.getSenderId()).isEqualTo(20);
        Message m = Message.text(1, 10, "Hi", AT);
        assertThat(m.getIsAuto()).isFalse();
        assertThat(m.getCreatedAt()).isEqualTo(AT);
        assertThat(m.status()).isEqualTo("DELIVERED");
        m.markRead();
        assertThat(m.status()).isEqualTo("SEEN");
        assertThat(Message.image(1, 10, "k", AT).getAttachmentUrl()).isEqualTo("k");
        assertThat(Message.productCard(1, 10, 5, AT).getProductVariantId()).isEqualTo(5);
        assertThat(Message.orderCard(1, 10, 44, AT).getOrderId()).isEqualTo(44);
    }
}
