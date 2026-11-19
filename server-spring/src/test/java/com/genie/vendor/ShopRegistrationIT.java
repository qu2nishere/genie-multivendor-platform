package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Mở shop — UC-VND-01 (API_SPEC 9.1–9.2, 10.1; plan Mở shop & KYC K03.05, K03.08). */
class ShopRegistrationIT extends IntegrationTestBase {

    @Autowired
    JwtService jwtService;

    private static String uniqueShop() {
        return "Shop Test " + UUID.randomUUID().toString().substring(0, 8);
    }

    private long categoryId;

    @org.junit.jupiter.api.BeforeEach
    void category() {
        categoryId = new TestDataBuilder(jdbc).category();
    }

    private String body(String shopName, String email, boolean terms) {
        return body(shopName, email, terms, categoryId);
    }

    private static String body(String shopName, String email, boolean terms, long category) {
        return "{\"shop_name\":\"" + shopName + "\",\"owner_name\":\"Đặng Quốc Huy\",\"phone_number\":\"0912345678\","
                + "\"email\":\"" + email + "\",\"main_category_id\":" + category + ",\"business_type\":\"BUSINESS\","
                + "\"accept_terms\":" + terms + "}";
    }

    private ResultActions register(String token, String shopName, String email) throws Exception {
        return mvc.perform(post("/api/v1/me/shop-registrations").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(body(shopName, email, true)));
    }

    private ResultActions confirm(String token, String challengeId, String otp) throws Exception {
        return mvc.perform(post("/api/v1/me/shop-registrations/" + challengeId + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"otp\":\"" + otp + "\"}"));
    }

    @Test
    void fullFlowCreatesShopPromotesAndKeepsSession() throws Exception {
        String email = uniqueEmail();
        long userId = createCustomer(email, PASSWORD).getId();
        MvcResult login = login(email, PASSWORD);
        String token = bearer(accessTokenOf(login));
        Cookie refreshCookie = refreshCookieOf(login);
        String shopEmail = "shop-" + email;
        String shopName = uniqueShop();

        MvcResult sent = register(token, shopName, shopEmail.toUpperCase())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.purpose").value("SHOP_REGISTRATION"))
                .andExpect(jsonPath("$.data.email_masked").exists())
                .andExpect(jsonPath("$.data.resend_after").value(60))
                .andReturn();
        String challenge = json(sent, "$.data.challenge_id");

        MvcResult done = confirm(token, challenge, mails.lastOtp(shopEmail))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.kyc_status").value("NOT_SUBMITTED"))
                .andReturn();
        long shopId = Long.parseLong(json(done, "$.data.shop_id"));

        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, userId)).isEqualTo("VENDOR");
        var row = jdbc.queryForMap("SELECT * FROM vendor_profiles WHERE id = ?", shopId);
        assertThat(row.get("user_id")).isEqualTo(userId);
        assertThat(row.get("shop_name")).isEqualTo(shopName);
        assertThat(row.get("representative")).isEqualTo("Đặng Quốc Huy");
        assertThat(row.get("contact_phone")).isEqualTo("0912345678");
        assertThat(row.get("contact_email")).isEqualTo(shopEmail);
        assertThat(row.get("commission_rate")).isNull();
        assertThat(row.get("ghn_shop_id")).isNull();
        // K03.06 / K09.02: cấu hình vận chuyển mặc định (GHN, GHTK bật) được tạo sau commit
        var cfg = jdbc.queryForMap("SELECT ghn_enabled, ghtk_enabled, vtp_enabled FROM vendor_shipping_config WHERE vendor_id = ?",
                userId);
        assertThat(cfg.get("ghn_enabled")).isEqualTo(true);
        assertThat(cfg.get("vtp_enabled")).isEqualTo(false);

        // D11: phiên KHÔNG bị thu hồi — refresh cấp token mang VENDOR và vào được Seller Center
        MvcResult refreshed = mvc.perform(post("/api/v1/auth/sessions/refresh").cookie(refreshCookie)).andReturn();
        assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);
        String vendorToken = bearer(accessTokenOf(refreshed));
        mvc.perform(get("/api/v1/seller/me").header(HttpHeaders.AUTHORIZATION, vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shop.id").value(shopId))
                .andExpect(jsonPath("$.data.shop.name").value(shopName))
                .andExpect(jsonPath("$.data.shop.is_mall").value(false))
                .andExpect(jsonPath("$.data.kyc_status").value("NOT_SUBMITTED"))
                .andExpect(jsonPath("$.data.badges.orders_to_confirm").value(0))
                .andExpect(jsonPath("$.data.badges.unread_chats").value(0));
        // vẫn mua hàng bình thường (VENDOR ⊃ CUSTOMER) bằng cả token cũ lẫn token mới
        mvc.perform(get("/api/v1/me/following").header(HttpHeaders.AUTHORIZATION, vendorToken)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/following").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        // đã có shop
        register(vendorToken, uniqueShop(), "other-" + shopEmail)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-VND-4093"));
    }

    @Test
    void duplicateOrBannedNamesAreRejected() throws Exception {
        long vendor = new TestDataBuilder(jdbc).vendor();
        String existing = jdbc.queryForObject("SELECT shop_name FROM vendor_profiles WHERE user_id = ?", String.class, vendor);
        String token = bearer(jwtService.issue(createCustomer(uniqueEmail(), PASSWORD).getId(), UserRole.CUSTOMER, 1L).token());
        register(token, "  " + existing.toUpperCase().replace(" ", "   ") + " ", uniqueEmail())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-VND-4094"));
        register(token, "Hàng Giả Chính Hãng", uniqueEmail())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-VND-4094"));
        mvc.perform(post("/api/v1/me/shop-registrations").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(uniqueShop(), uniqueEmail(), false)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/me/shop-registrations").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body(uniqueShop(), uniqueEmail(), true, 999_999)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.field").value("main_category_id"));
        assertThat(mails.sent()).isEmpty();
    }

    @Test
    void fiveWrongCodesCancelTheRegistration() throws Exception {
        long userId = createCustomer(uniqueEmail(), PASSWORD).getId();
        String token = bearer(jwtService.issue(userId, UserRole.CUSTOMER, 1L).token());
        String shopEmail = uniqueEmail();
        String challenge = json(register(token, uniqueShop(), shopEmail).andReturn(), "$.data.challenge_id");
        String wrong = "000000".equals(mails.lastOtp(shopEmail)) ? "111111" : "000000";
        for (int i = 1; i <= 4; i++) {
            confirm(token, challenge, wrong).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("GNE-AUTH-4222"));
        }
        confirm(token, challenge, wrong).andExpect(jsonPath("$.code").value("GNE-AUTH-4221"));
        confirm(token, challenge, mails.lastOtp(shopEmail)).andExpect(status().isGone());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vendor_profiles WHERE user_id = ?", Integer.class, userId))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, userId)).isEqualTo("CUSTOMER");
    }

    @Test
    void concurrentConfirmsCreateOneShop() throws Exception {
        long userId = createCustomer(uniqueEmail(), PASSWORD).getId();
        String token = bearer(jwtService.issue(userId, UserRole.CUSTOMER, 1L).token());
        String shopEmail = uniqueEmail();
        String challenge = json(register(token, uniqueShop(), shopEmail).andReturn(), "$.data.challenge_id");
        String otp = mails.lastOtp(shopEmail);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Callable<Integer> c = () -> {
                    go.await();
                    return confirm(token, challenge, otp).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(c));
            }
            go.countDown();
            int created = 0;
            for (Future<Integer> f : results) {
                created += f.get() == 201 ? 1 : 0;
            }
            assertThat(created).isEqualTo(1);
        } finally {
            pool.shutdown();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vendor_profiles WHERE user_id = ?", Integer.class, userId))
                .isEqualTo(1);
    }

    @Test
    void staffCannotOpenAShop() throws Exception {
        Long staff = jdbc.queryForObject("SELECT id FROM users WHERE email = 'phat.nt@genie.example'", Long.class);
        register(bearer(jwtService.issue(staff, UserRole.STAFF, 1L).token()), uniqueShop(), uniqueEmail())
                .andExpect(status().isForbidden());
    }
}
