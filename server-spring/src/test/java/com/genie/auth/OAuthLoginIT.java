package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.auth.oauth.application.IdentityClaims;
import com.genie.auth.profile.domain.User;
import com.genie.support.IntegrationTestBase;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/** UC-AUTH-03 đăng nhập Google (plan AUTH A14.03, A14.05) — Google giả {@code FakeIdentityProviderClient}. */
class OAuthLoginIT extends IntegrationTestBase {

    /** Bắt đầu đăng nhập → 302 sang "Google" → lấy state trên URL. */
    private String start(String next) throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/auth/oauth/google/authorize").param("next", next)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(302);
        String location = r.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith("https://accounts.fake/auth")
                .contains("redirect_uri=http://localhost:4321/auth/oauth-callback");
        return UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state");
    }

    private MvcResult token(String code, String state) throws Exception {
        return mvc.perform(post("/api/v1/auth/oauth/google/token").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"state\":\"" + state + "\"}")).andReturn();
    }

    private MvcResult signIn(String code, IdentityClaims claims) throws Exception {
        google.register(code, claims);
        return token(code, start("/cart"));
    }

    @Test
    void newGoogleUserGetsAccount() throws Exception {
        String email = uniqueEmail();
        MvcResult r = signIn("c-new", new IdentityClaims("sub-new-" + email, email, true, "Trần Google",
                "https://lh3.googleusercontent.com/a/photo"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(r, "$.data.result")).isEqualTo("CREATED");
        assertThat(json(r, "$.data.next")).isEqualTo("/cart");
        assertThat(json(r, "$.data.tokens.access_token")).isNotBlank();
        assertThat(json(r, "$.data.tokens.user.full_name")).isEqualTo("Trần Google");
        assertThat(refreshCookieOf(r)).isNotNull();
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getOauthProvider()).isEqualTo("google");
        assertThat(user.hasPassword()).isFalse();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        String avatar = customerProfileRepository.findByUserId(user.getId()).orElseThrow().getAvatarUrl();
        String prefix = MINIO.getS3URL() + "/avatar-assets/";
        assertThat(avatar).startsWith(prefix + "user/" + user.getId() + "/").endsWith(".png");   // ảnh Google lưu ở Silo
        assertThat(existsInSilo(avatar.substring(prefix.length()))).isTrue();
    }

    private static boolean existsInSilo(String key) throws Exception {
        try {
            minioClient().statObject(StatObjectArgs.builder().bucket("avatar-assets").object(key).build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        }
    }

    @Test
    void existingAccountWithoutAvatarGetsGoogleAvatarButKeepsName() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);                                           // tên "Người dùng test", chưa có ảnh

        signIn("c-fill", new IdentityClaims("sub-fill-" + email, email, true, "Tên Google",
                "https://lh3.googleusercontent.com/a/photo=s96-c"));

        var profile = customerProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getFullName()).isEqualTo("Người dùng test");                         // đã có tên → giữ
        assertThat(profile.getAvatarUrl()).startsWith(MINIO.getS3URL() + "/avatar-assets/user/" + user.getId() + "/");
    }

    @Test
    void existingAvatarIsNotReplaced() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        var profile = customerProfileRepository.findByUserId(user.getId()).orElseThrow();
        profile.changeAvatar("https://s3.genie.vn/avatar-assets/user/1/cu.jpg");
        customerProfileRepository.saveAndFlush(profile);

        signIn("c-keep", new IdentityClaims("sub-keep-" + email, email, true, "Tên Google",
                "https://lh3.googleusercontent.com/a/photo"));

        assertThat(customerProfileRepository.findByUserId(user.getId()).orElseThrow().getAvatarUrl())
                .isEqualTo("https://s3.genie.vn/avatar-assets/user/1/cu.jpg");
    }

    @Test
    void brokenGoogleAvatarDoesNotBlockSignIn() throws Exception {
        String email = uniqueEmail();
        MvcResult r = signIn("c-broken", new IdentityClaims("sub-broken-" + email, email, true, "B",
                "https://lh3.googleusercontent.com/a/broken"));

        assertThat(json(r, "$.data.result")).isEqualTo("CREATED");
        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(customerProfileRepository.findByUserId(user.getId()).orElseThrow().getAvatarUrl()).isNull();
    }

    @Test
    void registeredEmailIsLinkedAndKeepsPassword() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);

        MvcResult r = signIn("c-link", new IdentityClaims("sub-link-" + email, email.toUpperCase(), true, "Tên Google", null));

        assertThat(json(r, "$.data.result")).isEqualTo("LINKED");
        assertThat(json(r, "$.data.tokens.user.full_name")).isEqualTo("Người dùng test");      // không ghi đè hồ sơ
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProviderId()).isEqualTo("sub-link-" + email);
        assertThat(login(email, PASSWORD).getResponse().getStatus()).isEqualTo(201);              // mật khẩu vẫn dùng được

        // Lần sau: tìm theo sub — kể cả khi email Google đã đổi
        MvcResult again = signIn("c-again", new IdentityClaims("sub-link-" + email, "doi.email@gmail.com", true, null, null));
        assertThat(json(again, "$.data.result")).isEqualTo("LOGGED_IN");
    }

    @Test
    void stateIsSingleUseAndMustExist() throws Exception {
        String email = uniqueEmail();
        google.register("c-once", new IdentityClaims("sub-once-" + email, email, true, "A", null));
        String state = start("/");

        assertThat(token("c-once", state).getResponse().getStatus()).isEqualTo(200);
        MvcResult reused = token("c-once", state);
        assertThat(reused.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(reused, "$.code")).isEqualTo("GNE-AUTH-4001");
        assertThat(json(token("c-once", "khong-ton-tai"), "$.code")).isEqualTo("GNE-AUTH-4001");
    }

    @Test
    void unverifiedGoogleEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        MvcResult r = signIn("c-unverified", new IdentityClaims("sub-u-" + email, email, false, "A", null));

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4032");
        assertThat(userRepository.findByEmail(email)).isEmpty();
    }

    @Test
    void suspendedAccountIsNotLinked() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        user.suspend("Vi phạm chính sách", OffsetDateTime.now(clock).plusDays(3));
        userRepository.saveAndFlush(user);

        MvcResult r = signIn("c-suspended", new IdentityClaims("sub-s-" + email, email, true, "A", null));

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4031");
        assertThat(userRepository.findByEmail(email).orElseThrow().getOauthProvider()).isNull();
    }

    @Test
    void accountAlreadyLinkedToAnotherGoogleIsRejected() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        signIn("c-first", new IdentityClaims("sub-first-" + email, email, true, "A", null));

        MvcResult r = signIn("c-second", new IdentityClaims("sub-second-" + email, email, true, "A", null));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(r, "$.code")).isEqualTo("GNE-AUTH-4092");
    }

    @Test
    void externalNextIsDropped() throws Exception {
        String email = uniqueEmail();
        google.register("c-next", new IdentityClaims("sub-next-" + email, email, true, "A", null));
        MvcResult r = token("c-next", start("//evil.com"));
        assertThat(r.getResponse().getContentAsString()).doesNotContain("\"next\"");
    }

    @Test
    void unknownProviderRedirectsBackWithError() throws Exception {
        MvcResult r = mvc.perform(get("/api/v1/auth/oauth/facebook/authorize")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(302);
        assertThat(r.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith("http://localhost:4321/auth/oauth-callback?error=");
    }
}
