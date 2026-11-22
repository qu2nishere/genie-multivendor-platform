package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.support.TestDataBuilder;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** API 11.13 — người dùng vi phạm (UC-ADM-03, plan B1 R05). Dữ liệu CSDL dùng chung giữa các test → lọc theo email riêng. */
class AdminUserIT extends AuditLogTestSupport {

    private MvcResult list(String token, String tab, String q) throws Exception {
        var req = get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, token).param("size", "60");
        if (tab != null) req.param("tab", tab);
        if (q != null) req.param("q", q);
        return mvc.perform(req).andReturn();
    }

    private MvcResult suspend(String token, long id, String body) throws Exception {
        return mvc.perform(post("/api/v1/admin/users/" + id + "/suspend").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private MvcResult reactivate(String token, long id, String reason) throws Exception {
        return mvc.perform(post("/api/v1/admin/users/" + id + "/reactivate").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}")).andReturn();
    }

    private long countOf(MvcResult r, String tab) throws Exception {
        return Long.parseLong(json(r, "$.summary.counts." + tab));
    }

    @Test
    void listsCustomersAndShopsButNeverStaff() throws Exception {
        String admin = adminToken();
        String email = uniqueEmail();
        long customerId = createCustomer(email, PASSWORD).getId();
        var shop = new TestDataBuilder(jdbc).shop().name("Tiệm Gốm " + TestDataBuilder.uniqueWord()).create();

        MvcResult byEmail = list(admin, "ALL", email.toUpperCase());
        assertThat(byEmail.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(byEmail, "$.data.length()")).isEqualTo("1");
        assertThat(json(byEmail, "$.data[0].id")).isEqualTo(String.valueOf(customerId));
        assertThat(json(byEmail, "$.data[0].code")).isEqualTo("U-" + customerId);
        assertThat(json(byEmail, "$.data[0].full_name")).isEqualTo("Người dùng test");
        assertThat(json(byEmail, "$.data[0].phone_number")).isEqualTo("0901234567");
        assertThat(json(byEmail, "$.data[0].role")).isEqualTo("CUSTOMER");
        assertThat(json(byEmail, "$.data[0].status")).isEqualTo("ACTIVE");
        assertThat(json(byEmail, "$.data[0].order_count")).isEqualTo("0");
        assertThat(json(byEmail, "$.data[0].suspension")).isNull();
        // counts theo 4 tab: ALL = CUSTOMER + VENDOR
        assertThat(countOf(byEmail, "ALL")).isEqualTo(countOf(byEmail, "CUSTOMER") + countOf(byEmail, "VENDOR"));

        // Tìm theo tên shop (không dấu) → chủ shop, kèm shop_name; tab CUSTOMER không chứa chủ shop
        String plain = shop.name().replace("Tiệm Gốm", "tiem gom");
        MvcResult byShop = list(admin, "VENDOR", plain);
        assertThat(json(byShop, "$.data.length()")).isEqualTo("1");
        assertThat(json(byShop, "$.data[0].role")).isEqualTo("VENDOR");
        assertThat(json(byShop, "$.data[0].shop_name")).isEqualTo(shop.name());
        assertThat(json(list(admin, "CUSTOMER", plain), "$.data.length()")).isEqualTo("0");

        // Nhân viên / Admin không xuất hiện ở đây (quản lý ở 11.18)
        assertThat(json(list(admin, "ALL", "yen.dh@genie.example"), "$.data.length()")).isEqualTo("0");
        assertThat(list(admin, "NOPE", null).getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void suspendForSevenDaysThenReactivate() throws Exception {
        String admin = adminToken();
        String email = uniqueEmail();
        long id = createCustomer(email, PASSWORD).getId();
        MvcResult session = login(email, PASSWORD);
        long lockedBefore = countOf(list(admin, "LOCKED", null), "LOCKED");

        MvcResult r = suspend(admin, id, "{\"reason_code\":\"ABNORMAL_RETURNS\",\"note\":null,\"duration\":\"P7D\"}");
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(r, "$.data.status")).isEqualTo("SUSPENDED");
        assertThat(json(r, "$.data.suspension.reason_code")).isEqualTo("ABNORMAL_RETURNS");
        assertThat(json(r, "$.data.suspension.note")).isNull();
        assertThat(OffsetDateTime.parse(json(r, "$.data.suspension.until")).toInstant())
                .isEqualTo(Instant.parse(START).plusSeconds(7 * 24 * 3600));
        assertThat(jdbc.queryForObject("SELECT suspension_reason FROM users WHERE id = ?", String.class, id))
                .isEqualTo("ABNORMAL_RETURNS");
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(403);
        // phiên đang mở bị thu hồi ngay
        assertThat(mvc.perform(post("/api/v1/auth/sessions/refresh").cookie(refreshCookieOf(session))).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'ACCOUNT_SUSPENDED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(id))).isEqualTo(1);
        MvcResult locked = list(admin, "LOCKED", email);
        assertThat(json(locked, "$.data.length()")).isEqualTo("1");
        assertThat(countOf(locked, "LOCKED")).isEqualTo(lockedBefore + 1);

        // Mở khóa: lý do bắt buộc
        assertThat(reactivate(admin, id, " ").getResponse().getStatus()).isEqualTo(422);
        MvcResult open = reactivate(admin, id, "Đã xác minh khiếu nại");
        assertThat(json(open, "$.data.status")).isEqualTo("ACTIVE");
        assertThat(json(open, "$.data.suspension")).isNull();
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT details::text FROM system_logs WHERE action_type = 'ACCOUNT_REACTIVATED' "
                + "AND reference_id = ? ORDER BY created_at DESC LIMIT 1", String.class, String.valueOf(id)))
                .contains("Đã xác minh khiếu nại");
    }

    @Test
    void permanentLockNeedsNoteForOtherAndRejectsBadInput() throws Exception {
        String admin = adminToken();
        long id = createCustomer(uniqueEmail(), PASSWORD).getId();

        assertThat(json(suspend(admin, id, "{\"reason_code\":\"OTHER\",\"duration\":null}"), "$.code"))
                .isEqualTo("GNE-VAL-4220");
        assertThat(json(suspend(admin, id, "{\"reason_code\":\"SPAM\",\"duration\":\"P7D\"}"), "$.code"))
                .isEqualTo("GNE-VAL-4220");
        assertThat(json(suspend(admin, id, "{\"reason_code\":\"FRAUD\",\"duration\":\"P1D\"}"), "$.code"))
                .isEqualTo("GNE-VAL-4220");

        MvcResult r = suspend(admin, id, "{\"reason_code\":\"OTHER\",\"note\":\"Lừa đảo qua chat\",\"duration\":null}");
        assertThat(json(r, "$.data.status")).isEqualTo("LOCKED");
        assertThat(json(r, "$.data.suspension.reason_code")).isEqualTo("OTHER");
        assertThat(json(r, "$.data.suspension.note")).isEqualTo("Lừa đảo qua chat");
        assertThat(json(r, "$.data.suspension.until")).isNull();
        assertThat(jdbc.queryForObject("SELECT suspension_reason FROM users WHERE id = ?", String.class, id))
                .isEqualTo("OTHER: Lừa đảo qua chat");
    }

    @Test
    void internalAccountsAreNotTargets() throws Exception {
        String admin = adminToken();
        Long staffId = createStaff("MODERATOR", "VIEW_DASHBOARD");
        assertThat(json(suspend(admin, staffId, "{\"reason_code\":\"FRAUD\",\"duration\":\"P7D\"}"), "$.code"))
                .isEqualTo("GNE-SYS-4040");
        assertThat(reactivate(admin, idOf("yen.dh@genie.example"), "Thử").getResponse().getStatus()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, staffId))
                .isEqualTo("ACTIVE");
    }

    @Test
    void onlyAdminOrStaffWithManageUser() throws Exception {
        Long userManager = createStaff("CS_SPECIALIST", "VIEW_DASHBOARD", "MANAGE_USER");
        Long moderator = createStaff("MODERATOR", "VIEW_DASHBOARD", "REVIEW_KYC");
        assertThat(list(staffToken(userManager), null, null).getResponse().getStatus()).isEqualTo(200);
        assertThat(list(staffToken(moderator), null, null).getResponse().getStatus()).isEqualTo(403);
        String customer = bearer(accessTokenOf(login(createCustomerEmail(), PASSWORD)));
        assertThat(list(customer, null, null).getResponse().getStatus()).isEqualTo(403);
    }

    private String createCustomerEmail() {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        return email;
    }

    private long idOf(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }
}
