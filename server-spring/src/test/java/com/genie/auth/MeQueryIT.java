package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.genie.auth.profile.application.AccountShopProvider;
import com.genie.auth.profile.domain.User;
import com.genie.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/** {@code GET /me} — ngữ cảnh phiên (API_SPEC 3.1, plan AUTH A11.02, D13). */
class MeQueryIT extends IntegrationTestBase {

    private MvcResult me(String token) throws Exception {
        return mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn();
    }

    @Test
    void customerWithoutShopGetsDefaults() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        MvcResult r = me(accessTokenOf(login(email, PASSWORD)));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(r.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(json(r, "$.data.id")).isEqualTo(String.valueOf(user.getId()));
        assertThat(json(r, "$.data.role")).isEqualTo("CUSTOMER");
        assertThat(json(r, "$.data.email")).isEqualTo(email);
        assertThat(json(r, "$.data.full_name")).isNotBlank();
        assertThat(json(r, "$.data.login_methods.password")).isEqualTo("true");
        assertThat(json(r, "$.data.login_methods.google")).isEqualTo("false");
        assertThat(json(r, "$.data.login_methods.zalo")).isEqualTo("false");
        assertThat(json(r, "$.data.shop")).isNull();
        assertThat(json(r, "$.data.counters.cart_items")).isEqualTo("0");
        assertThat(json(r, "$.data.counters.unread_notifications")).isEqualTo("0");
        assertThat(json(r, "$.data.counters.unread_chats")).isEqualTo("0");
        assertThat(json(r, "$.data.counters.available_vouchers")).isEqualTo("0");
        assertThat(json(r, "$.data.counters.following_shops")).isEqualTo("0");
    }

    @Test
    void shopAndCountersComeFromOtherModules() throws Exception {
        String email = uniqueEmail();
        User user = createCustomer(email, PASSWORD);
        accountContext.giveShop(user.getId(),
                new AccountShopProvider.ShopSummary(15, "TechZone Official", null, "VERIFIED"));
        accountContext.giveCounters(user.getId(), Map.of("cart_items", 11L, "unread_notifications", 3L,
                "khong_ho_tro", 99L));

        MvcResult r = me(accessTokenOf(login(email, PASSWORD)));

        assertThat(json(r, "$.data.shop.id")).isEqualTo("15");
        assertThat(json(r, "$.data.shop.name")).isEqualTo("TechZone Official");
        assertThat(json(r, "$.data.shop.kyc_status")).isEqualTo("VERIFIED");
        assertThat(json(r, "$.data.counters.cart_items")).isEqualTo("11");
        assertThat(json(r, "$.data.counters.unread_notifications")).isEqualTo("3");
        assertThat(json(r, "$.data.counters.available_vouchers")).isEqualTo("0");
    }

    @Test
    void socialOnlyAccountHasNoPassword() throws Exception {
        String email = uniqueEmail();
        createCustomer(email, PASSWORD);
        String token = accessTokenOf(login(email, PASSWORD));
        jdbc.update("UPDATE users SET password = NULL, oauth_provider = 'zalo', oauth_provider_id = ? WHERE email = ?",
                "zalo-" + email, email);

        MvcResult r = me(token);
        assertThat(json(r, "$.data.login_methods.password")).isEqualTo("false");
        assertThat(json(r, "$.data.login_methods.zalo")).isEqualTo("true");
        assertThat(json(r, "$.data.login_methods.google")).isEqualTo("false");
    }

    @Test
    void requiresSignIn() throws Exception {
        assertThat(mvc.perform(get("/api/v1/me")).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
}
