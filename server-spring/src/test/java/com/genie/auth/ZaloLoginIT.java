package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.oauth.application.IdentityClaims;
import com.genie.auth.profile.domain.User;
import com.genie.common.mail.MailTemplate;
import com.genie.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Đăng nhập / đăng ký / liên kết Zalo (UC-AUTH-03, plan AUTH A14.10, D23) — Zalo giả, không gọi mạng.
 * Zalo không trả email: tài khoản mới phải nhập email + OTP; không tự gộp theo email; liên kết phải nhập mật khẩu.
 */
class ZaloLoginIT extends IntegrationTestBase {

    /** Bắt đầu đăng nhập Zalo → lấy state từ URL "Zalo". */
    private String startLogin() throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/auth/oauth/zalo/authorize").param("next", "/cart")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(302);
        String location = r.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith("https://zalo.fake/auth");
        return UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state");
    }

    private MvcResult token(String code, String state) throws Exception {
        return mvc.perform(post("/api/v1/auth/oauth/zalo/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"state\":\"" + state + "\"}")).andReturn();
    }

    private MvcResult signup(String signupToken, String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/oauth/zalo/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"signup_token\":\"" + signupToken + "\",\"email\":\"" + email + "\"}")).andReturn();
    }

    private MvcResult verify(String challengeId, String otp) throws Exception {
        return mvc.perform(post("/api/v1/auth/otp-challenges/" + challengeId + "/verify")
                .contentType(MediaType.APPLICATION_JSON).content("{\"otp\":\"" + otp + "\"}")).andReturn();
    }

    private MvcResult startLink(String accessToken, String password) throws Exception {
        var req = post("/api/v1/me/identities/zalo/authorize").header(HttpHeaders.AUTHORIZATION, bearer(accessToken));
        if (password != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content("{\"current_password\":\"" + password + "\"}");
        }
        return mvc.perform(req).andReturn();
    }

    private static String stateOf(MvcResult authorize) throws Exception {
        String url = json(authorize, "$.data.authorize_url");
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("state");
    }

    @Test
    void newZaloUserMustVerifyEmailThenAccountIsCreated() throws Exception {
        String zaloId = "zalo-" + System.nanoTime();
        zalo.register("z-new", new IdentityClaims(zaloId, null, false, "Trần Thị Mai", null));

        MvcResult first = token("z-new", startLogin());
        assertThat(first.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(first, "$.data.result")).isEqualTo("SIGNUP_REQUIRED");
        assertThat(json(first, "$.data.next")).isEqualTo("/cart");
        assertThat(json(first, "$.data.signup.name")).isEqualTo("Trần Thị Mai");
        assertThat(first.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();             // chưa đăng nhập
        String signupToken = json(first, "$.data.signup.token");

        String email = uniqueEmail();
        MvcResult sent = signup(signupToken, email);
        assertThat(sent.getResponse().getStatus()).isEqualTo(202);
        assertThat(json(sent, "$.data.purpose")).isEqualTo("OAUTH_SIGNUP");
        assertThat(userRepository.findByEmail(email)).isEmpty();                                  // chưa ghi DB
        assertThat(mails.last(email, MailTemplate.OTP).orElseThrow().variables())
                .containsEntry("name", "Trần Thị Mai");

        MvcResult done = verify(json(sent, "$.data.challenge_id"), mails.lastOtp(email));
        assertThat(done.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(done, "$.data.access_token")).isNotBlank();
        assertThat(json(done, "$.data.user.full_name")).isEqualTo("Trần Thị Mai");

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getOauthProvider()).isEqualTo("zalo");
        assertThat(user.getOauthProviderId()).isEqualTo(zaloId);
        assertThat(user.hasPassword()).isFalse();
        assertThat(user.getEmailVerifiedAt()).isNotNull();

        // Lần sau đăng nhập Zalo → vào thẳng tài khoản
        zalo.register("z-again", new IdentityClaims(zaloId, null, false, "Trần Thị Mai", null));
        MvcResult again = token("z-again", startLogin());
        assertThat(json(again, "$.data.result")).isEqualTo("LOGGED_IN");
        assertThat(json(again, "$.data.tokens.access_token")).isNotBlank();

        // Màn Liên kết đăng nhập: Zalo, không email Zalo, có tên
        MvcResult methods = mvc.perform(get("/api/v1/me/login-methods")
                .header(HttpHeaders.AUTHORIZATION, bearer(json(done, "$.data.access_token")))).andReturn();
        assertThat(json(methods, "$.data.identities[0].provider")).isEqualTo("ZALO");
        assertThat(json(methods, "$.data.identities[0].display_name")).isEqualTo("Trần Thị Mai");
        assertThat(json(methods, "$.data.password.enabled")).isEqualTo("false");
    }

    @Test
    void emailOfExistingAccountIsNotMergedAutomatically() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        zalo.register("z-steal", new IdentityClaims("zalo-attacker-" + System.nanoTime(), null, false, "X", null));
        String signupToken = json(token("z-steal", startLogin()), "$.data.signup.token");

        MvcResult r = signup(signupToken, email.toUpperCase());
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4091");
        assertThat(json(r, "$.details.next_step")).isEqualTo("LINK_FROM_ACCOUNT");
        assertThat(mails.last(email, MailTemplate.OTP)).isEmpty();                                // không gửi mã
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isNull();
    }

    @Test
    void expiredOrForeignSignupTokenIsRejected() throws Exception {
        MvcResult r = signup("khong-ton-tai", uniqueEmail());
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4001");

        // mã phiên Zalo không dùng được ở đường dẫn Google
        zalo.register("z-x", new IdentityClaims("zalo-x-" + System.nanoTime(), null, false, "Y", null));
        String signupToken = json(token("z-x", startLogin()), "$.data.signup.token");
        MvcResult wrongProvider = mvc.perform(post("/api/v1/auth/oauth/google/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"signup_token\":\"" + signupToken + "\",\"email\":\"" + uniqueEmail() + "\"}")).andReturn();
        assertThat(json(wrongProvider, "$.code")).isEqualTo("GNE-AUTH-4001");
    }

    @Test
    void linkZaloRequiresPasswordThenSignInAndUnlink() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String accessToken = accessTokenOf(login(email, PASSWORD));

        MvcResult noPassword = startLink(accessToken, null);
        assertThat(noPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(noPassword, "$.code")).isEqualTo("GNE-AUTH-4011");
        assertThat(json(startLink(accessToken, "Wrong@123"), "$.code")).isEqualTo("GNE-AUTH-4011");

        MvcResult ok = startLink(accessToken, PASSWORD);
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        String zaloId = "zalo-link-" + System.nanoTime();
        zalo.register("z-link", new IdentityClaims(zaloId, null, false, "Mai Zalo", null));
        MvcResult linked = mvc.perform(post("/api/v1/me/identities/zalo")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"z-link\",\"state\":\"" + stateOf(ok) + "\"}")).andReturn();
        assertThat(linked.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(linked, "$.data.identities[0].provider")).isEqualTo("ZALO");
        assertThat(json(linked, "$.data.identities[0].display_name")).isEqualTo("Mai Zalo");
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProviderId()).isEqualTo(zaloId);

        // Đã gắn Zalo → bắt đầu liên kết lại bị chặn ngay
        assertThat(json(startLink(accessToken, PASSWORD), "$.code")).isEqualTo("GNE-AUTH-4092");

        // Đăng nhập bằng Zalo vừa gắn → vào đúng tài khoản
        zalo.register("z-login", new IdentityClaims(zaloId, null, false, "Mai Zalo", null));
        assertThat(json(token("z-login", startLogin()), "$.data.result")).isEqualTo("LOGGED_IN");

        // Gỡ liên kết cần mật khẩu
        MvcResult unlinked = mvc.perform(delete("/api/v1/me/identities/zalo")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"current_password\":\"" + PASSWORD + "\"}")).andReturn();
        assertThat(unlinked.getResponse().getStatus()).isEqualTo(204);
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isNull();
    }

    @Test
    void accountLinkedToGoogleCannotStartZaloLink() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        jdbc.update("UPDATE users SET oauth_provider = 'google', oauth_provider_id = ? WHERE email = ?",
                "sub-" + email, email);
        String accessToken = accessTokenOf(login(email, PASSWORD));

        MvcResult r = startLink(accessToken, PASSWORD);
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4092");
        assertThat(json(r, "$.details.reason")).isEqualTo("ACCOUNT_LINKED_TO_ANOTHER_GOOGLE");
    }
}
