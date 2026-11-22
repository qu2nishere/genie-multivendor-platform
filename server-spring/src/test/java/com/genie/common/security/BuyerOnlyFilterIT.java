package com.genie.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.common.domain.UserRole;
import com.genie.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tài khoản nội bộ không mua hàng (plan B1 D16 hướng B): STAFF / ADMIN gọi giỏ hàng / đặt hàng → {@code 403 GNE-ORD-4031};
 * khách hàng đi qua bình thường (endpoint giỏ hàng chưa hiện thực nên không bị filter chặn — không phải 4031).
 */
class BuyerOnlyFilterIT extends IntegrationTestBase {

    @Autowired
    private JwtService jwtService;

    private String token(String email, UserRole role) {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        return "Bearer " + jwtService.issue(id, role, 1L).token();
    }

    @Test
    void internalAccountsCannotUseCartOrCheckout() throws Exception {
        String admin = token("yen.dh@genie.example", UserRole.ADMIN);
        String staff = token("phat.nt@genie.example", UserRole.STAFF);

        MvcResult add = mvc.perform(post("/api/v1/me/cart/items").header(HttpHeaders.AUTHORIZATION, staff)
                .contentType(MediaType.APPLICATION_JSON).content("{\"variant_id\":1,\"quantity\":1}")).andReturn();
        assertThat(add.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(add, "$.code")).isEqualTo("GNE-ORD-4031");

        MvcResult cart = mvc.perform(get("/api/v1/me/cart").header(HttpHeaders.AUTHORIZATION, admin)).andReturn();
        assertThat(json(cart, "$.code")).isEqualTo("GNE-ORD-4031");

        MvcResult order = mvc.perform(post("/api/v1/me/checkout-sessions").header(HttpHeaders.AUTHORIZATION, admin)
                .header("Idempotency-Key", "k-1").contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();
        assertThat(json(order, "$.code")).isEqualTo("GNE-ORD-4031");

        // Khu khác của /me vẫn dùng được (ví dụ ngữ cảnh phiên)
        assertThat(mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, admin)).andReturn()
                .getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void customersAreNotBlocked() throws Exception {
        var customer = createCustomer(uniqueEmail(), PASSWORD);
        String token = "Bearer " + jwtService.issue(customer.getId(), UserRole.CUSTOMER, 1L).token();
        MvcResult cart = mvc.perform(get("/api/v1/me/cart").header(HttpHeaders.AUTHORIZATION, token)).andReturn();
        assertThat(cart.getResponse().getStatus()).isNotEqualTo(403);
        assertThat(cart.getResponse().getContentAsString()).doesNotContain("GNE-ORD-4031");
    }
}
