package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.mail.MailTemplate;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Áp kết quả thẩm định AI — UC-VND-03 (plan Mở shop & KYC K05.12; ENTITY 1.6). */
class KycDecisionIT extends KycTestSupport {

    static String identity() {
        return String.format("0791%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }

    private ResultActions callback(long id, String body) throws Exception {
        return mvc.perform(patch("/api/v1/internal/kyc/" + id + "/result").header("X-Internal-Token", "test-internal-token")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String email(long vendorUserId) {
        return jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, vendorUserId);
    }

    @Test
    void noSignalsAutoApprovesAndEmailsTheSeller() throws Exception {
        String id = identity();
        long[] s = submitted(id, true);
        // AI Worker nhận đủ 3 URL bản xem tạm + dữ liệu kê khai
        await(() -> aiWorker.requests().stream().anyMatch(r -> r.kycId() == s[1]));
        var req = aiWorker.requests().stream().filter(r -> r.kycId() == s[1]).findFirst().orElseThrow();
        assertThat(req.documents()).containsOnlyKeys("id_front", "id_back", "selfie");
        assertThat(req.documents().get("selfie")).contains("/kyc-dossiers/tmp/view/").contains("X-Amz-Expires=300");
        assertThat(req.identityNumber()).isEqualTo(id);
        assertThat(req.callbackUrl()).endsWith("/api/v1/internal/kyc/" + s[1] + "/result");

        callback(s[1], callbackBody(id, "DANG QUOC HUY")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kyc_status").value("VERIFIED"));
        var row = jdbc.queryForMap("SELECT kyc_status, kyc_verification_method FROM vendor_profiles WHERE id = ?", s[1]);
        assertThat(row.get("kyc_verification_method")).isEqualTo("OCR_AUTO");
        var result = jdbc.queryForMap("SELECT * FROM kyc_results WHERE vendor_profile_id = ?", s[1]);
        assertThat(result.get("overall_status")).isEqualTo("PASS");
        assertThat(result.get("risk_score")).isEqualTo(0);
        assertThat(result.get("extracted_id_number")).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'KYC_APPROVED' AND reference_id = ?",
                Integer.class, String.valueOf(s[1]))).isEqualTo(1);
        await(() -> mails.last(email(s[0]), MailTemplate.KYC_APPROVED).isPresent());
    }

    @Test
    void sampledMediumAndHighRisk() throws Exception {
        kycSampler.set(true);
        String id1 = identity();
        long[] sampled = submitted(id1, true);
        callback(sampled[1], callbackBody(id1, "Đặng Quốc Huy")).andExpect(jsonPath("$.data.kyc_status").value("MANUAL_REVIEW"));
        assertThat(jdbc.queryForObject("SELECT sampled_for_review FROM kyc_results WHERE vendor_profile_id = ?",
                Boolean.class, sampled[1])).isTrue();
        kycSampler.set(false);

        String id2 = identity();
        long[] medium = submitted(id2, true);   // 25 + 20 (họ tên trên QR khác kê khai) = 45
        callback(medium[1], callbackBody(id2, "NGUYEN VAN KHAC", "FACE_SCORE_BORDERLINE"))
                .andExpect(jsonPath("$.data.kyc_status").value("MANUAL_REVIEW"));
        var r2 = jdbc.queryForMap("SELECT overall_status, risk_score, risk_signals_json FROM kyc_results WHERE vendor_profile_id = ?",
                medium[1]);
        assertThat(r2.get("overall_status")).isEqualTo("PARTIAL");
        assertThat(r2.get("risk_score")).isEqualTo(45);
        assertThat(String.valueOf(r2.get("risk_signals_json"))).contains("DECLARED_DATA_MISMATCH");

        String id3 = identity();
        long[] high = submitted(id3, true);     // 60 + 60 = 120
        callback(high[1], callbackBody(id3, "DANG QUOC HUY", "QR_MRZ_MISMATCH", "FACE_SCORE_LOW"))
                .andExpect(jsonPath("$.data.kyc_status").value("REJECTED"));
        String reason = jdbc.queryForObject("SELECT kyc_rejection_reason FROM vendor_profiles WHERE id = ?", String.class, high[1]);
        assertThat(reason).contains("mã QR và dòng MRZ").contains("Yêu cầu nhân viên xem xét");
        await(() -> mails.last(email(high[0]), MailTemplate.KYC_REJECTED).isPresent());
    }

    @Test
    void duplicateIdNoConsentAndPreviouslyRequestedReview() throws Exception {
        String dup = identity();
        long[] first = submitted(dup, true);
        callback(first[1], callbackBody(dup, "DANG QUOC HUY")).andExpect(jsonPath("$.data.kyc_status").value("VERIFIED"));
        long[] second = submitted(dup, true);  // cùng CCCD với shop đã duyệt → +50 → vùng nhân viên duyệt (30–69)
        callback(second[1], callbackBody(dup, "DANG QUOC HUY")).andExpect(jsonPath("$.data.kyc_status").value("MANUAL_REVIEW"));
        var d = jdbc.queryForMap("SELECT duplicate_id_found, risk_score FROM kyc_results WHERE vendor_profile_id = ?", second[1]);
        assertThat(d.get("duplicate_id_found")).isEqualTo(true);
        assertThat(d.get("risk_score")).isEqualTo(50);

        String id = identity();
        long[] noConsent = submitted(id, false); // không đồng ý sinh trắc → luôn duyệt tay, bỏ tín hiệu khuôn mặt
        callback(noConsent[1], callbackBody(id, "DANG QUOC HUY", "FACE_SCORE_LOW"))
                .andExpect(jsonPath("$.data.kyc_status").value("MANUAL_REVIEW"));
        var r = jdbc.queryForMap("SELECT risk_score, face_match_score FROM kyc_results WHERE vendor_profile_id = ?", noConsent[1]);
        assertThat(r.get("risk_score")).isEqualTo(0);
        assertThat(r.get("face_match_score")).isNull();

        // hồ sơ đã từng yêu cầu nhân viên xem xét: AI kết luận FAIL → MANUAL_REVIEW chứ không REJECTED
        String id4 = identity();
        long[] asked = submitted(id4, true);
        jdbc.update("UPDATE vendor_profiles SET kyc_manual_review_requested_at = now() WHERE id = ?", asked[1]);
        callback(asked[1], callbackBody(id4, "DANG QUOC HUY", "QR_MRZ_MISMATCH", "FACE_SCORE_LOW"))
                .andExpect(jsonPath("$.data.kyc_status").value("MANUAL_REVIEW"));
    }
}
