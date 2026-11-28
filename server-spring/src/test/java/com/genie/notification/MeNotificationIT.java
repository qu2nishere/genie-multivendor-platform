// GET /me/notifications, PATCH /{id}, POST /mark-all-read (API_SPEC 3.12; plan 2026-11-26 thông báo N04.08)
package com.genie.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class MeNotificationIT extends NotificationTestSupport {

    private static final String URL = "/api/v1/me/notifications";

    private MvcResult page(long user, UserRole role, String query) throws Exception {
        return mvc.perform(get(URL + query).header(HttpHeaders.AUTHORIZATION, auth(user, role)))
                .andExpect(status().isOk()).andReturn();
    }

    private static List<String> ids(MvcResult r) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "$.data[*].id");
    }

    private static String cursorOf(MvcResult r) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "$.pagination.next_cursor");
    }

    private void markRead(long user, UserRole role, String id, int expected) throws Exception {
        mvc.perform(patch(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, auth(user, role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"is_read\":true}"))
                .andExpect(status().is(expected));
    }

    @Test
    void mergesPersonalAndPlatformAcrossPagesWithoutDuplicates() throws Exception {
        long u = customerCreatedAt("2026-01-01T00:00:00Z");
        long n1 = insertPersonal(u, "ORDER_STATUS_CHANGED", "Đơn 1", "2026-06-01T10:00:00Z", false);
        long p1 = insertPlatform("ALL", null, "Sàn 1", "2026-06-02T10:00:00Z");
        long n2 = insertPersonal(u, "NEW_PLATFORM_VOUCHER", "Voucher", "2026-06-03T10:00:00Z", true);
        long p2 = insertPlatform("CUSTOMER", null, "Sàn 2", "2026-06-03T10:00:00Z");   // cùng thời điểm với n2
        long n3 = insertPersonal(u, "CHAT_MESSAGE", "Chat", "2026-06-04T10:00:00Z", false);

        List<String> seen = new ArrayList<>();
        String q = "?limit=2";
        MvcResult r1 = page(u, UserRole.CUSTOMER, q);
        seen.addAll(ids(r1));
        MvcResult r2 = page(u, UserRole.CUSTOMER, q + "&cursor=" + cursorOf(r1));
        seen.addAll(ids(r2));
        MvcResult r3 = page(u, UserRole.CUSTOMER, q + "&cursor=" + cursorOf(r2));
        seen.addAll(ids(r3));
        assertThat((Object) cursorOf(r3)).isNull();
        // cùng thời điểm: nguồn hạng lớn hơn (pn_) đứng trước ntf_
        assertThat(seen).containsExactly("ntf_" + n3, "pn_" + p2, "ntf_" + n2, "pn_" + p1, "ntf_" + n1);

        String body = r1.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat((Integer) JsonPath.read(body, "$.summary.unread_count")).isEqualTo(4);
        assertThat((String) JsonPath.read(body, "$.data[0].category")).isEqualTo("CHAT");
        assertThat((String) JsonPath.read(body, "$.data[1].type")).isEqualTo("PLATFORM_ANNOUNCEMENT");
        assertThat((String) JsonPath.read(body, "$.data[1].body")).isEqualTo("Nội dung sàn");
        assertThat((String) JsonPath.read(body, "$.data[1].content_html")).contains("<b>sàn</b>");
        assertThat((String) JsonPath.read(body, "$.data[1].link.type")).isEqualTo("PLATFORM_NOTIFICATION");
        assertThat((String) JsonPath.read(body, "$.data[1].created_at")).endsWith("+07:00");
    }

    @Test
    void categoryFilterAndSystemIncludesPlatform() throws Exception {
        long u = customerCreatedAt("2026-01-01T00:00:00Z");
        insertPersonal(u, "ORDER_STATUS_CHANGED", "Đơn", "2026-06-01T10:00:00Z", false);
        long sys = insertPersonal(u, "NEW_DEVICE_LOGIN", "Thiết bị", "2026-06-02T10:00:00Z", false);
        long chat = insertPersonal(u, "CHAT_MESSAGE", "Chat", "2026-06-03T10:00:00Z", false);
        long pn = insertPlatform("ALL", null, "Sàn", "2026-06-04T10:00:00Z");

        assertThat(ids(page(u, UserRole.CUSTOMER, "?category=CHAT"))).containsExactly("ntf_" + chat);
        assertThat(ids(page(u, UserRole.CUSTOMER, "?category=SYSTEM"))).containsExactly("pn_" + pn, "ntf_" + sys);
        mvc.perform(get(URL + "?category=SPAM").header(HttpHeaders.AUTHORIZATION, auth(u, UserRole.CUSTOMER)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get(URL + "?limit=51").header(HttpHeaders.AUTHORIZATION, auth(u, UserRole.CUSTOMER)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get(URL + "?cursor=hong").header(HttpHeaders.AUTHORIZATION, auth(u, UserRole.CUSTOMER)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void platformAudienceRules() throws Exception {
        long buyer = customerCreatedAt("2026-01-01T00:00:00Z");
        long seller = vendorCreatedAt("2026-01-01T00:00:00Z");
        long late = customerCreatedAt("2026-06-10T00:00:00Z");
        long all = insertPlatform("ALL", null, "Tất cả", "2026-06-05T10:00:00Z");
        long onlyVendors = insertPlatform("VENDOR", null, "Shop", "2026-06-06T10:00:00Z");
        long onlyBuyers = insertPlatform("CUSTOMER", null, "Khách", "2026-06-07T10:00:00Z");
        long specific = insertPlatform("SPECIFIC_USERS", "[" + seller + "]", "Riêng", "2026-06-08T10:00:00Z");
        insertPlatform("ALL", null, "Hẹn giờ", null);   // chưa phát

        assertThat(ids(page(buyer, UserRole.CUSTOMER, ""))).containsExactly("pn_" + onlyBuyers, "pn_" + all);
        assertThat(ids(page(seller, UserRole.VENDOR, ""))).containsExactly("pn_" + specific, "pn_" + onlyVendors,
                "pn_" + all);
        assertThat(ids(page(late, UserRole.CUSTOMER, ""))).isEmpty();   // đăng ký sau lúc phát
        long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        assertThat(ids(page(admin, UserRole.ADMIN, ""))).noneMatch(id -> id.startsWith("pn_"));

        markRead(buyer, UserRole.CUSTOMER, "pn_" + onlyVendors, 404);   // ngoài đối tượng
        markRead(buyer, UserRole.CUSTOMER, "pn_" + all, 204);
        markRead(buyer, UserRole.CUSTOMER, "pn_" + all, 204);           // gọi lại vẫn 204
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_platform_notification_reads WHERE "
                + "platform_notification_id = ?", Integer.class, all)).isEqualTo(1);
    }

    @Test
    void readMarksAreScopedToTheOwner() throws Exception {
        long a = customerCreatedAt("2026-01-01T00:00:00Z");
        long b = customerCreatedAt("2026-01-01T00:00:00Z");
        long mine = insertPersonal(a, "ORDER_STATUS_CHANGED", "Của A", "2026-06-01T10:00:00Z", false);

        markRead(b, UserRole.CUSTOMER, "ntf_" + mine, 404);
        markRead(a, UserRole.CUSTOMER, "ntf_abc", 404);
        markRead(a, UserRole.CUSTOMER, "xyz_1", 404);
        mvc.perform(patch(URL + "/ntf_" + mine).header(HttpHeaders.AUTHORIZATION, auth(a, UserRole.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"is_read\":false}"))
                .andExpect(status().isUnprocessableEntity());
        markRead(a, UserRole.CUSTOMER, "ntf_" + mine, 204);
        assertThat(jdbc.queryForObject("SELECT is_read FROM notifications WHERE id = ?", Boolean.class, mine)).isTrue();
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, auth(b, UserRole.CUSTOMER)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void markAllReadByCategory() throws Exception {
        long u = customerCreatedAt("2026-01-01T00:00:00Z");
        insertPersonal(u, "ORDER_STATUS_CHANGED", "Đơn", "2026-06-01T10:00:00Z", false);
        insertPersonal(u, "CHAT_MESSAGE", "Chat", "2026-06-02T10:00:00Z", false);
        insertPlatform("ALL", null, "Sàn", "2026-06-03T10:00:00Z");

        mvc.perform(post(URL + "/mark-all-read").header(HttpHeaders.AUTHORIZATION, auth(u, UserRole.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"ORDER\"}"))
                .andExpect(status().isNoContent());
        assertThat((Integer) JsonPath.read(page(u, UserRole.CUSTOMER, "").getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                "$.summary.unread_count")).isEqualTo(2);

        mvc.perform(post(URL + "/mark-all-read").header(HttpHeaders.AUTHORIZATION, auth(u, UserRole.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"category\":null}"))
                .andExpect(status().isNoContent());
        assertThat((Integer) JsonPath.read(page(u, UserRole.CUSTOMER, "").getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                "$.summary.unread_count")).isZero();
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
