package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.domain.UserRole;
import com.genie.common.mail.MailTemplate;
import com.genie.vendor.kyc.application.KycSupplementExpiryJob;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Nhân viên duyệt hồ sơ KYC — UC-VND-04 (API_SPEC 11.3; plan Mở shop & KYC K06.12). */
class KycReviewIT extends KycTestSupport {

    private static final String URL = "/api/v1/admin/kyc-submissions";

    @Autowired
    KycSupplementExpiryJob expiryJob;

    private String staff(String email, UserRole role) {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        return "Bearer " + jwtService.issue(id, role, 1L).token();
    }

    private String moderator() {
        return staff("phat.nt@genie.example", UserRole.STAFF);
    }

    private String admin() {
        return staff("yen.dh@genie.example", UserRole.ADMIN);
    }

    /** Hồ sơ thật (3 ảnh đã mã hóa) đang chờ nhân viên duyệt. */
    private long[] manualReview() throws Exception {
        aiWorker.failNext(true);
        long[] s = submitted(KycDecisionIT.identity(), true);
        await(() -> "MANUAL_REVIEW".equals(kycStatus(s[1])));
        aiWorker.failNext(false);
        return s;
    }

    private ResultActions action(String token, long id, String path, String body) throws Exception {
        var req = post(URL + "/" + id + "/" + path).header(HttpHeaders.AUTHORIZATION, token);
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(req);
    }

    @Test
    void queueOrdersByRiskThenSubmissionAndCountsByStatus() throws Exception {
        long[] low = manualReview();
        long[] high = manualReview();
        jdbc.update("UPDATE kyc_results SET risk_score = 12 WHERE vendor_profile_id = ?", low[1]);
        jdbc.update("UPDATE kyc_results SET risk_score = 65 WHERE vendor_profile_id = ?", high[1]);
        String body = mvc.perform(get(URL).param("status", "PENDING").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, moderator()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.summary.counts.PENDING").isNumber())
                .andExpect(jsonPath("$.summary.counts.ALL").isNumber())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Integer> ids = JsonPath.read(body, "$.data[*].id");
        assertThat(ids).contains((int) low[1], (int) high[1]);
        assertThat(ids.indexOf((int) high[1])).isLessThan(ids.indexOf((int) low[1]));
        List<String> statuses = JsonPath.read(body, "$.data[*].status");
        assertThat(statuses).allMatch(st -> List.of("PENDING", "AI_PROCESSING", "MANUAL_REVIEW").contains(st));
        assertThat(JsonPath.<String>read(body, "$.data[0].code")).startsWith("KYC-");
    }

    @Test
    void detailShowsMaskedIdAndFiveMinuteImagesAndIsLogged() throws Exception {
        long[] s = manualReview();
        String body = mvc.perform(get(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, moderator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("KYC-" + s[1]))
                .andExpect(jsonPath("$.data.status").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.data.declared.identity_number_masked").value(org.hamcrest.Matchers.containsString("•")))
                .andExpect(jsonPath("$.data.documents.length()").value(3))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String url = JsonPath.read(body, "$.data.documents[0].url");
        assertThat(url).contains("X-Amz-Expires=300");
        HttpResponse<byte[]> img = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(img.statusCode()).isEqualTo(200);
        assertThat(img.body()).hasSize(30_000);   // bản giải mã đúng kích thước ảnh gốc
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'KYC_DOCUMENT_VIEWED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(s[1]))).isEqualTo(1);

        // tín hiệu rủi ro đã cộng điểm hiện cho nhân viên kèm nhãn (UC-VND-03 bước 5)
        jdbc.update("UPDATE kyc_results SET risk_signals_json = ? WHERE vendor_profile_id = ?",
                "[{\"code\":\"DUPLICATE_ID\",\"points\":50},{\"code\":\"UNREADABLE_IMAGE\",\"points\":30}]", s[1]);
        mvc.perform(get(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, moderator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risk_signals.length()").value(2))
                .andExpect(jsonPath("$.data.risk_signals[0].code").value("DUPLICATE_ID"))
                .andExpect(jsonPath("$.data.risk_signals[0].points").value(50))
                .andExpect(jsonPath("$.data.risk_signals[0].label").value("số CCCD đã được dùng cho một shop khác"));
        jdbc.update("UPDATE kyc_results SET risk_signals_json = 'không phải JSON' WHERE vendor_profile_id = ?", s[1]);
        mvc.perform(get(URL + "/" + s[1]).header(HttpHeaders.AUTHORIZATION, moderator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.risk_signals.length()").value(0));
    }

    @Test
    void approveRejectAndPermissions() throws Exception {
        long[] a = manualReview();
        // Moderator không có UPDATE_COMMISSION → không đặt được hoa hồng riêng
        action(moderator(), a[1], "approve", "{\"shop_commission_rate\":3.0}").andExpect(status().isForbidden());
        assertThat(kycStatus(a[1])).isEqualTo("MANUAL_REVIEW");
        action(moderator(), a[1], "approve", "{\"is_mall\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.is_mall").value(true));
        var row = jdbc.queryForMap("SELECT kyc_verified_by, kyc_verification_method FROM vendor_profiles WHERE id = ?", a[1]);
        assertThat(row.get("kyc_verification_method")).isEqualTo("MANUAL_STAFF");
        assertThat(row.get("kyc_verified_by")).isNotNull();
        await(() -> mails.last(jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, a[0]),
                MailTemplate.KYC_APPROVED).isPresent());
        // hồ sơ đã duyệt → thao tác duyệt lại 409
        action(moderator(), a[1], "approve", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-VND-4095"));

        long[] c = manualReview();   // Admin đặt hoa hồng 3%
        action(admin(), c[1], "approve", "{\"shop_commission_rate\":3.0}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shop_commission_rate").value(3));
        assertThat(jdbc.queryForObject("SELECT commission_rate FROM vendor_profiles WHERE id = ?", java.math.BigDecimal.class,
                c[1])).isEqualByComparingTo("0.03");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'COMMISSION_UPDATED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(c[1]))).isEqualTo(1);

        long[] r = manualReview();
        action(moderator(), r[1], "reject", "{\"reason_code\":\"OTHER\"}").andExpect(status().isUnprocessableEntity());
        action(moderator(), r[1], "reject", "{\"reason_code\":\"BLURRY_DOCUMENT\",\"note\":\"Mặt sau bị lóa\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejection_reason").value("Ảnh giấy tờ mờ, không đọc được: Mặt sau bị lóa"));

        // Finance Officer / CS không có REVIEW_KYC
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, staff("linh.tm@genie.example", UserRole.STAFF)))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, staff("truc.ht@genie.example", UserRole.STAFF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supplementExpiresAfterSevenDaysAndRequeue() throws Exception {
        long[] s = manualReview();
        action(moderator(), s[1], "request-supplement", "{\"documents\":[],\"note\":\"x\"}")
                .andExpect(status().isUnprocessableEntity());
        action(moderator(), s[1], "request-supplement", "{\"documents\":[\"ID_FRONT\"],\"note\":\"Ảnh mặt trước bị mờ\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUPPLEMENT_REQUESTED"))
                .andExpect(jsonPath("$.data.supplement.documents[0]").value("ID_FRONT"));
        mvc.perform(get("/api/v1/seller/kyc").header(HttpHeaders.AUTHORIZATION, vendorToken(s[0])))
                .andExpect(jsonPath("$.data.supplement.note").value("Ảnh mặt trước bị mờ"))
                .andExpect(jsonPath("$.data.supplement.documents[0]").value("ID_FRONT"))
                .andExpect(jsonPath("$.data.supplement.deadline_at").exists());
        String sellerEmail = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, s[0]);
        await(() -> mails.last(sellerEmail, MailTemplate.KYC_SUPPLEMENT).isPresent());

        clock.advance(Duration.ofDays(6));
        expiryJob.run();
        assertThat(kycStatus(s[1])).isEqualTo("SUPPLEMENT_REQUESTED");
        clock.advance(Duration.ofDays(2));
        expiryJob.run();
        assertThat(kycStatus(s[1])).isEqualTo("REJECTED");
        assertThat(jdbc.queryForObject("SELECT kyc_rejection_reason FROM vendor_profiles WHERE id = ?", String.class, s[1]))
                .isEqualTo("Quá hạn bổ sung hồ sơ");

        action(moderator(), s[1], "requeue", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MANUAL_REVIEW"));
        action(moderator(), s[1], "requeue", null).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'KYC_REQUEUED' "
                + "AND reference_id = ?", Integer.class, String.valueOf(s[1]))).isEqualTo(1);
    }
}
