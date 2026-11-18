package com.genie.vendor.kyc.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Chấm điểm rủi ro KYC theo cấu hình (plan Mở shop & KYC K05.03; ENTITY 1.6 d). */
class KycRiskPolicyTest {

    private static final Map<String, Integer> POINTS = Map.ofEntries(
            Map.entry("QR_MRZ_MISMATCH", 60), Map.entry("ID_STRUCTURE_MISMATCH", 40), Map.entry("EXPIRY_RULE_MISMATCH", 30),
            Map.entry("DUPLICATE_ID", 50), Map.entry("FACE_DUPLICATE", 70), Map.entry("FACE_SCORE_BORDERLINE", 25),
            Map.entry("FACE_SCORE_LOW", 60), Map.entry("LIVENESS_SUSPECT", 40), Map.entry("LEGACY_CARD", 30),
            Map.entry("DECLARED_DATA_MISMATCH", 20), Map.entry("UNREADABLE_IMAGE", 30));

    private static KycRiskPolicy policy(Map<String, Integer> points, double sampleRate, Random random) {
        return new KycRiskPolicy(new KycProperties.Risk(30, 70, sampleRate, points), random::nextDouble);
    }

    @Test
    void sumsSignalsOnceAndIgnoresUnknownCodes() {
        KycRiskPolicy p = policy(POINTS, 0, new Random(1));
        assertThat(p.assess(List.of()).score()).isZero();
        var a = p.assess(List.of("FACE_SCORE_BORDERLINE", "DECLARED_DATA_MISMATCH", "FACE_SCORE_BORDERLINE", "UNKNOWN_X"));
        assertThat(a.score()).isEqualTo(45);
        assertThat(a.signals()).extracting(KycRiskPolicy.ScoredSignal::code)
                .containsExactly("FACE_SCORE_BORDERLINE", "DECLARED_DATA_MISMATCH");
        assertThat(p.assess(List.of("QR_MRZ_MISMATCH", "FACE_SCORE_LOW")).score()).isEqualTo(120);
    }

    @Test
    void thresholdsBoundaries() {
        KycRiskPolicy p = policy(POINTS, 0, new Random(1));
        assertThat(p.assess(List.of("FACE_SCORE_BORDERLINE")).score()).isLessThan(p.autoApproveBelow());   // 25 < 30
        int unreadable = p.assess(List.of("UNREADABLE_IMAGE")).score();                                      // 30
        assertThat(unreadable).isGreaterThanOrEqualTo(p.autoApproveBelow()).isLessThan(p.rejectFrom());
        assertThat(p.assess(List.of("FACE_DUPLICATE")).score()).isGreaterThanOrEqualTo(p.rejectFrom());
    }

    @Test
    void changingConfigurationChangesTheScore() {
        Map<String, Integer> custom = Map.of("FACE_SCORE_BORDERLINE", 35);
        assertThat(policy(custom, 0, new Random(1)).assess(List.of("FACE_SCORE_BORDERLINE")).score()).isEqualTo(35);
    }

    @Test
    void samplesAboutFivePercentOfLowRiskDossiers() {
        KycRiskPolicy p = policy(POINTS, 0.05, new Random(42));
        int sampled = 0;
        for (int i = 0; i < 10_000; i++) {
            sampled += p.assess(List.of()).sampled() ? 1 : 0;
        }
        assertThat(sampled).isBetween(420, 580);
        // hồ sơ điểm cao không "bị lấy mẫu" (đã đi duyệt tay / từ chối)
        KycRiskPolicy always = policy(POINTS, 1.0, new Random(1));
        assertThat(always.assess(List.of("UNREADABLE_IMAGE")).sampled()).isFalse();
        assertThat(always.assess(List.of()).sampled()).isTrue();
    }
}
