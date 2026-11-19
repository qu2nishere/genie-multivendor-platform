package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Hợp đồng Backend ↔ AI Worker dùng chung (ADR 0011 mục 5; {@code docs/api-tests/kyc-contract/*.json} — plan AI Worker Y00.06):
 * backend nhận nguyên văn 3 tệp kết quả mẫu và ra đúng trạng thái README của thư mục đó mô tả.
 */
class KycContractIT extends KycTestSupport {

    private static final Path DIR = Path.of("..", "docs", "api-tests", "kyc-contract");

    private String apply(String file, String expected) throws Exception {
        long[] s = submitted(KycDecisionIT.identity(), true);
        // dữ liệu kê khai khớp phần "extracted" của tệp mẫu → không sinh DECLARED_DATA_MISMATCH
        jdbc.update("UPDATE vendor_profiles SET identity_code = '079190000012', representative = 'Phạm Thị Lan' WHERE id = ?", s[1]);
        String body = Files.readString(DIR.resolve(file), StandardCharsets.UTF_8);
        mvc.perform(patch("/api/v1/internal/kyc/" + s[1] + "/result").header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kyc_status").value(expected));
        Integer score = jdbc.queryForObject("SELECT risk_score FROM kyc_results WHERE vendor_profile_id = ?", Integer.class, s[1]);
        jdbc.update("UPDATE vendor_profiles SET identity_code = NULL WHERE id = ?", s[1]);   // tránh DUPLICATE_ID cho ca sau
        return String.valueOf(score);
    }

    @Test
    void sharedSamplesProduceTheDocumentedOutcome() throws Exception {
        assertThat(apply("result-pass.json", "VERIFIED")).isEqualTo("0");
        assertThat(apply("result-partial.json", "MANUAL_REVIEW")).isEqualTo("55");
        assertThat(apply("result-fail.json", "REJECTED")).isEqualTo("120");
    }
}
