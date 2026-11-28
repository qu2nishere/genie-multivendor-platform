// Admin › Thông báo sàn — soạn / phát / hẹn giờ / sửa / xóa (API_SPEC 11.17; plan 2026-11-26 thông báo N06.09)
package com.genie.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.jayway.jsonpath.JsonPath;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class AdminPlatformNotificationIT extends NotificationTestSupport {

    private static final String URL = "/api/v1/admin/platform-notifications";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    private final HttpClient http = HttpClient.newHttpClient();
    private long admin;
    private String adminAuth;

    @BeforeEach
    void admin() {
        admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        adminAuth = auth(admin, UserRole.ADMIN);
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                               String json) throws Exception {
        return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, adminAuth).contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private static String body(String title, String html, String image, String target, String emails, String sendAt) {
        return "{\"title\":" + q(title) + ",\"content_html\":" + q(html) + ",\"image_object_key\":" + q(image)
                + ",\"target\":" + q(target) + ",\"recipient_emails\":" + (emails == null ? "[]" : emails)
                + ",\"send_at\":" + q(sendAt) + "}";
    }

    private static String q(String v) {
        return v == null ? "null" : "\"" + v.replace("\"", "\\\"") + "\"";
    }

    private static String read(MvcResult r, String path) throws Exception {
        Object v = JsonPath.read(r.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return v == null ? null : v.toString();
    }

    /** Số người nhận theo đúng luật D1 / D3 tại giờ của đồng hồ test. */
    private long audience(String roleCondition) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE status = 'ACTIVE' AND " + roleCondition
                + " AND created_at <= ?", Long.class, Timestamp.from(clock.instant()));
    }

    private String uploadImage() throws Exception {
        var r = mvc.perform(post("/api/v1/uploads/presigned-urls").header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"NOTIFICATION_IMAGE\",\"content_type\":\"image/png\",\"content_length\":"
                        + PNG.length + "}")).andExpect(status().isOk()).andReturn();
        String json = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String url = JsonPath.read(json, "$.data.upload_url");
        http.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "image/png")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(PNG)).build(), HttpResponse.BodyHandlers.discarding());
        return JsonPath.read(json, "$.data.object_key");
    }

    private static boolean inSilo(String key) throws Exception {
        try {
            siloClient().statObject(StatObjectArgs.builder().bucket("content-assets").object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        }
    }

    @Test
    void publishNowFreezesReachSanitizesAndShowsInInbox() throws Exception {
        long buyer = customerCreatedAt("2026-01-01T00:00:00Z");
        long seller = vendorCreatedAt("2026-01-01T00:00:00Z");
        long expectedAll = audience("role IN ('CUSTOMER', 'VENDOR')");
        long expectedVendor = audience("role = 'VENDOR'");

        MvcResult all = send(post(URL), body("Siêu sale 12.12",
                "<p onclick=\"x()\">Giảm tới <b>50%</b></p><script>alert(1)</script>", null, "ALL", null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.is_scheduled").value(false))
                .andExpect(jsonPath("$.data.reach_count").value(expectedAll))
                .andReturn();
        assertThat(read(all, "$.data.content_html")).isEqualTo("<p>Giảm tới <b>50%</b></p>");
        assertThat(read(all, "$.data.content_preview")).isEqualTo("Giảm tới 50%");
        assertThat(read(all, "$.data.code")).startsWith("PN-");
        long id = Long.parseLong(read(all, "$.data.id"));

        send(post(URL), body("Chỉ shop", "<p>Phí sàn</p>", null, "VENDOR", null, null))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.reach_count").value(expectedVendor));

        // người mua thấy thông báo ALL trong hộp thư, không thấy bản VENDOR
        String inbox = mvc.perform(get("/api/v1/me/notifications").header(HttpHeaders.AUTHORIZATION,
                auth(buyer, UserRole.CUSTOMER))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> ids = JsonPath.read(inbox, "$.data[*].id");
        assertThat(ids).containsExactly("pn_" + id);
        String sellerInbox = mvc.perform(get("/api/v1/me/notifications").header(HttpHeaders.AUTHORIZATION,
                auth(seller, UserRole.VENDOR))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat((List<String>) JsonPath.read(sellerInbox, "$.data[*].id")).hasSize(2);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = "
                + "'PLATFORM_NOTIFICATION_PUBLISHED' AND reference_id = ?", Integer.class, String.valueOf(id))).isEqualTo(1);
    }

    @Test
    void specificUsersNeedKnownEmails() throws Exception {
        String email = uniqueEmail();
        long target = createCustomer(email, PASSWORD).getId();
        jdbc.update("UPDATE users SET created_at = '2026-01-01T00:00:00Z' WHERE id = ?", target);

        send(post(URL), body("Riêng", "<p>Xin chào</p>", null, "SPECIFIC_USERS", "[]", null))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GNE-NOT-4221"));
        send(post(URL), body("Riêng", "<p>Xin chào</p>", null, "SPECIFIC_USERS",
                "[\"" + email + "\",\"khong-co@genie.example\"]", null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-NOT-4221"))
                .andExpect(jsonPath("$.details.unknown_emails[0]").value("khong-co@genie.example"));
        MvcResult ok = send(post(URL), body("Riêng", "<p>Xin chào</p>", null, "SPECIFIC_USERS",
                "[\"" + email.toUpperCase() + "\"]", null))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.reach_count").value(1)).andReturn();
        long id = Long.parseLong(read(ok, "$.data.id"));
        mvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(jsonPath("$.data.recipient_emails[0]").value(email.toLowerCase()));
    }

    @Test
    void validationErrorsAreCollectedPerField() throws Exception {
        send(post(URL), body("", "<script>x</script>", null, "NOBODY", null, "2026-06-08T07:00:30Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'content_html')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'target')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'send_at')]").exists());
        send(post(URL), body("x".repeat(101), "<p>a</p>", null, "ALL", null, "2026-12-31T00:00:00Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'send_at')]").exists());
    }

    @Test
    void scheduledCanBeEditedThenDispatchedCannot() throws Exception {
        MvcResult r = send(post(URL), body("Bảo trì", "<p>02:00</p>", null, "ALL", null, "2026-06-09T02:00:00+07:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.is_scheduled").value(true))
                .andExpect(jsonPath("$.data.reach_count").value(0))
                .andReturn();
        long id = Long.parseLong(read(r, "$.data.id"));

        send(put(URL + "/" + id), body("Bảo trì (đổi giờ)", "<p>03:00</p>", null, "CUSTOMER", null,
                "2026-06-09T03:00:00+07:00")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Bảo trì (đổi giờ)"))
                .andExpect(jsonPath("$.data.is_scheduled").value(true));
        // send_at = null khi sửa → phát ngay
        send(put(URL + "/" + id), body("Bảo trì ngay", "<p>Bây giờ</p>", null, "CUSTOMER", null, null))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.is_scheduled").value(false));
        send(put(URL + "/" + id), body("Sửa lại", "<p>x</p>", null, "CUSTOMER", null, null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-NOT-4091"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = "
                + "'PLATFORM_NOTIFICATION_UPDATED' AND reference_id = ?", Integer.class, String.valueOf(id))).isEqualTo(2);
    }

    @Test
    void imageIsPromotedAndRemovedWithTheNotification() throws Exception {
        long buyer = customerCreatedAt("2026-01-01T00:00:00Z");
        String tmp = uploadImage();
        MvcResult r = send(post(URL), body("Có ảnh", "<p>Banner</p>", tmp, "ALL", null, null))
                .andExpect(status().isCreated()).andReturn();
        long id = Long.parseLong(read(r, "$.data.id"));
        String key = jdbc.queryForObject("SELECT image_url FROM platform_notifications WHERE id = ?", String.class, id);
        assertThat(key).startsWith("notice/" + id + "/");
        assertThat(read(r, "$.data.image_url")).contains("content-assets").contains(key);
        assertThat(inSilo(key)).isTrue();

        // khách đọc rồi Admin xóa (thông báo đã phát) → biến khỏi hộp thư, ảnh bị xóa
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/me/notifications/pn_" + id).header(HttpHeaders.AUTHORIZATION, auth(buyer, UserRole.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"is_read\":true}")).andExpect(status().isNoContent());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(jsonPath("$.data[0].read_count").value(1))
                .andExpect(jsonPath("$.summary.total_reads").value(1));
        mvc.perform(delete(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, adminAuth)).andExpect(status().isNoContent());
        assertThat(inSilo(key)).isFalse();
        mvc.perform(get("/api/v1/me/notifications").header(HttpHeaders.AUTHORIZATION, auth(buyer, UserRole.CUSTOMER)))
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, adminAuth)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = "
                + "'PLATFORM_NOTIFICATION_DELETED' AND reference_id = ?", Integer.class, String.valueOf(id))).isEqualTo(1);
    }

    @Test
    void listFiltersAndPagesByFive() throws Exception {
        for (int i = 0; i < 6; i++) {
            send(post(URL), body("Khuyến mãi " + i, "<p>Nội dung số " + i + "</p>", null, i % 2 == 0 ? "ALL" : "VENDOR",
                    null, null)).andExpect(status().isCreated());
        }
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.pagination.total_items").value(6))
                .andExpect(jsonPath("$.summary.sent_this_month").value(6));
        mvc.perform(get(URL + "?target=VENDOR").header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(jsonPath("$.pagination.total_items").value(3));
        mvc.perform(get(URL + "?q=so 4").header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(jsonPath("$.pagination.total_items").value(1));
        mvc.perform(get(URL + "?target=BAD").header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isUnprocessableEntity());
    }
}
