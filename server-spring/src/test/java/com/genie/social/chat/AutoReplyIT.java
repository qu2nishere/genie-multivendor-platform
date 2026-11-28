// Tin tự động khi mở / nhắn tin (plan 2026-11-26 chat M08.05; UC-VND-06, D11) — chủ shop không có kết nối WebSocket
package com.genie.social.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.social.chat.application.ChatQueryService;
import com.genie.social.chat.application.ChatService;
import com.genie.social.chat.application.ChatViews.MessageView;
import com.genie.social.chat.application.SendCommand;
import com.genie.social.chat.domain.Conversation.Side;
import com.genie.support.TestDataBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AutoReplyIT extends ChatTestSupport {

    @Autowired
    ChatService chat;

    @Autowired
    ChatQueryService query;

    private TestDataBuilder.Shop shopWithAutoReply(String greeting, String away) {
        TestDataBuilder.Shop shop = shop();
        jdbc.update("UPDATE vendor_profiles SET auto_reply_enabled = TRUE, auto_reply_greeting = ?, "
                + "auto_reply_offline = ? WHERE id = ?", greeting, away, shop.shopId());
        return shop;
    }

    private List<MessageView> history(long buyer, long conv) {
        return query.messages(buyer, Side.CUSTOMER, conv, null, 50).items();
    }

    private long autoCount(long conv) {
        return jdbc.queryForObject("SELECT count(*) FROM messages WHERE conversation_id = ? AND is_auto", Long.class, conv);
    }

    @Test
    void greetingOnceForANewConversation() {
        TestDataBuilder.Shop shop = shopWithAutoReply("Chào bạn, TechZone giúp gì được ạ?", "Shop vắng");
        long buyer = customer();
        ChatService.Opened opened = chat.open(buyer, shop.shopId());
        long conv = opened.view().id();
        assertThat(opened.view().unreadCount()).isEqualTo(1);
        List<MessageView> msgs = history(buyer, conv);
        assertThat(msgs).hasSize(1);
        assertThat(msgs.get(0).isAuto()).isTrue();
        assertThat(msgs.get(0).sender()).isEqualTo("THEM");
        assertThat(msgs.get(0).text()).isEqualTo("Chào bạn, TechZone giúp gì được ạ?");
        assertThat(jdbc.queryForObject("SELECT sender_id FROM messages WHERE conversation_id = ?", Long.class, conv))
                .isEqualTo(shop.userId());

        chat.open(buyer, shop.shopId());
        assertThat(autoCount(conv)).isEqualTo(1);
        // mở lại sau 25 giờ không có tin → chào lại
        clock.advance(Duration.ofHours(25));
        chat.open(buyer, shop.shopId());
        assertThat(autoCount(conv)).isEqualTo(2);
    }

    @Test
    void awayReplyAtNightAtMostOncePerHour() {
        TestDataBuilder.Shop shop = shopWithAutoReply(null, "Shop đang ngoài giờ, sẽ trả lời từ 8:00 nhé!");
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        assertThat(autoCount(conv)).isZero();   // không có lời chào

        clock.set(Instant.parse("2026-06-08T16:00:00Z"));   // 23:00 giờ VN
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Shop ơi còn hàng không?", null));
        List<MessageView> msgs = history(buyer, conv);
        assertThat(msgs).extracting(MessageView::isAuto).containsExactly(true, false);   // mới nhất trước
        assertThat(msgs.get(0).text()).isEqualTo("Shop đang ngoài giờ, sẽ trả lời từ 8:00 nhé!");

        clock.advance(Duration.ofMinutes(30));
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Shop ơi", null));
        assertThat(autoCount(conv)).isEqualTo(1);
        clock.advance(Duration.ofMinutes(31));
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Shop ơi ơi", null));
        assertThat(autoCount(conv)).isEqualTo(2);

        // tin của shop không kích hoạt tin vắng mặt
        clock.advance(Duration.ofHours(2));
        chat.send(shop.userId(), Side.VENDOR, conv, SendCommand.text("Dạ shop đây", null));
        assertThat(autoCount(conv)).isEqualTo(2);
        // bộ đếm: khách chưa đọc = 2 tin vắng mặt + 1 tin shop
        assertThat(query.unreadTotal(buyer, Side.CUSTOMER)).isEqualTo(3);
    }

    @Test
    void ownerOfflineDuringTheDayAlsoTriggersTheAwayReply() {
        TestDataBuilder.Shop shop = shopWithAutoReply(null, "Shop sẽ phản hồi sớm");
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Hỏi chút", null));   // 14:00 VN, chủ shop offline
        assertThat(autoCount(conv)).isEqualTo(1);
    }

    @Test
    void vacationMessageEvenWhenAutoReplyIsOff() {
        OffsetDateTime now = OffsetDateTime.parse(START);
        TestDataBuilder.Shop shop = data.shop().vacation(now.minusDays(1), now.plusDays(3)).create();
        jdbc.update("UPDATE vendor_profiles SET vacation_message = 'Shop nghỉ phép tới 11/6' WHERE id = ?", shop.shopId());
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Còn hàng không?", null));
        assertThat(history(buyer, conv).get(0).text()).isEqualTo("Shop nghỉ phép tới 11/6");
    }

    @Test
    void nothingWhenAutoReplyIsOff() {
        TestDataBuilder.Shop shop = shop();
        jdbc.update("UPDATE vendor_profiles SET auto_reply_enabled = FALSE, auto_reply_greeting = 'Chào', "
                + "auto_reply_offline = 'Vắng' WHERE id = ?", shop.shopId());
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        clock.set(Instant.parse("2026-06-08T16:00:00Z"));
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Ai đó không?", null));
        assertThat(autoCount(conv)).isZero();
        // tin tự động không tạo thông báo cho chủ shop; tin của khách thì có (chủ shop offline)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ? AND type = 'CHAT_MESSAGE'",
                Integer.class, shop.userId())).isEqualTo(1);
    }
}
