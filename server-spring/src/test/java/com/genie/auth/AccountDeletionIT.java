package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.genie.auth.address.domain.AddressRepository;
import com.genie.auth.deletion.application.AnonymizeAccountsJob;
import com.genie.auth.profile.domain.AccountStatus;
import com.genie.auth.profile.domain.CustomerProfile;
import com.genie.auth.profile.domain.User;
import com.genie.common.mail.MailTemplate;
import com.genie.support.FakeEmailSender;
import com.genie.support.IntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** UC-AUTH-10 xóa tài khoản: yêu cầu có OTP, chặn khi còn đơn, hủy trong 30 ngày, cron ẩn danh (plan AUTH A14.06–A14.08). */
class AccountDeletionIT extends IntegrationTestBase {

    @Autowired
    AnonymizeAccountsJob job;

    @Autowired
    AddressRepository addressRepository;

    private MvcResult sendDeletionOtp(String token) throws Exception {
        return mvc.perform(post("/api/v1/me/otp-challenges").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"ACCOUNT_DELETION\"}")).andReturn();
    }

    private MvcResult confirmDeletion(String token, String challengeId, String otp) throws Exception {
        return mvc.perform(put("/api/v1/me/deletion-request").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header("X-OTP-Challenge-Id", challengeId).header("X-OTP-Code", otp)).andReturn();
    }

    /** Đăng nhập → OTP → xác nhận xóa; trả access token của phiên đã dùng. */
    private String requestDeletion(String email) throws Exception {
        String token = accessTokenOf(login(email, PASSWORD));
        MvcResult sent = sendDeletionOtp(token);
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        MvcResult done = confirmDeletion(token, json(sent, "$.data.challenge_id"), mails.lastOtp(email));
        assertThat(done.getResponse().getStatus()).isEqualTo(200);
        return token;
    }

    @Test
    void requestNeedsOtpAndSchedulesAnonymizationIn30Days() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult otherDevice = login(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult noOtp = mvc.perform(put("/api/v1/me/deletion-request")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
        assertThat(noOtp.getResponse().getStatus()).isEqualTo(422);                 // thiếu mã bước 2

        MvcResult sent = sendDeletionOtp(token);
        MvcResult done = confirmDeletion(token, json(sent, "$.data.challenge_id"), mails.lastOtp(email));
        assertThat(done.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(done, "$.data.requested_at")).startsWith("2026-06-08");
        assertThat(json(done, "$.data.anonymize_at")).startsWith("2026-07-08");

        // Thiết bị khác bị đăng xuất, phiên đang dùng vẫn xem được hồ sơ có băng "sắp xóa"
        assertThat(mvc.perform(post("/api/v1/auth/sessions/refresh").cookie(refreshCookieOf(otherDevice)))
                .andReturn().getResponse().getStatus()).isEqualTo(401);
        MvcResult profile = mvc.perform(get("/api/v1/me/profile").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn();
        assertThat(json(profile, "$.data.deletion_request.anonymize_at")).startsWith("2026-07-08");

        FakeEmailSender.SentMail mail = waitForMail(email, MailTemplate.DELETION_SCHEDULED);
        assertThat(mail.variables()).containsEntry("name", "Người dùng test").containsEntry("anonymizeAt", "08/07/2026")
                .containsEntry("graceDays", 30L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'ACCOUNT_DELETION_REQUESTED'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void activeOrdersBlockBeforeAnyCodeIsSent() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        deletionBlocker.blockWithOrders(user.getId(), "TR-20260608-000044");
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult blocked = sendDeletionOtp(token);

        assertThat(blocked.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(blocked, "$.code")).isEqualTo("GNE-USR-4091");
        assertThat(json(blocked, "$.details.active_order_codes[0]")).isEqualTo("TR-20260608-000044");
        assertThat(mails.last(email, MailTemplate.OTP)).isEmpty();
    }

    @Test
    void orderPlacedWhileTypingTheCodeStillBlocks() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        MvcResult sent = sendDeletionOtp(token);
        deletionBlocker.blockWithOrders(user.getId(), "TR-20260608-000045");

        MvcResult blocked = confirmDeletion(token, json(sent, "$.data.challenge_id"), mails.lastOtp(email));

        assertThat(blocked.getResponse().getStatus()).isEqualTo(409);
        assertThat(userRepository.findByEmail(email).orElseThrow().getDeletionRequestedAt()).isNull();
    }

    @Test
    void cancelWithinGracePeriodKeepsAccount() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = requestDeletion(email);

        MvcResult cancelled = mvc.perform(delete("/api/v1/me/deletion-request")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
        assertThat(cancelled.getResponse().getStatus()).isEqualTo(204);
        assertThat(mvc.perform(delete("/api/v1/me/deletion-request").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getStatus()).isEqualTo(204);                      // gọi lại vẫn 204

        clock.advance(Duration.ofDays(31));
        job.run();
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getDeletionRequestedAt()).isNull();
    }

    @Test
    void jobAnonymizesAfter30DaysAndFreesTheEmail() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        requestDeletion(email);

        clock.advance(Duration.ofDays(29));
        job.run();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);       // vẫn đăng nhập được

        clock.advance(Duration.ofDays(2));
        assertThat(job.run()).isGreaterThanOrEqualTo(1);

        User anonymized = userRepository.findById(user.getId()).orElseThrow();
        assertThat(anonymized.getStatus()).isEqualTo(AccountStatus.ANONYMIZED);
        assertThat(anonymized.getEmail()).isEqualTo("deleted_user_" + user.getId() + "@anonymized.local");
        assertThat(anonymized.getPassword()).isNull();
        CustomerProfile profile = customerProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getFullName()).isEqualTo("Người dùng đã xóa");
        assertThat(profile.getPhoneNumber()).isNull();
        assertThat(profile.getAnonymizedAt()).isNotNull();
        assertThat(addressRepository.findActiveByUserId(user.getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'PII_ANONYMIZED' "
                + "AND user_id = ?", Integer.class, user.getId())).isEqualTo(1);

        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(401);       // không đăng nhập được nữa
        // Giới hạn "60 giây mới gửi lại mã" nằm ở Redis (TTL giờ thật, không theo MutableClock): mã xóa tài khoản vừa
        // gửi tới email này vài giây trước → xóa khóa để giả lập đã qua 30 ngày như ngoài đời.
        redis.delete("rl:otp:" + email);
        MvcResult again = mvc.perform(post("/api/v1/auth/registrations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"full_name\":\"Khách mới\",\"email\":\"" + email + "\",\"phone_number\":\"0901234567\","
                        + "\"password\":\"NewUser@2026\",\"accept_terms\":true}")).andReturn();
        assertThat(again.getResponse().getStatus()).isEqualTo(202);                        // email cũ đăng ký mới được
        assertThat(job.run()).isZero();                                                     // không xử lý lại
    }

    /** Email gửi nền sau commit ({@code @Async}) — chờ tối đa 5 giây. */
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
}
