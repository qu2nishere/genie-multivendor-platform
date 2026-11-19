package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/** Cổng KYC của Seller Center (API_SPEC 0.9; plan Mở shop & KYC K02.09). */
class KycGateIT extends IntegrationTestBase {

    private static final String PROBE = "/api/v1/seller/__kyc-gate-probe";

    @Autowired
    JwtService jwtService;

    private String token(long userId, UserRole role) {
        return "Bearer " + jwtService.issue(userId, role, 1L).token();
    }

    private long vendor(String kycStatus) {
        long id = new TestDataBuilder(jdbc).vendor();
        jdbc.update("UPDATE vendor_profiles SET kyc_status = ? WHERE user_id = ?", kycStatus, id);
        return id;
    }

    @Test
    void unverifiedShopIsBlockedExceptOpenScreens() throws Exception {
        long v = vendor("PENDING");
        String t = token(v, UserRole.VENDOR);
        mvc.perform(get(PROBE).header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GNE-VND-4031"))
                .andExpect(jsonPath("$.details.kyc_status").value("PENDING"));
        // các màn mở khi chưa VERIFIED: không bị cổng chặn (endpoint chưa có thì 404, có rồi thì 2xx / 4xx nghiệp vụ)
        String[][] open = {{"GET", "/api/v1/seller/me"}, {"GET", "/api/v1/seller/kyc"}, {"PUT", "/api/v1/seller/kyc/draft"},
                {"GET", "/api/v1/seller/settings"}, {"PATCH", "/api/v1/seller/settings"},
                {"PUT", "/api/v1/seller/pickup-address"}, {"POST", "/api/v1/seller/payout-account/verification"}};
        for (String[] o : open) {
            String code = mvc.perform(request(HttpMethod.valueOf(o[0]), o[1]).header(HttpHeaders.AUTHORIZATION, t)
                            .contentType("application/json").content("{}"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(code).as(o[0] + " " + o[1]).doesNotContain("GNE-VND-4031");
        }
    }

    @Test
    void verifiedShopPassesAndOthersAreForbidden() throws Exception {
        long v = vendor("VERIFIED");
        mvc.perform(get(PROBE).header(HttpHeaders.AUTHORIZATION, token(v, UserRole.VENDOR)))
                .andExpect(status().isOk());

        long customer = createCustomer(uniqueEmail(), PASSWORD).getId();
        mvc.perform(get(PROBE).header(HttpHeaders.AUTHORIZATION, token(customer, UserRole.CUSTOMER)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GNE-SEC-4030"));
        // vai trò VENDOR nhưng không có shop
        mvc.perform(get(PROBE).header(HttpHeaders.AUTHORIZATION, token(customer, UserRole.VENDOR)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GNE-SEC-4030"));
    }
}
