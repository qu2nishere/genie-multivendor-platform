// Dòng an_ trong hộp thư của nhân viên theo quyền, đọc chung (API_SPEC 3.12; plan 2026-11-26 thông báo D7, N04.09)
package com.genie.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AdminInboxIT extends NotificationTestSupport {

    private List<String> ids(long user, UserRole role) throws Exception {
        String body = mvc.perform(get("/api/v1/me/notifications").header(HttpHeaders.AUTHORIZATION, auth(user, role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data[*].id");
    }

    @Test
    void staffSeeOnlyTheTypesTheirPermissionsAllow() throws Exception {
        long kyc = insertAdmin("USER_APPROVAL", "Hồ sơ KYC mới", "2026-06-01T10:00:00Z");
        long payout = insertAdmin("WITHDRAW_REQUEST", "Yêu cầu rút", "2026-06-02T10:00:00Z");
        long alert = insertAdmin("SYSTEM_ALERT", "Cảnh báo", "2026-06-03T10:00:00Z");
        long moderator = staff("VIEW_DASHBOARD", "REVIEW_KYC");
        long finance = staff("VIEW_DASHBOARD", "PROCESS_PAYOUT", "VIEW_FINANCE");
        long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        long buyer = customer();

        assertThat(ids(moderator, UserRole.STAFF)).contains("an_" + alert, "an_" + kyc).doesNotContain("an_" + payout);
        assertThat(ids(finance, UserRole.STAFF)).contains("an_" + alert, "an_" + payout).doesNotContain("an_" + kyc);
        assertThat(ids(admin, UserRole.ADMIN)).contains("an_" + alert, "an_" + payout, "an_" + kyc);
        assertThat(ids(buyer, UserRole.CUSTOMER)).noneMatch(id -> id.startsWith("an_"));

        // không đủ quyền → 404; đọc chung: một người đọc thì cả đội thấy đã đọc
        mvc.perform(patch("/api/v1/me/notifications/an_" + payout).header(HttpHeaders.AUTHORIZATION,
                auth(moderator, UserRole.STAFF)).contentType(MediaType.APPLICATION_JSON).content("{\"is_read\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/me/notifications/an_" + kyc).header(HttpHeaders.AUTHORIZATION,
                auth(moderator, UserRole.STAFF)).contentType(MediaType.APPLICATION_JSON).content("{\"is_read\":true}"))
                .andExpect(status().isNoContent());
        String adminView = mvc.perform(get("/api/v1/me/notifications").header(HttpHeaders.AUTHORIZATION,
                auth(admin, UserRole.ADMIN))).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        List<Boolean> kycRead = JsonPath.read(adminView, "$.data[?(@.id == 'an_" + kyc + "')].is_read");
        assertThat(kycRead).containsExactly(true);
        Object linkTypes = JsonPath.read(adminView, "$.data[?(@.id == 'an_" + kyc + "')].link.type");
        assertThat(linkTypes.toString()).contains("ADMIN_KYC_QUEUE");
    }
}
