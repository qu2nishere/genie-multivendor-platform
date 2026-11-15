package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.auth.profile.domain.User;
import com.genie.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-AUTH-01 — đăng ký + OTP (plan AUTH A06.05).
 */
class RegisterFlowIT extends IntegrationTestBase {

    private MvcResult register(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/registrations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"full_name":"Nguyễn Văn A","email":"%s","phone_number":"0901234567","password":"%s","accept_terms":true}
                        """.formatted(email, password)))
                .andReturn();
    }

    private MvcResult verify(String challengeId, String otp) throws Exception {
        return mvc.perform(post("/api/v1/auth/otp-challenges/" + challengeId + "/verify")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + otp + "\"}"))
                .andReturn();
    }

    @Test
    void registersAndLogsInAfterOtp() throws Exception {
        String email = uniqueEmail();
        MvcResult sent = register(email.toUpperCase(), PASSWORD);
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        assertThat(json(sent, "$.data.purpose")).isEqualTo("REGISTER");
        assertThat(json(sent, "$.data.expires_in")).isEqualTo("300");
        assertThat(userRepository.findByEmail(email)).isEmpty();           // chưa ghi gì vào PostgreSQL

        MvcResult done = verify(json(sent, "$.data.challenge_id"), mails.lastOtp(email));

        assertThat(done.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(done, "$.data.token_type")).isEqualTo("Bearer");
        assertThat(json(done, "$.data.user.role")).isEqualTo("CUSTOMER");
        assertThat(json(done, "$.data.user.full_name")).isEqualTo("Nguyễn Văn A");
        assertThat(refreshCookieOf(done)).isNotNull();
        assertThat(refreshCookieOf(done).isHttpOnly()).isTrue();
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(passwordEncoder.matches(PASSWORD, user.getPassword())).isTrue();
        assertThat(customerProfileRepository.findByUserId(user.getId())).isPresent();
    }

    @Test
    void rejectsExistingEmail() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);

        mvc.perform(post("/api/v1/auth/registrations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"full_name":"A","email":"%s","phone_number":"0901234567","password":"Genie@123","accept_terms":true}
                        """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-AUTH-4091"));
    }

    @Test
    void rejectsWeakPasswordWithFieldError() throws Exception {
        mvc.perform(post("/api/v1/auth/registrations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"full_name":"A","email":"%s","phone_number":"0901234567","password":"weak","accept_terms":true}
                        """.formatted(uniqueEmail())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"))
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].rejected_value").doesNotExist());
    }

    @Test
    void wrongOtpCountsDownThenCancelsRegistration() throws Exception {
        String email = uniqueEmail();
        String challengeId = json(register(email, PASSWORD), "$.data.challenge_id");
        String wrong = mails.lastOtp(email).equals("000000") ? "111111" : "000000";

        MvcResult first = verify(challengeId, wrong);
        assertThat(first.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(first, "$.code")).isEqualTo("GNE-AUTH-4222");
        assertThat(json(first, "$.details.remaining_attempts")).isEqualTo("4");

        for (int i = 0; i < 3; i++) {
            verify(challengeId, wrong);
        }
        MvcResult fifth = verify(challengeId, wrong);
        assertThat(json(fifth, "$.code")).isEqualTo("GNE-AUTH-4221");

        MvcResult after = verify(challengeId, mails.lastOtp(email));
        assertThat(after.getResponse().getStatus()).isEqualTo(410);
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    void otpIsSingleUse() throws Exception {
        String email = uniqueEmail();
        String challengeId = json(register(email, PASSWORD), "$.data.challenge_id");
        String otp = mails.lastOtp(email);

        assertThat(verify(challengeId, otp).getResponse().getStatus()).isEqualTo(201);
        assertThat(verify(challengeId, otp).getResponse().getStatus()).isEqualTo(410);
    }

    @Test
    void resendTooSoonIsRateLimited() throws Exception {
        String email = uniqueEmail();
        String challengeId = json(register(email, PASSWORD), "$.data.challenge_id");

        mvc.perform(post("/api/v1/auth/otp-challenges/" + challengeId + "/resend"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("GNE-RAT-4290"));
    }
}
