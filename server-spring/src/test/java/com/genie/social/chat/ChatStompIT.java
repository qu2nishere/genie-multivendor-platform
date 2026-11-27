// Chat qua WebSocket STOMP thật (API_SPEC 13; plan 2026-11-26 chat M04.04)
package com.genie.social.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.domain.UserRole;
import com.genie.social.chat.application.ChatService;
import com.genie.social.chat.application.SendCommand;
import com.genie.social.chat.domain.Conversation.Side;
import com.genie.support.TestDataBuilder;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatStompIT extends ChatTestSupport {

    @LocalServerPort
    int port;

    @Autowired
    ChatService chat;

    private final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    private final List<StompSession> sessions = new ArrayList<>();

    ChatStompIT() {
        client.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void disconnect() {
        sessions.forEach(s -> {
            if (s.isConnected()) s.disconnect();
        });
        sessions.clear();
    }

    private record Client(StompSession session, BlockingQueue<Map<String, Object>> inbox) {
    }

    private Client connect(long userId, UserRole role) throws Exception {
        StompHeaders connect = new StompHeaders();
        connect.add(HttpHeaders.AUTHORIZATION, auth(userId, role));
        StompSession s = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connect,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
        sessions.add(s);
        BlockingQueue<Map<String, Object>> inbox = new ArrayBlockingQueue<>(64);
        s.subscribe("/user/queue/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.offer((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);   // chờ SUBSCRIBE tới broker
        return new Client(s, inbox);
    }

    private record Pair(TestDataBuilder.Shop shop, long buyer, long conversationId) {
    }

    private Pair pair() {
        TestDataBuilder.Shop shop = shop();
        long buyer = customer();
        return new Pair(shop, buyer, chat.open(buyer, shop.shopId()).view().id());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendOverStompAcksTheSenderAndDeliversToTheRecipient() throws Exception {
        Pair p = pair();
        Client buyer = connect(p.buyer(), UserRole.CUSTOMER);
        Client owner = connect(p.shop().userId(), UserRole.VENDOR);
        String cmid = UUID.randomUUID().toString();

        long started = System.nanoTime();
        buyer.session().send("/app/chat.send", Map.of("conversation_id", p.conversationId(), "client_message_id", cmid,
                "type", "TEXT", "text", "Còn hàng không shop?"));

        Map<String, Object> incoming = owner.inbox().poll(3, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        assertThat(incoming).isNotNull().containsEntry("type", "MESSAGE_CREATED")
                .containsEntry("unread_total", 1);
        assertThat(elapsedMs).isLessThan(1000);
        Map<String, Object> theirs = (Map<String, Object>) incoming.get("message");
        assertThat(theirs).containsEntry("sender", "THEM").containsEntry("text", "Còn hàng không shop?")
                .containsEntry("client_message_id", null);

        Map<String, Object> ack = buyer.inbox().poll(3, TimeUnit.SECONDS);
        assertThat(ack).isNotNull().containsEntry("type", "MESSAGE_CREATED").containsEntry("client_message_id", cmid)
                .containsEntry("unread_total", 0);
        assertThat((Map<String, Object>) ack.get("message")).containsEntry("sender", "ME")
                .containsEntry("status", "DELIVERED").containsEntry("client_message_id", cmid);
    }

    @Test
    void readOverStompTellsTheSenderMessagesWereSeen() throws Exception {
        Pair p = pair();
        long last = chat.send(p.buyer(), Side.CUSTOMER, p.conversationId(), SendCommand.text("Shop ơi", null))
                .view().id();
        Client buyer = connect(p.buyer(), UserRole.CUSTOMER);
        Client owner = connect(p.shop().userId(), UserRole.VENDOR);

        owner.session().send("/app/chat.read", Map.of("conversation_id", p.conversationId()));

        Map<String, Object> seen = buyer.inbox().poll(3, TimeUnit.SECONDS);
        assertThat(seen).isNotNull().containsEntry("type", "MESSAGE_SEEN")
                .containsEntry("conversation_id", (int) p.conversationId())
                .containsEntry("up_to_message_id", (int) last);
        assertThat(owner.inbox().poll(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void typingIsRelayedAndThrottled() throws Exception {
        Pair p = pair();
        Client buyer = connect(p.buyer(), UserRole.CUSTOMER);
        Client owner = connect(p.shop().userId(), UserRole.VENDOR);
        for (int i = 0; i < 5; i++) {
            buyer.session().send("/app/chat.typing", Map.of("conversation_id", p.conversationId()));
            Thread.sleep(150);
        }
        Map<String, Object> typing = owner.inbox().poll(3, TimeUnit.SECONDS);
        assertThat(typing).isNotNull().containsEntry("type", "TYPING");
        assertThat(owner.inbox().poll(600, TimeUnit.MILLISECONDS)).isNull();
        assertThat(buyer.inbox().poll(100, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void sendingIntoSomeoneElsesConversationFailsWithoutDroppingTheConnection() throws Exception {
        Pair p = pair();
        long stranger = customer();
        Client intruder = connect(stranger, UserRole.CUSTOMER);
        String cmid = UUID.randomUUID().toString();
        intruder.session().send("/app/chat.send", Map.of("conversation_id", p.conversationId(),
                "client_message_id", cmid, "type", "TEXT", "text", "chen ngang"));

        Map<String, Object> failed = intruder.inbox().poll(3, TimeUnit.SECONDS);
        assertThat(failed).isNotNull().containsEntry("type", "MESSAGE_FAILED").containsEntry("code", "GNE-SYS-4040")
                .containsEntry("client_message_id", cmid);
        intruder.session().send("/app/chat.send", Map.of("conversation_id", p.conversationId(), "type", "TEXT",
                "text", " "));
        // tin rỗng bị chặn ngay ở bước kiểm nội dung (trước khi tra hội thoại)
        assertThat(intruder.inbox().poll(3, TimeUnit.SECONDS)).isNotNull().containsEntry("code", "GNE-VAL-4220");
        assertThat(intruder.session().isConnected()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM messages WHERE conversation_id = ?", Integer.class,
                p.conversationId())).isZero();
    }

    @Test
    void invalidTextAndStaffAccountsGetMessageFailed() throws Exception {
        Pair p = pair();
        Client buyer = connect(p.buyer(), UserRole.CUSTOMER);
        buyer.session().send("/app/chat.send", Map.of("conversation_id", p.conversationId(), "type", "TEXT", "text",
                "   "));
        assertThat(buyer.inbox().poll(3, TimeUnit.SECONDS)).isNotNull().containsEntry("type", "MESSAGE_FAILED")
                .containsEntry("code", "GNE-VAL-4220");

        long staff = customer();
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", staff);
        Client staffClient = connect(staff, UserRole.STAFF);
        staffClient.session().send("/app/chat.send", Map.of("conversation_id", p.conversationId(), "type", "TEXT",
                "text", "xin chào"));
        assertThat(staffClient.inbox().poll(3, TimeUnit.SECONDS)).isNotNull().containsEntry("code", "GNE-SEC-4030");
    }

    @Test
    void offlineRecipientGetsOneChatNotificationPerConversation() throws Exception {
        Pair p = pair();
        Client owner = connect(p.shop().userId(), UserRole.VENDOR);
        chat.send(p.buyer(), Side.CUSTOMER, p.conversationId(), SendCommand.text("Lúc shop online", null));
        assertThat(owner.inbox().poll(3, TimeUnit.SECONDS)).isNotNull();
        assertThat(chatNotifications(p.shop().userId())).isZero();

        owner.session().disconnect();
        Thread.sleep(500);
        // ngắt kết nối → ghi "hoạt động lần cuối" (D13)
        assertThat(redis.hasKey("presence:last:" + p.shop().userId())).isTrue();

        for (int i = 0; i < 3; i++) {
            chat.send(p.buyer(), Side.CUSTOMER, p.conversationId(), SendCommand.text("Tin offline " + i, null));
        }
        assertThat(chatNotifications(p.shop().userId())).isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap("SELECT title, body, reference_id, reference_type FROM notifications "
                + "WHERE user_id = ? AND type = 'CHAT_MESSAGE'", p.shop().userId());
        assertThat(row.get("body")).isEqualTo("Tin offline 2");
        assertThat(row.get("reference_id")).isEqualTo(String.valueOf(p.conversationId()));
        assertThat(row.get("reference_type")).isEqualTo("CONVERSATION");
        assertThat((String) row.get("title")).startsWith("Tin nhắn mới từ ");

        // mở hội thoại → thông báo chat cũng thành đã đọc (8.5)
        chat.read(p.shop().userId(), Side.VENDOR, p.conversationId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ? AND type = 'CHAT_MESSAGE' "
                + "AND is_read = FALSE", Integer.class, p.shop().userId())).isZero();
    }

    @Test
    void ownerOnlineDuringBusinessHoursGetsNoAwayReplyButAtNightDoes() throws Exception {
        Pair p = pair();
        jdbc.update("UPDATE vendor_profiles SET auto_reply_enabled = TRUE, auto_reply_offline = 'Shop vắng' "
                + "WHERE id = ?", p.shop().shopId());
        Client owner = connect(p.shop().userId(), UserRole.VENDOR);
        chat.send(p.buyer(), Side.CUSTOMER, p.conversationId(), SendCommand.text("14:00 giờ VN", null));
        assertThat(owner.inbox().poll(3, TimeUnit.SECONDS)).isNotNull();
        assertThat(autoMessages(p.conversationId())).isZero();

        clock.set(java.time.Instant.parse("2026-06-08T16:00:00Z"));   // 23:00 giờ VN — vẫn online nhưng ngoài giờ
        chat.send(p.buyer(), Side.CUSTOMER, p.conversationId(), SendCommand.text("23:00 giờ VN", null));
        assertThat(autoMessages(p.conversationId())).isEqualTo(1);
    }

    private int autoMessages(long conv) {
        return jdbc.queryForObject("SELECT count(*) FROM messages WHERE conversation_id = ? AND is_auto", Integer.class,
                conv);
    }

    private int chatNotifications(long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ? AND type = 'CHAT_MESSAGE'",
                Integer.class, userId);
    }
}
