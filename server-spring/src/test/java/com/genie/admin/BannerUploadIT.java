package com.genie.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Xin link tải ảnh banner — purpose BANNER_IMAGE chỉ cho quyền MANAGE_VOUCHER (plan banner B01.05–B01.06; API 4.1). */
class BannerUploadIT extends AuditLogTestSupport {

    private ResultActions presign(String token, String contentType, long size) throws Exception {
        return mvc.perform(post("/api/v1/uploads/presigned-urls").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"BANNER_IMAGE\",\"file_name\":\"b.webp\",\"content_type\":\"" + contentType
                        + "\",\"content_length\":" + size + "}"));
    }

    @Test
    void financeOfficerAndAdminCanRequestBannerUpload() throws Exception {
        presign(token("linh.tm@genie.example", UserRole.STAFF), "image/webp", 200_000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.method").value("PUT"));
        presign(adminToken(), "image/jpeg", 5L * 1024 * 1024).andExpect(status().isOk());
    }

    @Test
    void othersAreForbidden() throws Exception {
        presign(token("phat.nt@genie.example", UserRole.STAFF), "image/png", 1000)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GNE-SEC-4030"));
        presign(token("mai.tran@gmail.com", UserRole.CUSTOMER), "image/png", 1000)
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWrongTypeAndTooLarge() throws Exception {
        String token = adminToken();
        presign(token, "image/gif", 1000).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"));
        presign(token, "image/png", 5L * 1024 * 1024 + 1).andExpect(status().isUnprocessableEntity());
    }
}
