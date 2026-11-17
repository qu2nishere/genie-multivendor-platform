package com.genie.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.genie.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Lớp bảo mật chung (plan AUTH A04.09): 401 / 403 đúng định dạng GNE-*, token bị thu hồi, CORS kèm cookie,
 * Argon2 đọc được hash sinh bởi scripts/hash-password.py, V13 + R__seed_demo nạp tài khoản.
 */
class SecurityIT extends IntegrationTestBase {

    @Autowired
    JwtService jwtService;

    @Autowired
    TokenRevocationStore revocations;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GNE-SEC-4010"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.meta.path").value("/api/v1/me"))
                .andExpect(jsonPath("$.meta.request_id").isNotEmpty());
    }

    @Test
    void validTokenPassesSecurity() throws Exception {
        String token = jwtService.issue(1L, UserRole.CUSTOMER, 1L).token();

        // đường dẫn cần đăng nhập nhưng không có controller → qua được lớp bảo mật thì nhận 404 chuẩn GNE-SYS-4040
        // (trước dùng /me — nay GET /me đã có, plan AUTH A11.02)
        mvc.perform(get("/api/v1/me/khong-ton-tai").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GNE-SYS-4040"));
    }

    @Test
    void tokenIssuedBeforeRevocationIsRejected() throws Exception {
        String token = jwtService.issue(7L, UserRole.CUSTOMER, 1L).token();
        clock.advance(java.time.Duration.ofSeconds(5));
        revocations.revokeAllIssuedBefore(7L, clock.instant());

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotOpenAdminApi() throws Exception {
        String token = jwtService.issue(1L, UserRole.CUSTOMER, 1L).token();

        mvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GNE-SEC-4030"));

        Integer denied = jdbc.queryForObject(
                "SELECT count(*) FROM system_logs WHERE action_type = 'ACCESS_DENIED'", Integer.class);
        assertThat(denied).isEqualTo(1);
    }

    @Test
    void corsPreflightAllowsWebOriginWithCredentials() throws Exception {
        mvc.perform(options("/api/v1/auth/sessions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4321")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4321"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void seededAccountsUseArgon2HashFromScript() {
        String hash = jdbc.queryForObject("SELECT password FROM users WHERE email = 'mai.tran@gmail.com'", String.class);
        assertThat(passwordEncoder.matches("Genie@123", hash)).isTrue();
        assertThat(passwordEncoder.matches("wrong", hash)).isFalse();

        Integer bootstrapAdmins = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE email = 'bootstrap@genie.test' AND role = 'ADMIN'", Integer.class);
        assertThat(bootstrapAdmins).isEqualTo(1);

        Integer moderatorPerms = jdbc.queryForObject("SELECT count(*) FROM staff_permissions p JOIN users u "
                + "ON u.id = p.staff_user_id WHERE u.email = 'phat.nt@genie.example'", Integer.class);
        assertThat(moderatorPerms).isEqualTo(4);
    }

    @Test
    void staffPassesAdminUrlRule() throws Exception {
        Long staffId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'linh.tm@genie.example'", Long.class);
        String token = jwtService.issue(staffId, UserRole.STAFF, 1L).token();

        // STAFF qua được luật URL /admin/** (chưa có controller → 404, không phải 403)
        mvc.perform(get("/api/v1/admin/withdrawals").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
