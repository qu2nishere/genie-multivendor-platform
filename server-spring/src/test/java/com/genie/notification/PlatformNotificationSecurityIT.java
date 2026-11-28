// Phân quyền màn Thông báo sàn + link ảnh NOTIFICATION_IMAGE (plan 2026-11-26 thông báo N06.10)
package com.genie.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class PlatformNotificationSecurityIT extends NotificationTestSupport {

    private static final String URL = "/api/v1/admin/platform-notifications";
    private static final String UPLOAD = "{\"purpose\":\"NOTIFICATION_IMAGE\",\"content_type\":\"image/png\",\"content_length\":100}";

    private int listAs(String bearer) throws Exception {
        var req = get(URL);
        if (bearer != null) req.header(HttpHeaders.AUTHORIZATION, bearer);
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }

    private int uploadAs(String bearer) throws Exception {
        return mvc.perform(post("/api/v1/uploads/presigned-urls").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(UPLOAD)).andReturn().getResponse().getStatus();
    }

    @Test
    void onlyAdminOrStaffWithThePermission() throws Exception {
        long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        String adminAuth = auth(admin, UserRole.ADMIN);
        String granted = auth(staff("VIEW_DASHBOARD", "MANAGE_PLATFORM_NOTIFICATION"), UserRole.STAFF);
        String finance = auth(staff("VIEW_DASHBOARD", "PROCESS_PAYOUT", "UPDATE_COMMISSION", "VIEW_FINANCE",
                "MANAGE_VOUCHER"), UserRole.STAFF);
        String moderator = auth(staff("VIEW_DASHBOARD", "REVIEW_KYC"), UserRole.STAFF);
        String buyer = auth(customer(), UserRole.CUSTOMER);

        org.assertj.core.api.Assertions.assertThat(listAs(adminAuth)).isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(listAs(granted)).isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(listAs(finance)).isEqualTo(403);
        org.assertj.core.api.Assertions.assertThat(listAs(moderator)).isEqualTo(403);
        org.assertj.core.api.Assertions.assertThat(listAs(buyer)).isEqualTo(403);
        org.assertj.core.api.Assertions.assertThat(listAs(null)).isEqualTo(401);

        org.assertj.core.api.Assertions.assertThat(uploadAs(granted)).isEqualTo(200);
        org.assertj.core.api.Assertions.assertThat(uploadAs(finance)).isEqualTo(403);
        org.assertj.core.api.Assertions.assertThat(uploadAs(buyer)).isEqualTo(403);
        mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, moderator).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"content_html\":\"<p>x</p>\",\"target\":\"ALL\"}"))
                .andExpect(status().isForbidden());
    }
}
