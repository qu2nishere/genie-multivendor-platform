package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.genie.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-AUTH-04 quên mật khẩu, UC-AUTH-06 đổi mật khẩu với OTP bước 2 (plan AUTH A08.04, A09.03).
 */
class PasswordFlowIT extends IntegrationTestBase {

    private MvcResult sendOtp(String purpose, String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/otp-challenges").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"" + purpose + "\",\"email\":\"" + email + "\"}")).andReturn();
    }

    private MvcResult verify(String challengeId, String otp) throws Exception {
        return mvc.perform(post("/api/v1/auth/otp-challenges/" + challengeId + "/verify")
                .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + otp + "\"}")).andReturn();
    }

    private MvcResult reset(String token, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-resets").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reset_token\":\"" + token + "\",\"new_password\":\"" + newPassword + "\"}")).andReturn();
    }

    @Test
    void forgotPasswordFlow() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult oldSession = login(email, PASSWORD);

        MvcResult sent = sendOtp("PASSWORD_RESET", email);
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        MvcResult verified = verify(json(sent, "$.data.challenge_id"), mails.lastOtp(email));
        assertThat(verified.getResponse().getStatus()).isEqualTo(200);
        String resetToken = json(verified, "$.data.reset_token");
        assertThat(resetToken).startsWith("rst_");

        assertThat(json(reset(resetToken, PASSWORD), "$.code")).isEqualTo("GNE-AUTH-4223");   // trùng mật khẩu cũ
        assertThat(reset(resetToken, "NewPass@2026").getResponse().getStatus()).isEqualTo(204);
        assertThat(reset(resetToken, "Other@2026").getResponse().getStatus()).isEqualTo(410);   // dùng một lần

        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(401);
        assertThat(login(email, "NewPass@2026").getResponse().getStatus()).isEqualTo(201);
        MvcResult refreshOld = mvc.perform(post("/api/v1/auth/sessions/refresh").cookie(refreshCookieOf(oldSession)))
                .andReturn();
        assertThat(refreshOld.getResponse().getStatus()).isEqualTo(401);                        // phiên cũ bị thu hồi
    }

    @Test
    void forgotPasswordForUnknownEmailLooksTheSame() throws Exception {
        MvcResult sent = sendOtp("PASSWORD_RESET", uniqueEmail());
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        assertThat(json(sent, "$.data.challenge_id")).startsWith("otp_");
        assertThat(mails.sent()).isEmpty();
    }

    @Test
    void changePasswordRequiresStepUpOtp() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult phone = login(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        String body = "{\"current_password\":\"" + PASSWORD + "\",\"new_password\":\"Changed@2026\"}";

        MvcResult noOtp = mvc.perform(put("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        assertThat(noOtp.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(noOtp, "$.code")).isEqualTo("GNE-AUTH-4222");

        MvcResult challenge = mvc.perform(post("/api/v1/me/otp-challenges").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"PASSWORD_CHANGE\"}")).andReturn();
        assertThat(challenge.getResponse().getStatus()).isEqualTo(202);

        MvcResult changed = mvc.perform(put("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("X-OTP-Challenge-Id", json(challenge, "$.data.challenge_id"))
                .header("X-OTP-Code", mails.lastOtp(email))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        assertThat(changed.getResponse().getStatus()).isEqualTo(204);

        assertThat(login(email, "Changed@2026").getResponse().getStatus()).isEqualTo(201);
        assertThat(mvc.perform(post("/api/v1/auth/sessions/refresh").cookie(refreshCookieOf(phone))).andReturn()
                .getResponse().getStatus()).isEqualTo(401);                                     // phiên khác bị đăng xuất
    }

    @Test
    void weakNewPasswordDoesNotConsumeOtp() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        MvcResult challenge = mvc.perform(post("/api/v1/me/otp-challenges").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"PASSWORD_CHANGE\"}")).andReturn();
        String challengeId = json(challenge, "$.data.challenge_id");
        String otp = mails.lastOtp(email);

        MvcResult weak = mvc.perform(put("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("X-OTP-Challenge-Id", challengeId).header("X-OTP-Code", otp)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"current_password\":\"" + PASSWORD + "\",\"new_password\":\"weak\"}")).andReturn();
        assertThat(weak.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(weak, "$.code")).isEqualTo("GNE-VAL-4220");

        MvcResult ok = mvc.perform(put("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("X-OTP-Challenge-Id", challengeId).header("X-OTP-Code", otp)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"current_password\":\"" + PASSWORD + "\",\"new_password\":\"Strong@2026\"}")).andReturn();
        assertThat(ok.getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void verifyCurrentPassword() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult good = mvc.perform(post("/api/v1/me/password/verify").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + PASSWORD + "\"}")).andReturn();
        MvcResult bad = mvc.perform(post("/api/v1/me/password/verify").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Nope@123\"}")).andReturn();

        assertThat(json(good, "$.data.valid")).isEqualTo("true");
        assertThat(json(bad, "$.data.valid")).isEqualTo("false");
    }
}
