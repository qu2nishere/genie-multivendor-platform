package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ADM-07 — xuất nhật ký CSV (plan nhật ký kiểm toán L04.04). Giới hạn dòng hạ xuống 3 để thử cắt + header X-Truncated.
 */
@TestPropertySource(properties = "genie.audit.export-max=3")
class AuditLogExportIT extends AuditLogTestSupport {

    private static final String URL = "/api/v1/admin/audit-logs/export";
    private static final Instant NOW = Instant.parse(START);

    @Test
    void exportsFilteredRowsAsCsvWithBomAndEscaping() throws Exception {
        String token = adminToken();
        clearLogs();
        insertLog(NOW.minusSeconds(60), 1L, "a@genie.test", "CUSTOMER", "LOGIN_FAILED", null,
                "{\"target_label\":\"=HYPERLINK(\\\"x\\\"), \\\"y\\\"\"}", "1.1.1.1", "FAILED");
        insertLog(NOW.minusSeconds(120), 2L, "b@genie.test", "VENDOR", "KYC_SUBMITTED", "KYC-1", null, null, "SUCCESS");

        MvcResult r = mvc.perform(get(URL).param("category", "AUTH").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"nhat-ky-he-thong.csv\""))
                .andExpect(header().doesNotExist("X-Truncated"))
                .andReturn();
        String csv = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿Thời gian,");
        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(2);   // tiêu đề + 1 dòng nhóm AUTH
        assertThat(lines[1]).isEqualTo("2026-06-08 13:59:00,a@genie.test,CUSTOMER,LOGIN_FAILED,AUTH,"
                + "\"'=HYPERLINK(\"\"x\"\"), \"\"y\"\"\",1.1.1.1,FAILED");
    }

    @Test
    void truncatesAtLimitWithHeader() throws Exception {
        String token = adminToken();
        clearLogs();
        for (int i = 1; i <= 5; i++) {
            insertLog(NOW.minusSeconds(i * 60L), 1L, "u" + i + "@genie.test", "CUSTOMER", "LOGIN_SUCCESS", null, null,
                    null, "SUCCESS");
        }
        MvcResult r = mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Truncated", "true"))
                .andReturn();
        assertThat(r.getResponse().getContentAsString(StandardCharsets.UTF_8).split("\r\n")).hasSize(4);
    }

    @Test
    void requiresPermissionAndIsRateLimited() throws Exception {
        Long staff = createStaff("CS_SPECIALIST", "VIEW_DASHBOARD");
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, staffToken(staff)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GNE-SEC-4030"));

        String token = adminToken();
        for (int i = 0; i < 5; i++) {
            mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        }
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("GNE-RAT-4290"));
    }

    @Test
    void invalidRangeIsRejectedBeforeStreaming() throws Exception {
        mvc.perform(get(URL).param("from", "2026-01-01").param("to", "2026-06-01")
                        .header(HttpHeaders.AUTHORIZATION, adminToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"));
    }
}
