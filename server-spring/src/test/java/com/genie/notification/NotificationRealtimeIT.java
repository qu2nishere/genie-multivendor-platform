// Kênh /user/queue/notifications qua WebSocket thật (API_SPEC 13; plan 2026-11-26 thông báo D5, N04.10)
package com.genie.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.notification.admin.application.AdminInboxFacade;
import com.genie.notification.admin.application.StaffAlertKind;
import com.genie.notification.inbox.application.NotificationCommand;
import com.genie.notification.inbox.application.NotificationFacade;
import com.genie.notification.inbox.application.NotificationKind;
import com.genie.notification.inbox.application.NotificationLink;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationRealtimeIT extends NotificationTestSupport {

    @LocalServerPort
    int port;

    @Autowired
    NotificationFacade facade;

    @Autowired
    AdminInboxFacade adminInbox;

    private final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    private final List<StompSession> sessions = new ArrayList<>();

    NotificationRealtimeIT() {
        client.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void disconnect() {
        sessions.forEach(s -> {
            if (s.isConnected()) s.disconnect();
        });
        sessions.clear();
    }

    /** Đăng nhập thật, mở WebSocket, đăng ký /user/queue/notifications. */
    private BlockingQueue<Map<String, Object>> listen(String email) throws Exception {
        return listen(email, "/user/queue/notifications");
    }

    private BlockingQueue<Map<String, Object>> listen(String email, String destination) throws Exception {
        String token = accessTokenOf(login(email, PASSWORD));
        StompHeaders connect = new StompHeaders();
        connect.add(HttpHeaders.AUTHORIZATION, bearer(token));
        StompSession s = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connect,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
        sessions.add(s);
        BlockingQueue<Map<String, Object>> inbox = new ArrayBlockingQueue<>(16);
        s.subscribe(destination, new StompFrameHandler() {
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
        return inbox;
    }

    private long user(String email) {
        long id = createCustomer(email, PASSWORD).getId();
        // thông báo sàn chỉ tới người có tài khoản trước lúc phát (đồng hồ test ở 2026-06-08) — D3
        jdbc.update("UPDATE users SET created_at = '2026-01-01T00:00:00Z' WHERE id = ?", id);
        return id;
    }

    private void broadcast(String json) throws Exception {
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        int status = mvc.perform(post("/api/v1/admin/platform-notifications")
                .header(HttpHeaders.AUTHORIZATION, auth(admin, com.genie.common.domain.UserRole.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(201);
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcastToAllGoesThroughThePlatformTopic() throws Exception {
        String email = uniqueEmail();
        user(email);
        BlockingQueue<Map<String, Object>> topic = listen(email, "/topic/platform");

        broadcast("{\"title\":\"Siêu sale\",\"content_html\":\"<p>50%</p>\",\"target\":\"ALL\"}");

        Map<String, Object> event = topic.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull().containsEntry("type", "PLATFORM_NOTIFICATION");
        Map<String, Object> n = (Map<String, Object>) event.get("notification");
        assertThat(n.get("id").toString()).startsWith("pn_");
        assertThat(n).containsEntry("type", "PLATFORM_ANNOUNCEMENT").containsEntry("content_html", "<p>50%</p>");
    }

    @Test
    void targetedBroadcastReachesOnlyOnlineRecipients() throws Exception {
        String picked = uniqueEmail();
        user(picked);
        String other = uniqueEmail();
        user(other);
        BlockingQueue<Map<String, Object>> mine = listen(picked);
        BlockingQueue<Map<String, Object>> theirs = listen(other);
        BlockingQueue<Map<String, Object>> theirTopic = listen(other, "/topic/platform");

        broadcast("{\"title\":\"Riêng\",\"content_html\":\"<p>Cho bạn</p>\",\"target\":\"SPECIFIC_USERS\","
                + "\"recipient_emails\":[\"" + picked + "\"]}");
        Map<String, Object> event = mine.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull().containsEntry("type", "PLATFORM_NOTIFICATION").containsEntry("unread_count", 1);
        assertThat(theirs.poll(700, TimeUnit.MILLISECONDS)).isNull();
        assertThat(theirTopic.poll(300, TimeUnit.MILLISECONDS)).isNull();

        broadcast("{\"title\":\"Chỉ shop\",\"content_html\":\"<p>Phí</p>\",\"target\":\"VENDOR\"}");
        assertThat(mine.poll(700, TimeUnit.MILLISECONDS)).isNull();   // khách thuần không nhận bản VENDOR
    }

    @Test
    @SuppressWarnings("unchecked")
    void newNotificationIsPushedWithUnreadCountOnlyToItsOwner() throws Exception {
        String email = uniqueEmail();
        long me = user(email);
        String otherEmail = uniqueEmail();
        user(otherEmail);
        BlockingQueue<Map<String, Object>> mine = listen(email);
        BlockingQueue<Map<String, Object>> theirs = listen(otherEmail);

        facade.send(new NotificationCommand(me, NotificationKind.ORDER_STATUS_CHANGED, "Đơn đang giao", "GHN đang giao",
                null, NotificationLink.of(NotificationLink.ORDER, 44L), Map.of()));

        Map<String, Object> event = mine.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull().containsEntry("type", "NEW_NOTIFICATION").containsEntry("unread_count", 1);
        Map<String, Object> n = (Map<String, Object>) event.get("notification");
        assertThat(n.get("id").toString()).startsWith("ntf_");
        assertThat(n).containsEntry("category", "ORDER").containsEntry("title", "Đơn đang giao")
                .containsEntry("is_read", false);
        assertThat((Map<String, Object>) n.get("link")).containsEntry("type", "ORDER").containsEntry("id", 44);
        assertThat(theirs.poll(700, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void markAllReadSyncsOtherTabsOfTheSameUser() throws Exception {
        String email = uniqueEmail();
        long me = user(email);
        BlockingQueue<Map<String, Object>> tab1 = listen(email);
        BlockingQueue<Map<String, Object>> tab2 = listen(email);
        facade.send(NotificationCommand.of(me, NotificationKind.SYSTEM_ALERT, "Bảo trì", "02:00", null));
        assertThat(tab1.poll(5, TimeUnit.SECONDS)).containsEntry("type", "NEW_NOTIFICATION");
        assertThat(tab2.poll(5, TimeUnit.SECONDS)).containsEntry("type", "NEW_NOTIFICATION");

        mvc.perform(post("/api/v1/me/notifications/mark-all-read")
                .header(HttpHeaders.AUTHORIZATION, auth(me, com.genie.common.domain.UserRole.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        Map<String, Object> sync = tab2.poll(5, TimeUnit.SECONDS);
        assertThat(sync).isNotNull().containsEntry("type", "NOTIFICATIONS_READ").containsEntry("unread_count", 0);
    }

    @Test
    void staffAlertReachesOnlyOnlineStaffWithThePermission() throws Exception {
        String modEmail = uniqueEmail();
        long moderator = user(modEmail);
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", moderator);
        jdbc.update("INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at) "
                + "VALUES (?, 'Kiểm duyệt', FALSE, 'MODERATOR', now(), now())", moderator);
        Long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        jdbc.update("INSERT INTO staff_permissions (staff_user_id, permission, granted_by_admin_id, granted_at) "
                + "VALUES (?, 'REVIEW_KYC', ?, now())", moderator, admin);
        String buyerEmail = uniqueEmail();
        user(buyerEmail);
        BlockingQueue<Map<String, Object>> mod = listen(modEmail);
        BlockingQueue<Map<String, Object>> buyer = listen(buyerEmail);

        adminInbox.raise(StaffAlertKind.WITHDRAW_REQUEST, "Rút tiền", "Shop A rút 1.000.000₫", "/admin/withdrawals");
        assertThat(mod.poll(700, TimeUnit.MILLISECONDS)).isNull();   // không có PROCESS_PAYOUT

        adminInbox.raise(StaffAlertKind.USER_APPROVAL, "Hồ sơ KYC mới: Shop B", "Cần thẩm định", "/admin/kyc");
        Map<String, Object> event = mod.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull().containsEntry("type", "NEW_NOTIFICATION");
        assertThat(buyer.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }
}
