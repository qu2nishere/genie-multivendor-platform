package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** UC-ADM-07 — quyền VIEW_AUDIT_LOG (plan nhật ký kiểm toán L03.08). */
class AuditLogPermissionIT extends AuditLogTestSupport {

    private static final String URL = "/api/v1/admin/audit-logs";

    @Test
    void adminAndStaffWithPermissionCanRead() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, adminToken())).andExpect(status().isOk());
        Long staff = createStaff("CS_SPECIALIST", "VIEW_DASHBOARD", "VIEW_AUDIT_LOG");
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, staffToken(staff))).andExpect(status().isOk());
    }

    @Test
    void staffWithoutPermissionIsDeniedAndLogged() throws Exception {
        Long staff = createStaff("CS_SPECIALIST", "VIEW_DASHBOARD", "HANDLE_DISPUTE");
        clearLogs();
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, staffToken(staff)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GNE-SEC-4030"));
        Integer denied = jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'ACCESS_DENIED' "
                + "AND user_id = ?", Integer.class, staff);
        assertThat(denied).isEqualTo(1);
    }

    @Test
    void customerIsForbiddenAndAnonymousUnauthorized() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token("mai.tran@gmail.com", UserRole.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GNE-SEC-4030"));
        mvc.perform(get(URL)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("GNE-SEC-4010"));
    }
}
