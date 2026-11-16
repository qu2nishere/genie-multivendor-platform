package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.profile.domain.User;
import com.genie.support.IntegrationTestBase;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-AUTH-02 — đăng nhập mật khẩu, khóa 15 phút, đăng nhập bằng OTP, mở khóa qua email (plan AUTH A07.11).
 */
class LoginFlowIT extends IntegrationTestBase {

    @Test
    void seededAccountLogsIn() throws Exception {
        MvcResult result = login("mai.tran@gmail.com", "Genie@123");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(result, "$.data.expires_in")).isEqualTo("900");
        assertThat(json(result, "$.data.user.full_name")).isEqualTo("Trần Thị Mai");
        assertThat(refreshCookieOf(result).getPath()).isEqualTo("/api/v1/auth");
        Integer logs = jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'LOGIN_SUCCESS'",
                Integer.class);
        assertThat(logs).isEqualTo(1);
    }

    @Test
    void unknownEmailAndWrongPasswordLookTheSame() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);

        MvcResult unknown = login(uniqueEmail(), PASSWORD);
        MvcResult wrong = login(email, "Wrong@123");

        assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(unknown, "$.code")).isEqualTo("GNE-AUTH-4011");
        assertThat(json(wrong, "$.code")).isEqualTo("GNE-AUTH-4011");
        assertThat(json(wrong, "$.details.remaining_attempts")).isEqualTo("4");
    }

    @Test
    void fiveWrongPasswordsLockForFifteenMinutes() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        for (int i = 0; i < 4; i++) {
            assertThat(login(email, "Wrong@123").getResponse().getStatus()).isEqualTo(401);
        }

        MvcResult locked = login(email, "Wrong@123");
        assertThat(locked.getResponse().getStatus()).isEqualTo(423);
        assertThat(json(locked, "$.code")).isEqualTo("GNE-AUTH-4230");
        assertThat(json(locked, "$.details.locked_by")).isEqualTo("PASSWORD");

        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(423);   // đúng mật khẩu vẫn bị khóa

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void googleOnlyAccountGetsClearHintAndIsNeverLocked() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, null);   // tạo bằng Google, chưa thiết lập mật khẩu
        user.linkIdentity("google", "sub-nopw-" + email);
        userRepository.saveAndFlush(user);

        for (int i = 0; i < 6; i++) {               // quá 5 lần vẫn không bị khóa
            MvcResult r = login(email, "Any@1234");
            assertThat(r.getResponse().getStatus()).isEqualTo(401);
            assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4012");
            assertThat(json(r, "$.details.reason")).isEqualTo("PASSWORD_NOT_SET");
            assertThat(json(r, "$.details.provider")).isEqualTo("google");
        }
        User after = userRepository.findByEmail(email).orElseThrow();
        assertThat(after.getFailedLoginAttempts()).isIn(null, 0);
        assertThat(after.getLockedUntil()).isNull();
    }

    @Test
    void suspendedAccountIsRejectedAfterPasswordCheck() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        user.suspend("Spam", OffsetDateTime.now(clock).plusDays(3));
        userRepository.saveAndFlush(user);

        assertThat(json(login(email, "Wrong@123"), "$.code")).isEqualTo("GNE-AUTH-4011");
        MvcResult suspended = login(email, PASSWORD);
        assertThat(suspended.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(suspended, "$.code")).isEqualTo("GNE-AUTH-4031");
        assertThat(json(suspended, "$.details.reason")).isEqualTo("Spam");
    }

    @Test
    void logsInWithEmailOtp() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);

        MvcResult sent = mvc.perform(post("/api/v1/auth/otp-challenges").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"LOGIN\",\"email\":\"" + email + "\"}")).andReturn();
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);

        MvcResult done = mvc.perform(post("/api/v1/auth/otp-challenges/" + json(sent, "$.data.challenge_id") + "/verify")
                .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + mails.lastOtp(email) + "\"}")).andReturn();
        assertThat(done.getResponse().getStatus()).isEqualTo(200);
        assertThat(refreshCookieOf(done)).isNotNull();
    }

    @Test
    void otpLoginForUnknownEmailLooksSuccessfulButSendsNothing() throws Exception {
        String email = uniqueEmail();
        MvcResult sent = mvc.perform(post("/api/v1/auth/otp-challenges").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"LOGIN\",\"email\":\"" + email + "\"}")).andReturn();

        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        assertThat(mails.sent()).isEmpty();
    }

    @Test
    void lockedAccountUnlocksThroughEmail() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        for (int i = 0; i < 5; i++) {
            login(email, "Wrong@123");
        }
        String unlockJson = "{\"purpose\":\"ACCOUNT_UNLOCK\",\"email\":\"" + email + "\"}";
        MvcResult sent = mvc.perform(post("/api/v1/auth/otp-challenges").contentType(MediaType.APPLICATION_JSON)
                .content(unlockJson)).andReturn();
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);

        MvcResult done = mvc.perform(post("/api/v1/auth/otp-challenges/" + json(sent, "$.data.challenge_id") + "/verify")
                .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + mails.lastOtp(email) + "\"}")).andReturn();
        assertThat(done.getResponse().getStatus()).isEqualTo(200);
        assertThat(userRepository.findByEmail(email).orElseThrow().getLockedUntil()).isNull();
    }

    @Test
    void unlockRequestForUnlockedAccountIsRejected() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);

        MvcResult result = mvc.perform(post("/api/v1/auth/otp-challenges").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"ACCOUNT_UNLOCK\",\"email\":\"" + email + "\"}")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(result, "$.code")).isEqualTo("GNE-AUTH-4096");
    }

    @Test
    void accessTokenWorksOnProtectedRoute() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult sessions = mvc.perform(get("/api/v1/me/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn();
        assertThat(sessions.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(sessions, "$.data[0].is_current")).isEqualTo("true");
    }
}
