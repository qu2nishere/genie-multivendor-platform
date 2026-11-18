package com.genie.vendor.kyc.infra;

import com.genie.common.domain.NameNormalizer;
import com.genie.vendor.kyc.application.KycAiResult;
import com.genie.vendor.kyc.application.KycDecisionService;
import com.genie.vendor.kyc.application.KycVerificationClient;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Giả lập AI Worker khi chưa chạy bản thật ({@code genie.ai-worker.mode = stub} — mặc định profile local; ADR 0011 mục 6;
 * plan Mở shop & KYC D7, K05.05). Sinh tín hiệu theo 2 số cuối CCCD kê khai rồi áp kết quả sau 2 giây:
 * <ul>
 *   <li>{@code 00–29}: không tín hiệu → tự duyệt (trừ khi bị lấy mẫu 5%);</li>
 *   <li>{@code 30–69}: {@code FACE_SCORE_BORDERLINE + UNREADABLE_IMAGE} (55 điểm) → nhân viên duyệt;</li>
 *   <li>{@code 70–99}: {@code QR_MRZ_MISMATCH + FACE_SCORE_LOW} (120 điểm) → từ chối.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "genie.ai-worker", name = "mode", havingValue = "stub")
public class StubKycVerificationClient implements KycVerificationClient {

    private static final Logger log = LoggerFactory.getLogger(StubKycVerificationClient.class);
    static final long DELAY_MS = 2_000;

    private final KycDecisionService decisions;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "kyc-ai-stub");
        t.setDaemon(true);
        return t;
    });

    public StubKycVerificationClient(@Lazy KycDecisionService decisions) {
        this.decisions = decisions;
    }

    @Override
    public void requestVerification(Request r) {
        KycAiResult result = simulate(r);
        executor.schedule(() -> {
            try {
                decisions.apply(r.kycId(), result);
            } catch (RuntimeException e) {
                log.warn("Giả lập AI KYC lỗi cho hồ sơ {}: {}", r.kycId(), e.getMessage());
            }
        }, DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /** Kết quả giả theo 2 số cuối CCCD kê khai. */
    public static KycAiResult simulate(Request r) {
        String id = r.identityNumber() == null ? "" : r.identityNumber();
        int tail = id.length() >= 2 ? Integer.parseInt(id.substring(id.length() - 2)) : 0;
        List<String> signals;
        BigDecimal face;
        boolean qrMrz = true;
        if (tail < 30) {
            signals = List.of();
            face = new BigDecimal("0.9100");
        } else if (tail < 70) {
            signals = List.of("FACE_SCORE_BORDERLINE", "UNREADABLE_IMAGE");
            face = new BigDecimal("0.3000");
        } else {
            signals = List.of("QR_MRZ_MISMATCH", "FACE_SCORE_LOW");
            face = new BigDecimal("0.1200");
            qrMrz = false;
        }
        Boolean faceOk = r.biometricConsent() ? face.compareTo(new BigDecimal("0.363")) >= 0 : null;
        KycAiResult.Checks checks = new KycAiResult.Checks(true, true, qrMrz, true, true, faceOk, null, null, null);
        KycAiResult.Extracted extracted = new KycAiResult.Extracted(id,
                r.fullName() == null ? null : NameNormalizer.normalize(r.fullName()), null, null, null, null, null, null);
        return new KycAiResult(checks, r.biometricConsent() ? face : null, r.biometricConsent() ? new BigDecimal("0.9000") : null,
                signals, extracted, List.of(), Map.of("pipeline_version", "stub"));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
