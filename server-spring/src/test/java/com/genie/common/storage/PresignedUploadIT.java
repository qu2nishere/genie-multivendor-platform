package com.genie.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Purpose tải tệp của người bán: KYC_DOCUMENT, SHOP_LOGO, SHOP_COVER (API 4.1; plan Mở shop & KYC D4, K01.06–K01.07). */
class PresignedUploadIT extends IntegrationTestBase {

    @Autowired
    JwtService jwtService;

    private ResultActions presign(String token, String purpose, String contentType, long size) throws Exception {
        return mvc.perform(post("/api/v1/uploads/presigned-urls").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"" + purpose + "\",\"file_name\":\"f\",\"content_type\":\"" + contentType
                        + "\",\"content_length\":" + size + "}"));
    }

    private String vendorToken() {
        long vendor = new TestDataBuilder(jdbc).vendor();
        return "Bearer " + jwtService.issue(vendor, UserRole.VENDOR, 1L).token();
    }

    @Test
    void vendorGetsTenMinuteKycLinkInPrivateBucket() throws Exception {
        String body = presign(vendorToken(), "KYC_DOCUMENT", "image/jpeg", 2_000_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.object_key").value(org.hamcrest.Matchers.startsWith("tmp/user/")))
                .andExpect(jsonPath("$.data.upload_url").value(org.hamcrest.Matchers.containsString("/kyc-dossiers/")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Instant expires = java.time.OffsetDateTime.parse(
                com.jayway.jsonpath.JsonPath.read(body, "$.data.expires_at").toString()).toInstant();
        assertThat(Duration.between(clock.instant(), expires)).isEqualTo(Duration.ofMinutes(10));
        presign(vendorToken(), "KYC_DOCUMENT", "application/pdf", 10L * 1024 * 1024).andExpect(status().isOk());
        presign(vendorToken(), "SHOP_LOGO", "image/png", 1024 * 1024).andExpect(status().isOk());
        presign(vendorToken(), "SHOP_COVER", "image/jpeg", 2L * 1024 * 1024).andExpect(status().isOk());
    }

    @Test
    void customerWithoutShopIsForbidden() throws Exception {
        long customer = createCustomer(uniqueEmail(), PASSWORD).getId();
        String token = "Bearer " + jwtService.issue(customer, UserRole.CUSTOMER, 1L).token();
        for (String purpose : new String[] {"KYC_DOCUMENT", "SHOP_LOGO", "SHOP_COVER"}) {
            presign(token, purpose, "image/jpeg", 100_000)
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GNE-SEC-4030"));
        }
    }

    @Test
    void rejectsTinyImagesOversizedFilesAndWrongTypes() throws Exception {
        String token = vendorToken();
        presign(token, "KYC_DOCUMENT", "image/png", 5 * 1024)                 // ảnh 5 KB — dưới 20 KB
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"))
                .andExpect(jsonPath("$.details.min_bytes").value(20 * 1024));
        presign(token, "KYC_DOCUMENT", "application/pdf", 5 * 1024).andExpect(status().isOk());   // PDF nhỏ vẫn nhận
        presign(token, "KYC_DOCUMENT", "application/pdf", 11L * 1024 * 1024)
                .andExpect(status().isUnprocessableEntity());
        presign(token, "KYC_DOCUMENT", "image/webp", 100_000).andExpect(status().isUnprocessableEntity());
        presign(token, "SHOP_COVER", "image/jpeg", 2L * 1024 * 1024 + 1).andExpect(status().isUnprocessableEntity());
        presign(token, "SHOP_LOGO", "image/png", 1024 * 1024 + 1).andExpect(status().isUnprocessableEntity());
    }
}
