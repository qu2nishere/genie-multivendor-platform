package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.profile.application.StaffInvitationMail;
import com.genie.auth.profile.application.UserFacade;
import com.genie.auth.profile.domain.AccountStatus;
import com.genie.common.mail.MailTemplate;
import com.genie.support.FakeEmailSender;
import com.genie.support.IntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ADM-01 bước 5 — nhân viên kích hoạt lời mời (plan B1 R02.06; API 2.11): mã lời mời ở Redis
 * {@code auth:staff_invite:*}, OTP {@code STAFF_ACTIVATION}, đặt mật khẩu → {@code 201 $AuthTokens}.
 */
class StaffActivationIT extends IntegrationTestBase {

    private static final String NEW_PASSWORD = "Staff@2026";

    @Autowired
    private UserFacade userFacade;

    /** Tạo nhân viên chờ kích hoạt + gửi email mời; trả mã lời mời đọc từ liên kết trong email. */
    private String invite(String email) {
        long id = userFacade.createStaffAccount(email);
        userFacade.sendStaffInvitation(id, new StaffInvitationMail("Hồ Thanh Trúc", "Đặng Hải Yến", "CSKH"));
        FakeEmailSender.SentMail mail = mails.last(email, MailTemplate.STAFF_INVITATION).orElseThrow();
        String url = mail.variables().get("activationUrl").toString();
        assertThat(url).contains("/auth/staff-activation?token=inv_");
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private MvcResult requestOtp(String token) throws Exception {
        return mvc.perform(post("/api/v1/auth/staff-activations/otp-challenges").contentType(MediaType.APPLICATION_JSON)
                .content("{\"invitation_token\":\"" + token + "\"}")).andReturn();
    }

    private MvcResult activate(String token, String challengeId, String otp, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/staff-activations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"invitation_token\":\"" + token + "\",\"challenge_id\":\"" + challengeId
                        + "\",\"otp\":\"" + otp + "\",\"password\":\"" + password + "\"}")).andReturn();
    }

    @Test
    void invitedStaffActivatesWithOtpAndPassword() throws Exception {
        String email = uniqueEmail();
        String token = invite(email);
        assertThat(redis.keys("auth:staff_invite:*")).hasSize(2);   // khóa theo mã + khóa theo nhân viên
        assertThat(redis.getExpire("auth:staff_invite:user:" + userRepository.findByEmail(email).orElseThrow().getId()))
                .isBetween(47 * 3600L, 48 * 3600L);
        assertThat(login(email, NEW_PASSWORD).getResponse().getStatus()).isNotEqualTo(201);   // chưa kích hoạt

        MvcResult sent = requestOtp(token);
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        assertThat(json(sent, "$.data.purpose")).isEqualTo("STAFF_ACTIVATION");

        MvcResult done = activate(token, json(sent, "$.data.challenge_id"), mails.lastOtp(email), NEW_PASSWORD);
        assertThat(done.getResponse().getStatus()).isEqualTo(201);
        assertThat(done.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("genie_rt=");
        assertThat(json(done, "$.data.user.role")).isEqualTo("STAFF");
        assertThat(userRepository.findByEmail(email).orElseThrow().getStatus()).isEqualTo(AccountStatus.ACTIVE);

        MvcResult me = mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(accessTokenOf(done))))
                .andReturn();
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        assertThat(redis.keys("auth:staff_invite:*")).isEmpty();                              // mã dùng một lần
        assertThat(requestOtp(token).getResponse().getStatus()).isEqualTo(410);
        assertThat(login(email, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void unknownOrExpiredInvitationIsGone() throws Exception {
        MvcResult unknown = requestOtp("inv_khong-ton-tai");
        assertThat(unknown.getResponse().getStatus()).isEqualTo(410);
        assertThat(json(unknown, "$.code")).isEqualTo("GNE-AUTH-4103");

        String email = uniqueEmail();
        String token = invite(email);
        redis.delete(List.copyOf(redis.keys("auth:staff_invite:*")));                          // hết 48 giờ
        assertThat(json(requestOtp(token), "$.code")).isEqualTo("GNE-AUTH-4103");
    }

    @Test
    void resendInvalidatesTheOldLink() throws Exception {
        String email = uniqueEmail();
        String first = invite(email);
        long id = userRepository.findByEmail(email).orElseThrow().getId();
        userFacade.sendStaffInvitation(id, new StaffInvitationMail("Hồ Thanh Trúc", "Đặng Hải Yến", "CSKH"));

        assertThat(requestOtp(first).getResponse().getStatus()).isEqualTo(410);
        String url = mails.last(email, MailTemplate.STAFF_INVITATION).orElseThrow().variables().get("activationUrl")
                .toString();
        String second = url.substring(url.indexOf("token=") + "token=".length());
        assertThat(requestOtp(second).getResponse().getStatus()).isEqualTo(202);
    }

    @Test
    void wrongOtpAndWeakPasswordAreRejected() throws Exception {
        String email = uniqueEmail();
        String token = invite(email);
        String challengeId = json(requestOtp(token), "$.data.challenge_id");
        String wrong = mails.lastOtp(email).equals("000000") ? "111111" : "000000";

        MvcResult bad = activate(token, challengeId, wrong, NEW_PASSWORD);
        assertThat(bad.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(bad, "$.code")).isEqualTo("GNE-AUTH-4222");

        MvcResult weak = activate(token, challengeId, mails.lastOtp(email), "123");
        assertThat(weak.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(weak, "$.code")).isEqualTo("GNE-VAL-4220");
        assertThat(userRepository.findByEmail(email).orElseThrow().getStatus()).isEqualTo(AccountStatus.UNVERIFIED);
    }

    @Test
    void duplicateEmailCannotBeInvited() {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userFacade.createStaffAccount(email))
                .hasFieldOrPropertyWithValue("errorCode", com.genie.common.error.ErrorCode.STAFF_EMAIL_TAKEN);
    }
}
