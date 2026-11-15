package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-AUTH-02 / UC-AUTH-11 — xoay vòng refresh token, phát hiện dùng lại, đăng xuất, thiết bị đăng nhập
 * (plan AUTH A07.05–A07.09, A07.11).
 */
class SessionFlowIT extends IntegrationTestBase {

    private MvcResult refresh(Cookie cookie) throws Exception {
        var request = post("/api/v1/auth/sessions/refresh");
        if (cookie != null) {
            request.cookie(cookie);
        }
        return mvc.perform(request).andReturn();
    }

    @Test
    void refreshRotatesCookieAndKeepsSession() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult loggedIn = login(email, PASSWORD);
        Cookie first = refreshCookieOf(loggedIn);

        MvcResult refreshed = refresh(first);

        assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(refreshed, "$.data.access_token")).isNotBlank();
        assertThat(json(refreshed, "$.data.user.role")).isEqualTo("CUSTOMER");
        Cookie second = refreshCookieOf(refreshed);
        assertThat(second.getValue()).isNotEqualTo(first.getValue());

        MvcResult sessions = mvc.perform(get("/api/v1/me/sessions")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessTokenOf(refreshed)))).andReturn();
        assertThat(json(sessions, "$.data.length()")).isEqualTo("1");
    }

    @Test
    void missingCookieIsUnauthorized() throws Exception {
        MvcResult result = refresh(null);
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(result, "$.code")).isEqualTo("GNE-SEC-4010");
    }

    @Test
    void reuseInsideGraceDoesNotRevokeEverything() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        Cookie first = refreshCookieOf(login(email, PASSWORD));
        Cookie second = refreshCookieOf(refresh(first));

        clock.advance(Duration.ofSeconds(3));
        MvcResult reused = refresh(first);
        assertThat(reused.getResponse().getStatus()).isEqualTo(401);
        assertThat(reused.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();   // không xóa cookie mới của tab kia

        assertThat(refresh(second).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void reuseAfterGraceRevokesAllSessionsAndAccessTokens() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult loggedIn = login(email, PASSWORD);
        Cookie first = refreshCookieOf(loggedIn);
        MvcResult rotated = refresh(first);
        String accessToken = accessTokenOf(rotated);

        clock.advance(Duration.ofSeconds(30));
        assertThat(refresh(first).getResponse().getStatus()).isEqualTo(401);   // token cũ bị dùng lại → nghi đánh cắp

        assertThat(refresh(refreshCookieOf(rotated)).getResponse().getStatus()).isEqualTo(401);
        mvc.perform(get("/api/v1/me/sessions").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    @Test
    void logoutRevokesSessionAndAccessToken() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult loggedIn = login(email, PASSWORD);
        String token = accessTokenOf(loggedIn);

        MvcResult out = mvc.perform(delete("/api/v1/auth/sessions/current")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)).cookie(refreshCookieOf(loggedIn))).andReturn();

        assertThat(out.getResponse().getStatus()).isEqualTo(204);
        assertThat(refreshCookieOf(out).getMaxAge()).isZero();
        assertThat(refresh(refreshCookieOf(loggedIn)).getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(get("/api/v1/me/sessions").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void manageOtherDevices() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        MvcResult phone = login(email, PASSWORD);
        MvcResult laptop = login(email, PASSWORD);
        String laptopToken = accessTokenOf(laptop);

        MvcResult list = mvc.perform(get("/api/v1/me/sessions").header(HttpHeaders.AUTHORIZATION, bearer(laptopToken)))
                .andReturn();
        assertThat(json(list, "$.data.length()")).isEqualTo("2");
        assertThat(json(list, "$.data[0].is_current")).isEqualTo("true");
        String currentId = json(list, "$.data[0].id");
        String otherId = json(list, "$.data[1].id");

        assertThat(mvc.perform(delete("/api/v1/me/sessions/" + currentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(laptopToken)))
                .andReturn().getResponse().getStatus()).isEqualTo(409);

        assertThat(mvc.perform(delete("/api/v1/me/sessions/" + otherId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(laptopToken)))
                .andReturn().getResponse().getStatus()).isEqualTo(204);
        assertThat(refresh(refreshCookieOf(phone)).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void cannotRevokeSomeoneElsesSession() throws Exception {
        String a = uniqueEmail();
        String b = uniqueEmail();
        createCustomer(a, PASSWORD);
        createCustomer(b, PASSWORD);
        String tokenA = accessTokenOf(login(a, PASSWORD));
        String tokenB = accessTokenOf(login(b, PASSWORD));
        String sessionB = json(mvc.perform(get("/api/v1/me/sessions").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andReturn(), "$.data[0].id");

        assertThat(mvc.perform(delete("/api/v1/me/sessions/" + sessionB).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
    }
}
