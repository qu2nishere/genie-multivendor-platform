package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.vendor.kyc.application.KycAiTimeoutJob;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Callback nội bộ AI Worker + chống kẹt hồ sơ (API_SPEC 12; plan Mở shop & KYC K05.13). */
class KycCallbackIT extends KycTestSupport {

    @Autowired
    KycAiTimeoutJob timeoutJob;

    private org.springframework.test.web.servlet.ResultActions callback(long id, String token, String body) throws Exception {
        var req = patch("/api/v1/internal/kyc/" + id + "/result").contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) {
            req = req.header("X-Internal-Token", token);
        }
        return mvc.perform(req);
    }

    @Test
    void requiresInternalTokenAndIsIdempotent() throws Exception {
        String id = KycDecisionIT.identity();
        long[] s = submitted(id, true);
        callback(s[1], null, callbackBody(id, "DANG QUOC HUY")).andExpect(status().isUnauthorized());
        callback(s[1], "sai-token", callbackBody(id, "DANG QUOC HUY")).andExpect(status().isUnauthorized());
        assertThat(kycStatus(s[1])).isEqualTo("AI_PROCESSING");

        callback(s[1], "test-internal-token", callbackBody(id, "DANG QUOC HUY"))
                .andExpect(jsonPath("$.data.kyc_status").value("VERIFIED"));
        // gọi lại với kết quả xấu: đã chốt thì không đổi gì
        callback(s[1], "test-internal-token", callbackBody(id, "DANG QUOC HUY", "QR_MRZ_MISMATCH", "FACE_SCORE_LOW"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.kyc_status").value("VERIFIED"));
        assertThat(jdbc.queryForObject("SELECT risk_score FROM kyc_results WHERE vendor_profile_id = ?", Integer.class, s[1]))
                .isZero();
        // hồ sơ chưa nộp: 200, không đổi trạng thái
        long[] fresh = newShop();
        callback(fresh[1], "test-internal-token", callbackBody(id, "X"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.kyc_status").value("NOT_SUBMITTED"));
        callback(fresh[1], "test-internal-token", "{\"face_match_score\":3}").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void stuckDossiersGoToManualReviewAfterThirtySeconds() throws Exception {
        long[] s = submitted(KycDecisionIT.identity(), true);
        clock.advance(Duration.ofSeconds(20));
        timeoutJob.run();
        assertThat(kycStatus(s[1])).isEqualTo("AI_PROCESSING");
        clock.advance(Duration.ofSeconds(15));
        assertThat(timeoutJob.run()).isGreaterThanOrEqualTo(1);
        assertThat(kycStatus(s[1])).isEqualTo("MANUAL_REVIEW");
        assertThat(jdbc.queryForObject("SELECT errors FROM kyc_results WHERE vendor_profile_id = ?", String.class, s[1]))
                .contains("AI_TIMEOUT");
    }

    @Test
    void workerDownRoutesToManualReviewImmediately() throws Exception {
        aiWorker.failNext(true);
        long[] s = submitted(KycDecisionIT.identity(), true);
        await(() -> "MANUAL_REVIEW".equals(kycStatus(s[1])));
        assertThat(jdbc.queryForObject("SELECT errors FROM kyc_results WHERE vendor_profile_id = ?", String.class, s[1]))
                .contains("AI_WORKER_UNAVAILABLE");
    }
}
