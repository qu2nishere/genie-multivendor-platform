package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.genie.support.TestDataBuilder;
import com.genie.vendor.shop.application.VendorShopPorts;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/** Cổng cho module khác + ô chọn shop của Admin (plan Mở shop & KYC K10.01–K10.05). */
class ShopPortsIT extends KycTestSupport {

    @Autowired
    VendorShopPorts ports;

    private String staff(String email, UserRole role) {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        return "Bearer " + jwtService.issue(id, role, 1L).token();
    }

    @Test
    void vacationAndAccountShop() {
        TestDataBuilder data = new TestDataBuilder(jdbc);
        long resting = data.vendor();
        long open = data.vendor();
        jdbc.update("UPDATE vendor_profiles SET vacation_starts_at = ?, vacation_ends_at = ? WHERE user_id = ?",
                java.sql.Timestamp.from(clock.instant().minusSeconds(3600)),
                java.sql.Timestamp.from(clock.instant().plusSeconds(86400)), resting);
        assertThat(ports.vendorIdsOnVacation(List.of(resting, open))).containsExactly(resting);
        var shop = ports.shopOf(open).orElseThrow();
        assertThat(shop.id()).isEqualTo(data.shopIdOf(open));
        assertThat(shop.kycStatus()).isEqualTo("VERIFIED");
        assertThat(ports.shopOf(createCustomer(uniqueEmail(), PASSWORD).getId())).isEmpty();
    }

    @Test
    void adminShopPickerSearchesVerifiedShops() throws Exception {
        TestDataBuilder data = new TestDataBuilder(jdbc);
        long v = data.vendor();
        String name = jdbc.queryForObject("SELECT shop_name FROM vendor_profiles WHERE user_id = ?", String.class, v);
        long pending = data.vendor();
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'PENDING', shop_name = ? WHERE user_id = ?", name + " B", pending);
        mvc.perform(get("/api/v1/admin/shops").param("q", name.toLowerCase())
                        .header(HttpHeaders.AUTHORIZATION, staff("linh.tm@genie.example", UserRole.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(data.shopIdOf(v)))
                .andExpect(jsonPath("$.data[0].name").value(name));
        mvc.perform(get("/api/v1/admin/shops").header(HttpHeaders.AUTHORIZATION, staff("phat.nt@genie.example", UserRole.STAFF)))
                .andExpect(status().isForbidden());
    }
}
