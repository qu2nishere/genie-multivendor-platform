package com.genie.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

/** UC-ADM-07 — tra cứu nhật ký: lọc, tìm, phân trang cursor, denied_count (plan nhật ký kiểm toán L03.07). */
class AuditLogQueryIT extends AuditLogTestSupport {

    private static final Instant NOW = Instant.parse(START);   // 2026-06-08T07:00:00Z = 14:00 giờ Việt Nam

    @Test
    void filtersByCategoryAndMapsColumns() throws Exception {
        String token = adminToken();
        clearLogs();
        Long staff = createStaff("MODERATOR", "REVIEW_KYC");
        insertLog(NOW.minusSeconds(60), staff, "phat@genie.example", "STAFF", "KYC_APPROVED", "1037",
                "{\"target_label\":\"KYC-1037 · TravelGear\"}", "113.161.40.12", "SUCCESS");
        insertLog(NOW.minusSeconds(120), null, "system@genie.vn", "SYSTEM", "ESCROW_RELEASED", "TR-1", null, null, "SUCCESS");
        insertLog(NOW.minusSeconds(180), 5L, "a@genie.test", "CUSTOMER", "ACCESS_DENIED", null, null, "10.0.0.1", "FAILED");

        mvc.perform(get("/api/v1/admin/audit-logs").param("category", "KYC").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].actor_role").value("MODERATOR"))
                .andExpect(jsonPath("$.data[0].category").value("KYC"))
                .andExpect(jsonPath("$.data[0].target").value("KYC-1037 · TravelGear"))
                .andExpect(jsonPath("$.data[0].result").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].created_at").value("2026-06-08T13:59:00+07:00"))
                .andExpect(jsonPath("$.summary.denied_count").value(0));

        mvc.perform(get("/api/v1/admin/audit-logs").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[1].actor_email").value("system"))
                .andExpect(jsonPath("$.data[1].target").value("TR-1"))
                .andExpect(jsonPath("$.data[2].result").value("DENIED"))
                .andExpect(jsonPath("$.data[2].category").value("SECURITY"))
                .andExpect(jsonPath("$.summary.denied_count").value(1))
                .andExpect(jsonPath("$.pagination.has_more").value(false))
                .andExpect(jsonPath("$.pagination.limit").value(50));
    }

    @Test
    void timeWindowIncludesFromAndExcludesTo() throws Exception {
        String token = adminToken();
        clearLogs();
        Instant from = Instant.parse("2026-06-07T00:00:00Z");
        Instant to = Instant.parse("2026-06-08T00:00:00Z");
        insertLog(from, 1L, "x@a.b", "CUSTOMER", "LOGIN_SUCCESS", null, null, null, "SUCCESS");
        insertLog(to, 1L, "y@a.b", "CUSTOMER", "LOGIN_SUCCESS", null, null, null, "SUCCESS");
        insertLog(from.minusSeconds(1), 1L, "z@a.b", "CUSTOMER", "LOGIN_SUCCESS", null, null, null, "SUCCESS");

        mvc.perform(get("/api/v1/admin/audit-logs").param("from", from.toString()).param("to", to.toString())
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].actor_email").value("x@a.b"));

        // dạng ngày: giờ Việt Nam, "to" tính hết ngày → 2026-06-07 07:00 VN (00:00Z) và 2026-06-08 07:00 VN đều thuộc ngày 07–08
        mvc.perform(get("/api/v1/admin/audit-logs").param("from", "2026-06-07").param("to", "2026-06-08")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void searchesEmailIpAndReferenceCaseInsensitively() throws Exception {
        String token = adminToken();
        clearLogs();
        insertLog(NOW.minusSeconds(10), 1L, "Mai.Tran@Gmail.com", "CUSTOMER", "LOGIN_SUCCESS", null, null, "1.2.3.4", "SUCCESS");
        insertLog(NOW.minusSeconds(20), 2L, "b@genie.test", "CUSTOMER", "LOGIN_FAILED", null, null, "113.161.40.12", "FAILED");
        insertLog(NOW.minusSeconds(30), 3L, "c@genie.test", "VENDOR", "KYC_SUBMITTED", "KYC-1042", null, null, "SUCCESS");
        insertLog(NOW.minusSeconds(40), 4L, "50%off@genie.test", "CUSTOMER", "LOGIN_SUCCESS", null, null, null, "SUCCESS");

        assertCount(token, "mai.tran", 1);
        assertCount(token, "113.161", 1);
        assertCount(token, "kyc-1042", 1);
        assertCount(token, "50%", 1);       // ký tự đại diện LIKE được thoát
        assertCount(token, "%", 1);
        assertCount(token, "khong-co", 0);
    }

    @Test
    void cursorPagingHasNoDuplicatesOrGapsWithEqualTimestamps() throws Exception {
        String token = adminToken();
        clearLogs();
        Set<String> inserted = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            // 7 dòng cùng một thời điểm + 3 dòng khác thời điểm
            inserted.add(insertLog(NOW.minus(5, ChronoUnit.MINUTES), 1L, "same@a.b", "CUSTOMER", "LOGIN_SUCCESS",
                    null, null, null, "SUCCESS"));
        }
        for (int i = 1; i <= 3; i++) {
            inserted.add(insertLog(NOW.minus(i, ChronoUnit.HOURS), 1L, "h@a.b", "CUSTOMER", "LOGIN_SUCCESS",
                    null, null, null, "SUCCESS"));
        }
        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            var req = get("/api/v1/admin/audit-logs").param("limit", "4").header(HttpHeaders.AUTHORIZATION, token);
            if (cursor != null) {
                req = req.param("cursor", cursor);
            }
            MvcResult r = mvc.perform(req).andExpect(status().isOk()).andReturn();
            String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
            List<String> ids = JsonPath.read(body, "$.data[*].id");
            seen.addAll(ids);
            cursor = JsonPath.read(body, "$.pagination.next_cursor");
            pages++;
        } while (cursor != null);
        assertThat(pages).isEqualTo(3);
        assertThat(seen).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(inserted);
    }

    @Test
    void emptyResultIsEmptyArray() throws Exception {
        String token = adminToken();
        clearLogs();
        mvc.perform(get("/api/v1/admin/audit-logs").param("category", "FINANCE").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.pagination.next_cursor").isEmpty());
    }

    @Test
    void rejectsInvalidParameters() throws Exception {
        String token = adminToken();
        mvc.perform(get("/api/v1/admin/audit-logs").param("from", "2026-01-01").param("to", "2026-06-01")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VAL-4220"))
                .andExpect(jsonPath("$.errors[0].field").value("from"))
                .andExpect(jsonPath("$.errors[0].code").value("RANGE_TOO_LONG"));
        mvc.perform(get("/api/v1/admin/audit-logs").param("category", "BOGUS").param("limit", "500")
                        .param("cursor", "%%%").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(3));
        mvc.perform(get("/api/v1/admin/audit-logs").param("from", "2026-06-08").param("to", "2026-06-01")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_RANGE"));
    }

    private void assertCount(String token, String q, int expected) throws Exception {
        mvc.perform(get("/api/v1/admin/audit-logs").param("q", q).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(expected));
    }
}
