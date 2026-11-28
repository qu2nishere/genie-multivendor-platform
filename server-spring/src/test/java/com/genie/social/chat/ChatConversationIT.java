// Mở hội thoại, gửi tin chữ, đánh dấu đã xem — tầng service (plan 2026-11-26 chat M03.08–M03.10)
package com.genie.social.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.TooManyRequestsException;
import com.genie.social.chat.application.ChatQueryService;
import com.genie.social.chat.application.ChatService;
import com.genie.social.chat.application.ChatViews.MessageView;
import com.genie.social.chat.application.SendCommand;
import com.genie.social.chat.domain.Conversation.Side;
import com.genie.support.TestDataBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatConversationIT extends ChatTestSupport {

    @Autowired
    ChatService chat;

    @Autowired
    ChatQueryService query;

    private static ErrorCode codeOf(Throwable t) {
        return ((BusinessException) t).getErrorCode();
    }

    // ===== Mở hội thoại (M03.08) =====

    @Test
    void openCreatesOnceAndReturnsTheSameConversation() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        ChatService.Opened first = chat.open(buyer, shop.shopId());
        ChatService.Opened again = chat.open(buyer, shop.shopId());
        assertThat(first.created()).isTrue();
        assertThat(again.created()).isFalse();
        assertThat(again.view().id()).isEqualTo(first.view().id());
        assertThat(first.view().counterpart().type()).isEqualTo("SHOP");
        assertThat(first.view().counterpart().id()).isEqualTo(shop.shopId());
        assertThat(first.view().counterpart().name()).isEqualTo(shop.name());
        assertThat(first.view().unreadCount()).isZero();
        // hội thoại chưa có tin không hiện trong hộp thư (D16)
        assertThat(query.conversations(buyer, Side.CUSTOMER, null, null).page().items()).isEmpty();
    }

    @Test
    void concurrentOpensProduceOneRow() throws Exception {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<ChatService.Opened>> futures = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                futures.add(pool.submit(() -> chat.open(buyer, shop.shopId())));
            }
            Set<Long> ids = new HashSet<>();
            int created = 0;
            for (Future<ChatService.Opened> f : futures) {
                ChatService.Opened o = f.get();
                ids.add(o.view().id());
                created += o.created() ? 1 : 0;
            }
            assertThat(ids).hasSize(1);
            assertThat(created).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conversations WHERE customer_id = ?", Integer.class, buyer))
                .isEqualTo(1);
    }

    @Test
    void openRejectsUnavailableAndOwnShop() {
        long buyer = customer();
        TestDataBuilder.Shop pending = data.shop().kyc("PENDING").create();
        TestDataBuilder.Shop suspended = data.shop().userStatus("SUSPENDED").create();
        TestDataBuilder.Shop mine = shop();
        assertThatThrownBy(() -> chat.open(buyer, pending.shopId())).satisfies(
                e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SHOP_UNAVAILABLE));
        assertThatThrownBy(() -> chat.open(buyer, suspended.shopId())).satisfies(
                e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SHOP_UNAVAILABLE));
        assertThatThrownBy(() -> chat.open(buyer, -1L)).satisfies(
                e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SHOP_UNAVAILABLE));
        assertThatThrownBy(() -> chat.open(mine.userId(), mine.shopId())).satisfies(
                e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.CHAT_OWN_SHOP));
        // chủ shop chat với shop KHÁC được (D5)
        assertThat(chat.open(mine.userId(), shop().shopId()).created()).isTrue();
    }

    // ===== Gửi tin (M03.09) =====

    @Test
    void textMessagesFlowBothWaysWithCountersOnTheRightSide() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();

        MessageView sent = chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("  Còn màu đen không?  ", null)).view();
        assertThat(sent.sender()).isEqualTo("ME");
        assertThat(sent.text()).isEqualTo("Còn màu đen không?");
        assertThat(sent.status()).isEqualTo("DELIVERED");
        assertThat(sent.isAuto()).isFalse();
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Shop ơi", null));
        chat.send(shop.userId(), Side.VENDOR, conv, SendCommand.text("Dạ còn bạn nhé!", null));

        var buyerInbox = query.conversations(buyer, Side.CUSTOMER, null, null);
        assertThat(buyerInbox.unreadTotal()).isEqualTo(1);
        var row = buyerInbox.page().items().get(0);
        assertThat(row.unreadCount()).isEqualTo(1);
        assertThat(row.lastMessage().preview()).isEqualTo("Dạ còn bạn nhé!");
        assertThat(row.lastMessage().type()).isEqualTo("TEXT");

        var shopInbox = query.conversations(shop.userId(), Side.VENDOR, null, null);
        assertThat(shopInbox.unreadTotal()).isEqualTo(2);
        assertThat(shopInbox.page().items().get(0).counterpart().type()).isEqualTo("CUSTOMER");
        assertThat(shopInbox.page().items().get(0).counterpart().id()).isEqualTo(buyer);

        List<MessageView> seenByShop = query.messages(shop.userId(), Side.VENDOR, conv, null, null).items();
        assertThat(seenByShop).extracting(MessageView::sender).containsExactly("ME", "THEM", "THEM");
        assertThat(seenByShop.get(2).text()).isEqualTo("Còn màu đen không?");
    }

    @Test
    void concurrentSendsFromBothSidesKeepCountersExact() throws Exception {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<Object>> jobs = new ArrayList<>();
            for (int i = 0; i < 18; i++) {
                int n = i;
                jobs.add(() -> chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("khách " + n, null)));
            }
            for (int i = 0; i < 12; i++) {
                int n = i;
                jobs.add(() -> chat.send(shop.userId(), Side.VENDOR, conv, SendCommand.text("shop " + n, null)));
            }
            for (Future<Object> f : pool.invokeAll(jobs)) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
        var counts = jdbc.queryForMap("SELECT customer_unread_count, vendor_unread_count FROM conversations WHERE id = ?",
                conv);
        assertThat(counts.get("vendor_unread_count")).isEqualTo(18);
        assertThat(counts.get("customer_unread_count")).isEqualTo(12);
    }

    @Test
    void invalidTextIsRejected() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        assertThatThrownBy(() -> chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("   ", null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("x".repeat(2001), null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("hi", "not-a-uuid")))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM messages WHERE conversation_id = ?", Integer.class, conv))
                .isZero();
    }

    @Test
    void resendingTheSameClientMessageIdReturnsTheStoredMessage() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        String cmid = UUID.randomUUID().toString();
        ChatService.Sent first = chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Một lần thôi", cmid));
        ChatService.Sent again = chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Một lần thôi", cmid));
        assertThat(first.created()).isTrue();
        assertThat(again.created()).isFalse();
        assertThat(again.view().id()).isEqualTo(first.view().id());
        assertThat(again.view().clientMessageId()).isEqualTo(cmid);
        assertThat(jdbc.queryForObject("SELECT vendor_unread_count FROM conversations WHERE id = ?", Integer.class, conv))
                .isEqualTo(1);
    }

    @Test
    void the21stMessageInTenSecondsIsRateLimited() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        for (int i = 0; i < 20; i++) {
            chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("tin " + i, null));
        }
        String cmid = UUID.randomUUID().toString();
        assertThatThrownBy(() -> chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("quá nhanh", cmid)))
                .isInstanceOf(TooManyRequestsException.class);
        // khóa chống trùng đã được nhả → lần sau (hết cửa sổ) gửi lại cùng uuid vẫn tạo tin
        assertThat(redis.hasKey("chat:cmid:" + buyer + ":" + cmid)).isFalse();
    }

    @Test
    void outsidersAndWrongSideGetNotFound() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long stranger = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Xin chào", null));

        assertThatThrownBy(() -> chat.send(stranger, Side.CUSTOMER, conv, SendCommand.text("chen ngang", null)))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.NOT_FOUND));
        assertThatThrownBy(() -> query.messages(stranger, Side.CUSTOMER, conv, null, null))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.NOT_FOUND));
        // chủ shop không đọc hội thoại của mình qua đường "khách"
        assertThatThrownBy(() -> query.messages(shop.userId(), Side.CUSTOMER, conv, null, null))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.NOT_FOUND));
        assertThatThrownBy(() -> chat.read(stranger, null, conv))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void sendingToADeactivatedShopIsBlockedButHistoryStaysReadable() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Trước khi khóa", null));
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", shop.userId());

        assertThatThrownBy(() -> chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Sau khi khóa", null)))
                .satisfies(e -> assertThat(codeOf(e)).isEqualTo(ErrorCode.SHOP_UNAVAILABLE));
        assertThat(query.messages(buyer, Side.CUSTOMER, conv, null, null).items()).hasSize(1);
    }

    @Test
    void messageCursorWalksThreePagesWithoutOverlap() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        for (int i = 0; i < 12; i++) {
            chat.send(i % 2 == 0 ? buyer : shop.userId(), null, conv, SendCommand.text("tin " + i, null));
            clock.advance(java.time.Duration.ofSeconds(i % 3 == 0 ? 0 : 1));   // có tin trùng thời điểm
        }
        Set<Long> seen = new HashSet<>();
        String before = null;
        int pages = 0;
        do {
            var page = query.messages(buyer, Side.CUSTOMER, conv, before, 5);
            page.items().forEach(m -> assertThat(seen.add(m.id())).isTrue());
            before = page.pagination().nextCursor();
            pages++;
        } while (before != null);
        assertThat(pages).isEqualTo(3);
        assertThat(seen).hasSize(12);
        assertThatThrownBy(() -> query.messages(buyer, Side.CUSTOMER, conv, "rác", 5))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void inboxIsNewestFirstAndPaginated() {
        long buyer = customer();
        List<Long> convs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            long conv = chat.open(buyer, shop().shopId()).view().id();
            chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("hỏi shop " + i, null));
            convs.add(conv);
            clock.advance(java.time.Duration.ofMinutes(1));
        }
        var first = query.conversations(buyer, Side.CUSTOMER, null, 2);
        assertThat(first.page().items()).extracting(r -> r.id()).containsExactly(convs.get(2), convs.get(1));
        var second = query.conversations(buyer, Side.CUSTOMER, first.page().pagination().nextCursor(), 2);
        assertThat(second.page().items()).extracting(r -> r.id()).containsExactly(convs.get(0));
        assertThat(second.page().pagination().nextCursor()).isNull();
    }

    // ===== Đã xem (M03.10) =====

    @Test
    void readResetsMyCounterMarksTheirMessagesSeenAndClearsTheChatNotification() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        long conv = chat.open(buyer, shop.shopId()).view().id();
        chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Shop ơi", null));
        long last = chat.send(buyer, Side.CUSTOMER, conv, SendCommand.text("Còn hàng không?", null)).view().id();
        chat.send(shop.userId(), Side.VENDOR, conv, SendCommand.text("Dạ đợi chút", null));
        // chủ shop không trực tuyến → đã có một thông báo CHAT_MESSAGE chưa đọc cho hội thoại (M04.03)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ? AND type = 'CHAT_MESSAGE' "
                + "AND is_read = FALSE AND reference_id = ?", Integer.class, shop.userId(), String.valueOf(conv)))
                .isEqualTo(1);

        Long upTo = chat.read(shop.userId(), Side.VENDOR, conv);
        assertThat(upTo).isEqualTo(last);
        assertThat(query.unreadTotal(shop.userId(), Side.VENDOR)).isZero();
        assertThat(query.unreadTotal(buyer, Side.CUSTOMER)).isEqualTo(1);   // phía khách chưa đọc tin của shop
        List<MessageView> buyerView = query.messages(buyer, Side.CUSTOMER, conv, null, null).items();
        assertThat(buyerView).extracting(MessageView::status).containsExactly("DELIVERED", "SEEN", "SEEN");
        assertThat(jdbc.queryForObject("SELECT is_read FROM notifications WHERE user_id = ? AND type = 'CHAT_MESSAGE'",
                Boolean.class, shop.userId())).isTrue();
        assertThat(chat.read(shop.userId(), Side.VENDOR, conv)).isNull();   // đọc lại: không có gì mới
    }
}
