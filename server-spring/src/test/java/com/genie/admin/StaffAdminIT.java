package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.genie.common.mail.MailTemplate;
import com.genie.support.FakeEmailSender;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ADM-01/02/03 — quản lý nhân viên, lời mời (kể cả hết hạn), phân quyền, khóa / xóa, khung Admin
 * (plan B1 R03.04, R04.06, R04.07; API 11.1, 11.18).
 */
class StaffAdminIT extends AuditLogTestSupport {

    private static String staffEmail() {
        return "nv" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + "@genie.example";
    }

    private MvcResult invite(String token, String email, String title, String permissions) throws Exception {
        return mvc.perform(post("/api/v1/admin/staff/invitations").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"full_name\":\"Hồ Thanh Trúc\",\"email\":\"" + email + "\",\"staff_title\":\"" + title
                        + "\",\"permissions\":" + permissions + "}")).andReturn();
    }

    private MvcResult list(String token, String q) throws Exception {
        return mvc.perform(get("/api/v1/admin/staff").param("q", q).param("size", "60")
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn();
    }

    /** Email mời gửi nền sau commit — chờ tối đa 5 giây. */
    private FakeEmailSender.SentMail awaitInvitation(String email, int count) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            List<FakeEmailSender.SentMail> sent = mails.sent().stream()
                    .filter(m -> m.to().equals(email) && m.template() == MailTemplate.STAFF_INVITATION).toList();
            if (sent.size() >= count) return sent.get(sent.size() - 1);
            Thread.sleep(100);
        }
        throw new AssertionError("Không thấy email mời tới " + email);
    }

    private static String tokenOf(FakeEmailSender.SentMail mail) {
        String url = mail.variables().get("activationUrl").toString();
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private long idOf(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    @Test
    void inviteThenActivateEndToEnd() throws Exception {
        String admin = adminToken();
        String email = staffEmail();
        MvcResult created = invite(admin, email, "CS_SPECIALIST", "[\"VIEW_DASHBOARD\",\"HANDLE_DISPUTE\"]");
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(created, "$.data.status")).isEqualTo("PENDING_ACTIVATION");
        assertThat(json(created, "$.data.invitation_expires_at")).isNotBlank();
        long id = idOf(email);
        assertThat(jdbc.queryForObject("SELECT staff_title FROM admin_profiles WHERE user_id = ?", String.class, id))
                .isEqualTo("CS_SPECIALIST");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM staff_permissions WHERE staff_user_id = ? "
                + "AND revoked_at IS NULL", Integer.class, id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'STAFF_INVITED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(id))).isEqualTo(1);

        FakeEmailSender.SentMail mail = awaitInvitation(email, 1);
        assertThat(mail.variables()).containsEntry("staffTitle", "Nhân viên chăm sóc khách hàng");
        String inv = tokenOf(mail);
        MvcResult otp = mvc.perform(post("/api/v1/auth/staff-activations/otp-challenges")
                .contentType(MediaType.APPLICATION_JSON).content("{\"invitation_token\":\"" + inv + "\"}")).andReturn();
        MvcResult done = mvc.perform(post("/api/v1/auth/staff-activations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"invitation_token\":\"" + inv + "\",\"challenge_id\":\"" + json(otp, "$.data.challenge_id")
                        + "\",\"otp\":\"" + mails.lastOtp(email) + "\",\"password\":\"Staff@2026\"}")).andReturn();
        assertThat(done.getResponse().getStatus()).isEqualTo(201);

        MvcResult me = mvc.perform(get("/api/v1/admin/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessTokenOf(done)))).andReturn();
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(me, "$.data.role")).isEqualTo("STAFF");
        assertThat(json(me, "$.data.staff_title")).isEqualTo("CS_SPECIALIST");
        assertThat(json(me, "$.data.permissions")).contains("HANDLE_DISPUTE").doesNotContain("MANAGE_RBAC");
        assertThat(json(list(admin, email), "$.data[0].status")).isEqualTo("ACTIVE");
    }

    @Test
    void expiredInvitationIsVisibleAndResendRenewsIt() throws Exception {
        String admin = adminToken();
        String email = staffEmail();
        invite(admin, email, "MODERATOR", "[\"VIEW_DASHBOARD\"]");
        long id = idOf(email);
        String first = tokenOf(awaitInvitation(email, 1));

        redis.delete(List.copyOf(redis.keys("auth:staff_invite:*")));          // quá 48 giờ
        MvcResult expired = list(admin, email);
        assertThat(json(expired, "$.data[0].status")).isEqualTo("INVITATION_EXPIRED");
        assertThat(json(expired, "$.data[0].invitation_expires_at")).isNotBlank();

        MvcResult resent = mvc.perform(post("/api/v1/admin/staff/" + id + "/invitations/resend")
                .header(HttpHeaders.AUTHORIZATION, admin)).andReturn();
        assertThat(resent.getResponse().getStatus()).isEqualTo(202);
        String second = tokenOf(awaitInvitation(email, 2));
        assertThat(second).isNotEqualTo(first);
        assertThat(json(list(admin, email), "$.data[0].status")).isEqualTo("PENDING_ACTIVATION");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'STAFF_INVITED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(id))).isEqualTo(2);
    }

    @Test
    void invitationValidation() throws Exception {
        String admin = adminToken();
        String email = staffEmail();
        assertThat(invite(admin, email, "MODERATOR", "[]").getResponse().getStatus()).isEqualTo(201);
        assertThat(json(invite(admin, email, "MODERATOR", "[]"), "$.code")).isEqualTo("GNE-ADM-4091");
        // D3 (2026-11-20): mặc định nhận mọi email hợp lệ — kể cả Gmail; email sai dạng → 422 GNE-VAL-4220
        String gmail = "nv" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        assertThat(invite(admin, gmail, "MODERATOR", "[]").getResponse().getStatus()).isEqualTo(201);
        assertThat(json(invite(admin, "khong-phai-email", "MODERATOR", "[]"), "$.code")).isEqualTo("GNE-VAL-4220");
        assertThat(json(invite(admin, staffEmail(), "CUSTOM", "[\"MANAGE_RBAC\"]"), "$.code"))
                .isEqualTo("GNE-SEC-4031");
        MvcResult sod = invite(admin, staffEmail(), "CUSTOM", "[\"PROCESS_PAYOUT\",\"UPDATE_COMMISSION\"]");
        assertThat(json(sod, "$.data.warnings[0]")).isEqualTo("SEGREGATION_OF_DUTIES");
        assertThat(json(sod, "$.data.staff_title")).isEqualTo("CUSTOM");
    }

    @Test
    void emailCheckWarnsWhenEmailBelongsToCustomer() throws Exception {
        String admin = adminToken();
        String customerEmail = uniqueEmail();
        createCustomer(customerEmail, PASSWORD);

        MvcResult taken = emailCheck(admin, customerEmail.toUpperCase());
        assertThat(taken.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(taken, "$.data.available")).isEqualTo("false");
        assertThat(json(taken, "$.data.reason")).isEqualTo("TAKEN");
        assertThat(json(taken, "$.data.taken_by")).isEqualTo("CUSTOMER");
        // Gửi lời mời vẫn bị chặn ở server, kèm vai trò đang giữ email
        MvcResult rejected = invite(admin, customerEmail, "MODERATOR", "[]");
        assertThat(json(rejected, "$.code")).isEqualTo("GNE-ADM-4091");
        assertThat(json(rejected, "$.details.taken_by")).isEqualTo("CUSTOMER");

        assertThat(json(emailCheck(admin, "yen.dh@genie.example"), "$.data.taken_by")).isEqualTo("ADMIN");
        MvcResult free = emailCheck(admin, staffEmail());
        assertThat(json(free, "$.data.available")).isEqualTo("true");
        assertThat(json(emailCheck(admin, "khong-phai-email"), "$.data.reason")).isEqualTo("INVALID");
    }

    private MvcResult emailCheck(String token, String email) throws Exception {
        return mvc.perform(get("/api/v1/admin/staff/email-check").param("email", email)
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn();
    }

    @Test
    void changingPermissionsTakesEffectImmediately() throws Exception {
        Long staffId = createStaff("MODERATOR", "VIEW_DASHBOARD", "REVIEW_KYC");
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, staffId);
        MvcResult session = login(email, PASSWORD);
        String staffAccess = bearer(accessTokenOf(session));
        assertThat(json(mvc.perform(get("/api/v1/admin/me").header(HttpHeaders.AUTHORIZATION, staffAccess))
                .andReturn(), "$.data.permissions")).contains("REVIEW_KYC");

        MvcResult changed = mvc.perform(put("/api/v1/admin/staff/" + staffId + "/permissions")
                .header(HttpHeaders.AUTHORIZATION, adminToken()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"staff_title\":\"FINANCE_OFFICER\",\"permissions\":[\"VIEW_DASHBOARD\",\"VIEW_FINANCE\"]}"))
                .andReturn();
        assertThat(changed.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(changed, "$.data.staff_title")).isEqualTo("FINANCE_OFFICER");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'PERMISSION_REVOKED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(staffId))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'RBAC_ROLE_CHANGED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(staffId))).isEqualTo(1);
        MvcResult refresh = mvc.perform(post("/api/v1/auth/sessions/refresh").cookie(refreshCookieOf(session)))
                .andReturn();
        assertThat(refresh.getResponse().getStatus()).isEqualTo(401);                        // phiên cũ bị thu hồi

        String fresh = bearer(accessTokenOf(login(email, PASSWORD)));
        MvcResult me = mvc.perform(get("/api/v1/admin/me").header(HttpHeaders.AUTHORIZATION, fresh)).andReturn();
        assertThat(json(me, "$.data.permissions")).contains("VIEW_FINANCE").doesNotContain("REVIEW_KYC");

        MvcResult adminTarget = mvc.perform(put("/api/v1/admin/staff/" + idOf("yen.dh@genie.example") + "/permissions")
                .header(HttpHeaders.AUTHORIZATION, adminToken()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"staff_title\":\"CUSTOM\",\"permissions\":[]}")).andReturn();
        assertThat(json(adminTarget, "$.code")).isEqualTo("GNE-SEC-4031");
    }

    @Test
    void lockUnlockAndDelete() throws Exception {
        String admin = adminToken();
        Long staffId = createStaff("CS_SPECIALIST", "VIEW_DASHBOARD");
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, staffId);

        MvcResult locked = mvc.perform(post("/api/v1/admin/staff/" + staffId + "/lock")
                .header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Nhân viên đã chấm dứt hợp đồng\"}")).andReturn();
        assertThat(json(locked, "$.data.status")).isEqualTo("LOCKED");
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(403);
        MvcResult unlocked = mvc.perform(post("/api/v1/admin/staff/" + staffId + "/unlock")
                .header(HttpHeaders.AUTHORIZATION, admin)).andReturn();
        assertThat(json(unlocked, "$.data.status")).isEqualTo("ACTIVE");
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);

        long me = idOf("yen.dh@genie.example");
        assertThat(json(mvc.perform(delete("/api/v1/admin/staff/" + me).header(HttpHeaders.AUTHORIZATION, admin))
                .andReturn(), "$.code")).isEqualTo("GNE-ADM-4092");
        assertThat(mvc.perform(delete("/api/v1/admin/staff/" + staffId).header(HttpHeaders.AUTHORIZATION, admin))
                .andReturn().getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, staffId))
                .isEqualTo("ANONYMIZED");
        assertThat(json(list(admin, email), "$.data.length()")).isEqualTo("0");
    }

    @Test
    void onlyAdminsManageStaffButEveryStaffHasAdminMe() throws Exception {
        Long userManager = createStaff("CS_SPECIALIST", "VIEW_DASHBOARD", "MANAGE_USER");
        String staff = staffToken(userManager);
        assertThat(list(staff, "").getResponse().getStatus()).isEqualTo(403);
        assertThat(mvc.perform(get("/api/v1/admin/permission-catalog").header(HttpHeaders.AUTHORIZATION, staff))
                .andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(mvc.perform(get("/api/v1/admin/me").header(HttpHeaders.AUTHORIZATION, staff))
                .andReturn().getResponse().getStatus()).isEqualTo(200);

        var customer = createCustomer(uniqueEmail(), PASSWORD);
        assertThat(mvc.perform(get("/api/v1/admin/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessTokenOf(login(customer.getEmail(), PASSWORD)))))
                .andReturn().getResponse().getStatus()).isEqualTo(403);

        MvcResult catalog = mvc.perform(get("/api/v1/admin/permission-catalog")
                .header(HttpHeaders.AUTHORIZATION, adminToken())).andReturn();
        assertThat(json(catalog, "$.data.permissions.length()")).isEqualTo("13");
        assertThat(json(catalog, "$.data.title_presets.MODERATOR")).contains("REVIEW_KYC");

        MvcResult renamed = mvc.perform(patch("/api/v1/admin/me").header(HttpHeaders.AUTHORIZATION, adminToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"full_name\":\"Đặng Hải Yến\"}")).andReturn();
        assertThat(json(renamed, "$.data.full_name")).isEqualTo("Đặng Hải Yến");
        assertThat(Optional.ofNullable(json(renamed, "$.data.permissions"))).isPresent();
    }
}
