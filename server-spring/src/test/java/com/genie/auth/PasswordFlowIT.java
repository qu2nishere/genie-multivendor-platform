package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.genie.common.mail.MailTemplate;
import com.genie.support.FakeEmailSender;
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
        String email = uniqueEmail();
        MvcResult sent = sendOtp("PASSWORD_RESET", email);
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        assertThat(json(sent, "$.data.challenge_id")).startsWith("otp_");
        assertThat(mailsTo(email)).isEmpty();
    }

    @Test
    void fiveWrongCodesLockRecoveryForThirtyMinutes() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult sent = sendOtp("PASSWORD_RESET", email);
        String challengeId = json(sent, "$.data.challenge_id");
        String wrong = mails.lastOtp(email).equals("000000") ? "111111" : "000000";

        for (int i = 1; i <= 4; i++) {
            MvcResult r = verify(challengeId, wrong);
            assertThat(r.getResponse().getStatus()).isEqualTo(422);
            assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4222");
            assertThat(json(r, "$.details.remaining_attempts")).isEqualTo(String.valueOf(5 - i));
        }
        MvcResult locked = verify(challengeId, wrong);
        assertThat(json(locked, "$.code")).isEqualTo("GNE-AUTH-4221");
        assertThat(json(locked, "$.details.locked_until")).isNotBlank();
        assertThat(redis.hasKey("auth:pwreset_lock:" + email)).isTrue();
        Long ttl = redis.getExpire("auth:pwreset_lock:" + email);
        assertThat(ttl).isBetween(29 * 60L, 30 * 60L);

        // Đang bị khóa: xin mã lại (sau 60 giây chờ gửi lại) vẫn 202 — không lộ trạng thái — nhưng không gửi thư.
        mails.clear();
        redis.delete("rl:otp:" + email);
        MvcResult again = sendOtp("PASSWORD_RESET", email);
        assertThat(again.getResponse().getStatus()).isEqualTo(202);
        assertThat(json(again, "$.data.challenge_id")).startsWith("otp_");
        assertThat(mailsTo(email)).isEmpty();

        // Hết 30 phút khóa → xin mã được lại.
        redis.delete("auth:pwreset_lock:" + email);
        redis.delete("rl:otp:" + email);
        sendOtp("PASSWORD_RESET", email);
        assertThat(mails.lastOtp(email)).hasSize(6);
    }

    @Test
    void googleOnlyAccountSetsUpPasswordThroughForgotFlow() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, null);   // tạo bằng Google, chưa thiết lập mật khẩu

        MvcResult sent = sendOtp("PASSWORD_RESET", email);
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        String token = json(verify(json(sent, "$.data.challenge_id"), mails.lastOtp(email)), "$.data.reset_token");
        assertThat(reset(token, "NewPass@2026").getResponse().getStatus()).isEqualTo(204);

        assertThat(login(email, "NewPass@2026").getResponse().getStatus()).isEqualTo(201);
        FakeEmailSender.SentMail mail = waitForMail(email, MailTemplate.PASSWORD_CHANGED);
        assertThat(mail.variables()).containsEntry("method", "SETUP");
    }

    @Test
    void resetSendsBrandedSecurityEmailWithDevice() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult sent = sendOtp("PASSWORD_RESET", email);
        String token = json(verify(json(sent, "$.data.challenge_id"), mails.lastOtp(email)), "$.data.reset_token");

        MvcResult done = mvc.perform(post("/api/v1/auth/password-resets").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 "
                        + "(KHTML, like Gecko) Version/18.0 Safari/605.1.15")
                .content("{\"reset_token\":\"" + token + "\",\"new_password\":\"NewPass@2026\"}")).andReturn();
        assertThat(done.getResponse().getStatus()).isEqualTo(204);

        FakeEmailSender.SentMail mail = waitForMail(email, MailTemplate.PASSWORD_CHANGED);
        assertThat(mail.variables()).containsEntry("name", "Người dùng test").containsEntry("method", "RESET")
                .containsEntry("email", email);
        assertThat((String) mail.variables().get("device")).contains("Safari");
        assertThat((String) mail.variables().get("changedAt")).isNotBlank();
    }

    /** Thư gửi tới {@code to} — bỏ qua email cảnh báo gửi nền của test khác đến muộn. */
    private java.util.List<FakeEmailSender.SentMail> mailsTo(String to) {
        return mails.sent().stream().filter(m -> m.to().equals(to)).toList();
    }

    /** Email cảnh báo gửi nền sau commit ({@code @Async}) — chờ tối đa 5 giây. */
    private FakeEmailSender.SentMail waitForMail(String to, MailTemplate template) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            var mail = mails.last(to, template);
            if (mail.isPresent()) {
                return mail.get();
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Chưa có email " + template + " gửi tới " + to);
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
