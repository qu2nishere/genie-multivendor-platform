package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.oauth.application.IdentityClaims;
import com.genie.auth.profile.domain.User;
import com.genie.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Tài khoản › Liên kết đăng nhập (API_SPEC 3.3, plan AUTH A14.04): xem cách đăng nhập, liên kết / gỡ Google.
 * Google giả {@code FakeIdentityProviderClient}.
 */
class OAuthLinkIT extends IntegrationTestBase {

    private MvcResult methods(String token) throws Exception {
        return mvc.perform(get("/api/v1/me/login-methods").header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
    }

    /** Bắt đầu liên kết (cần đăng nhập) → URL "Google" → lấy state. */
    private String startLink(String token) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/me/identities/google/authorize")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        String url = json(r, "$.data.authorize_url");
        assertThat(url).startsWith("https://accounts.fake/auth");
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("state");
    }

    private MvcResult link(String token, String code, String state) throws Exception {
        return mvc.perform(post("/api/v1/me/identities/google").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"state\":\"" + state + "\"}")).andReturn();
    }

    private MvcResult unlink(String token, String currentPassword) throws Exception {
        return mvc.perform(delete("/api/v1/me/identities/google").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"current_password\":\"" + currentPassword + "\"}")).andReturn();
    }

    @Test
    void linkGoogleThenSignInWithItAndUnlink() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult before = methods(token);
        assertThat(json(before, "$.data.email")).isEqualTo(email);
        assertThat(json(before, "$.data.password.enabled")).isEqualTo("true");
        assertThat(json(before, "$.data.identities.length()")).isEqualTo("0");

        google.register("c-link-1", new IdentityClaims("sub-acc-" + email, email.toUpperCase(), true, "G", null));
        MvcResult linked = link(token, "c-link-1", startLink(token));
        assertThat(linked.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(linked, "$.data.identities[0].provider")).isEqualTo("GOOGLE");
        assertThat(json(linked, "$.data.identities[0].email")).isEqualToIgnoringCase(email);
        assertThat(json(linked, "$.data.identities[0].linked_at")).isNotBlank();

        // Đăng nhập bằng Google vừa gắn → vào đúng tài khoản này
        google.register("c-login", new IdentityClaims("sub-acc-" + email, email, true, "G", null));
        MvcResult r = mvc.perform(get("/api/v1/auth/oauth/google/authorize")).andReturn();
        String state = UriComponentsBuilder.fromUriString(r.getResponse().getHeader(HttpHeaders.LOCATION)).build()
                .getQueryParams().getFirst("state");
        MvcResult signIn = mvc.perform(post("/api/v1/auth/oauth/google/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"c-login\",\"state\":\"" + state + "\"}")).andReturn();
        assertThat(json(signIn, "$.data.result")).isEqualTo("LOGGED_IN");

        // Gỡ liên kết phải nhập lại mật khẩu: sai → 401, vẫn còn liên kết
        MvcResult wrong = unlink(token, "Wrong@123");
        assertThat(wrong.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(wrong, "$.code")).isEqualTo("GNE-AUTH-4011");
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isEqualTo("google");
        assertThat(mvc.perform(delete("/api/v1/me/identities/google").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getStatus()).isNotEqualTo(204);                      // thiếu mật khẩu

        assertThat(unlink(token, PASSWORD).getResponse().getStatus()).isEqualTo(204);
        assertThat(json(methods(token), "$.data.identities.length()")).isEqualTo("0");
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isNull();
        assertThat(unlink(token, PASSWORD).getResponse().getStatus()).isEqualTo(204);           // gỡ lại vẫn 204
    }

    @Test
    void googleWithAnotherEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        google.register("c-other", new IdentityClaims("sub-other-" + email, "nguoi.khac@gmail.com", true, "A", null));
        MvcResult r = link(token, "c-other", startLink(token));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4098");
        assertThat(json(r, "$.details.account_email")).isEqualTo(email);
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isNull();
        assertThat(json(methods(token), "$.data.identities.length()")).isEqualTo("0");      // lần thất bại không tính
    }

    @Test
    void googleOfAnotherGenieAccountCannotBeTaken() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String victim = uniqueEmail();
        createCustomer(victim, PASSWORD);                     // tài khoản Genie khác, chưa gắn Google
        String token = accessTokenOf(login(email, PASSWORD));

        google.register("c-victim", new IdentityClaims("sub-victim-" + victim, victim, true, "B", null));
        assertThat(json(link(token, "c-victim", startLink(token)), "$.code")).isEqualTo("GNE-AUTH-4098");

        // Chủ tài khoản kia đăng nhập bằng Google của họ → vẫn vào đúng tài khoản của họ (tự liên kết theo email)
        google.register("c-victim-login", new IdentityClaims("sub-victim-" + victim, victim, true, "B", null));
        MvcResult r = mvc.perform(get("/api/v1/auth/oauth/google/authorize")).andReturn();
        String state = UriComponentsBuilder.fromUriString(r.getResponse().getHeader(HttpHeaders.LOCATION)).build()
                .getQueryParams().getFirst("state");
        MvcResult signIn = mvc.perform(post("/api/v1/auth/oauth/google/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"c-victim-login\",\"state\":\"" + state + "\"}")).andReturn();
        assertThat(json(signIn, "$.data.result")).isEqualTo("LINKED");
        assertThat(userRepository.findByEmail(victim).orElseThrow().getOauthProviderId()).isEqualTo("sub-victim-" + victim);
    }

    @Test
    void googleOnlyAccountCannotUnlinkItsOnlySignInMethod() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, null);
        user.linkIdentity("google", "sub-only-" + email);
        userRepository.saveAndFlush(user);
        String token = json(signInWithGoogle("c-only", "sub-only-" + email, email), "$.data.tokens.access_token");

        MvcResult m = methods(token);
        assertThat(json(m, "$.data.password.enabled")).isEqualTo("false");
        assertThat(json(m, "$.data.identities[0].provider")).isEqualTo("GOOGLE");

        MvcResult r = unlink(token, "Any@1234");
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4093");
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isEqualTo("google");
    }

    @Test
    void googleAlreadyOwnedByAnotherUserIsRejected() throws Exception {
        String owner = uniqueEmail();
        User other = createCustomer(owner, PASSWORD);
        other.linkIdentity("google", "sub-taken");
        userRepository.saveAndFlush(other);
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        google.register("c-taken", new IdentityClaims("sub-taken", owner, true, "A", null));
        MvcResult r = link(token, "c-taken", startLink(token));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4092");
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isNull();
    }

    @Test
    void unverifiedGoogleEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));

        google.register("c-unverified", new IdentityClaims("sub-unv-" + email, "x@gmail.com", false, "A", null));
        MvcResult r = link(token, "c-unverified", startLink(token));

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4032");
    }

    @Test
    void loginAndLinkStatesCannotBeSwapped() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        String otherEmail = uniqueEmail();
        createCustomer(otherEmail, PASSWORD);
        String otherToken = accessTokenOf(login(otherEmail, PASSWORD));

        // State của luồng đăng nhập không dùng để liên kết được
        MvcResult r = mvc.perform(get("/api/v1/auth/oauth/google/authorize")).andReturn();
        String loginState = UriComponentsBuilder.fromUriString(r.getResponse().getHeader(HttpHeaders.LOCATION)).build()
                .getQueryParams().getFirst("state");
        google.register("c-swap-1", new IdentityClaims("sub-swap-1", email, true, "A", null));
        assertThat(json(link(token, "c-swap-1", loginState), "$.code")).isEqualTo("GNE-AUTH-4001");

        // State liên kết của người A: người B không dùng được, cũng không dùng để đăng nhập được
        String linkState = startLink(token);
        google.register("c-swap-2", new IdentityClaims("sub-swap-2", email, true, "A", null));
        assertThat(json(link(otherToken, "c-swap-2", linkState), "$.code")).isEqualTo("GNE-AUTH-4001");
        String linkState2 = startLink(token);
        MvcResult asLogin = mvc.perform(post("/api/v1/auth/oauth/google/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"c-swap-2\",\"state\":\"" + linkState2 + "\"}")).andReturn();
        assertThat(json(asLogin, "$.code")).isEqualTo("GNE-AUTH-4001");
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isNull();
    }

    @Test
    void unlinkPasswordAttemptsAreRateLimited() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        user.linkIdentity("google", "sub-rl-" + email);
        userRepository.saveAndFlush(user);
        String token = accessTokenOf(login(email, PASSWORD));

        for (int i = 0; i < 10; i++) {
            assertThat(unlink(token, "Wrong@123").getResponse().getStatus()).isEqualTo(401);
        }
        MvcResult limited = unlink(token, PASSWORD);                                              // lần 11 trong 1 phút
        assertThat(limited.getResponse().getStatus()).isEqualTo(429);
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isEqualTo("google");
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);              // không khóa tài khoản
    }

    @Test
    void loginMethodsNeedSignIn() throws Exception {
        assertThat(mvc.perform(get("/api/v1/me/login-methods")).andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(post("/api/v1/me/identities/google/authorize")).andReturn().getResponse().getStatus())
                .isEqualTo(401);
    }

    private MvcResult signInWithGoogle(String code, String sub, String email) throws Exception {
        google.register(code, new IdentityClaims(sub, email, true, "G", null));
        MvcResult r = mvc.perform(get("/api/v1/auth/oauth/google/authorize")).andReturn();
        String state = UriComponentsBuilder.fromUriString(r.getResponse().getHeader(HttpHeaders.LOCATION)).build()
                .getQueryParams().getFirst("state");
        MvcResult done = mvc.perform(post("/api/v1/auth/oauth/google/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"state\":\"" + state + "\"}")).andReturn();
        assertThat(done.getResponse().getStatus()).isEqualTo(200);
        return done;
    }

    @Test
    void internalAccountCannotStartLinkingGoogle() throws Exception {
        String email = uniqueEmail();
        long id = createCustomer(email, PASSWORD).getId();
        jdbc.update("UPDATE users SET role = 'STAFF' WHERE id = ?", id);
        String token = accessTokenOf(login(email, PASSWORD));

        MvcResult r = mvc.perform(post("/api/v1/me/identities/google/authorize")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4033");
    }
}
