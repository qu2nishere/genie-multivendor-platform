package com.genie.common.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
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

/**
 * Kênh realtime {@code /user/queue/account} (API_SPEC mục 13, plan B1): bị khóa / đổi quyền → thiết bị đang mở nhận
 * {@code SESSION_REVOKED} ngay, không cần tải lại trang. Chạy server thật (cổng ngẫu nhiên) để đi qua WebSocket thật.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountChannelIT extends IntegrationTestBase {

    @LocalServerPort
    int port;

    @Autowired
    JwtService jwtService;

    private final WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
    private StompSession session;

    AccountChannelIT() {
        client.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void disconnect() {
        if (session != null && session.isConnected()) session.disconnect();
    }

    private StompSession connect(String bearer) throws Exception {
        StompHeaders connect = new StompHeaders();
        if (bearer != null) connect.add(HttpHeaders.AUTHORIZATION, bearer);
        return client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connect,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> subscribe(StompSession s) {
        BlockingQueue<Map<String, Object>> inbox = new ArrayBlockingQueue<>(4);
        s.subscribe("/user/queue/account", new StompFrameHandler() {
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
        return inbox;
    }

    private String adminToken() {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        return bearer(jwtService.issue(id, UserRole.ADMIN, 1L).token());
    }

    @Test
    void suspendedUserIsToldImmediately() throws Exception {
        String email = uniqueEmail();
        long id = createCustomer(email, PASSWORD).getId();
        session = connect(bearer(accessTokenOf(login(email, PASSWORD))));
        BlockingQueue<Map<String, Object>> inbox = subscribe(session);
        Thread.sleep(300);   // chờ khung SUBSCRIBE tới broker

        int status = mvc.perform(post("/api/v1/admin/users/" + id + "/suspend").header(HttpHeaders.AUTHORIZATION, adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason_code\":\"FRAUD\",\"duration\":\"P7D\"}")).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        Map<String, Object> event = inbox.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event).containsEntry("type", "SESSION_REVOKED").containsEntry("reason", "SUSPENDED");
    }

    @Test
    void permissionChangeKeepsTheSessionAndAsksToReloadMenu() throws Exception {
        String email = uniqueEmail();
        long staffId = createCustomer(email, PASSWORD).getId();
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", staffId);
        jdbc.update("INSERT INTO admin_profiles (user_id, full_name, is_active_moderator, staff_title, created_at, updated_at) "
                + "VALUES (?, 'Nhân viên test', FALSE, 'MODERATOR', now(), now())", staffId);
        session = connect(bearer(accessTokenOf(login(email, PASSWORD))));
        BlockingQueue<Map<String, Object>> inbox = subscribe(session);
        Thread.sleep(300);

        int status = mvc.perform(put("/api/v1/admin/staff/" + staffId + "/permissions")
                .header(HttpHeaders.AUTHORIZATION, adminToken()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"staff_title\":\"CUSTOM\",\"permissions\":[\"VIEW_DASHBOARD\"]}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        Map<String, Object> event = inbox.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull().containsEntry("type", "PERMISSIONS_CHANGED");
        assertThat(inbox.poll(500, TimeUnit.MILLISECONDS)).isNull();   // không kèm SESSION_REVOKED
        assertThat(session.isConnected()).isTrue();
    }

    @Test
    void otherUsersDoNotReceiveIt() throws Exception {
        String bystander = uniqueEmail();
        createCustomer(bystander, PASSWORD);
        session = connect(bearer(accessTokenOf(login(bystander, PASSWORD))));
        BlockingQueue<Map<String, Object>> inbox = subscribe(session);
        Thread.sleep(300);

        long target = createCustomer(uniqueEmail(), PASSWORD).getId();
        mvc.perform(post("/api/v1/admin/users/" + target + "/suspend").header(HttpHeaders.AUTHORIZATION, adminToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason_code\":\"FRAUD\",\"duration\":null}"));

        assertThat(inbox.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void connectWithoutValidTokenIsRejected() {
        assertThatThrownBy(() -> connect(null)).isInstanceOfAny(ExecutionException.class,
                java.util.concurrent.TimeoutException.class);
        assertThatThrownBy(() -> connect("Bearer khong-hop-le")).isInstanceOfAny(ExecutionException.class,
                java.util.concurrent.TimeoutException.class);
    }
}
