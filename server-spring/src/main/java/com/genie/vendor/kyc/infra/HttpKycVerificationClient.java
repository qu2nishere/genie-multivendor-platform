package com.genie.vendor.kyc.infra;

import com.genie.common.ai.AiWorkerHttpClient;
import com.genie.vendor.kyc.application.KycVerificationClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Gửi hồ sơ tới AI Worker thật: {@code POST {AI_WORKER_URL}/kyc/verify} (hợp đồng ADR 0011 mục 5) — chỉ chờ {@code 202},
 * kết quả về sau qua callback {@code PATCH /api/v1/internal/kyc/{id}/result}.
 */
@Component
@ConditionalOnProperty(prefix = "genie.ai-worker", name = "mode", havingValue = "http", matchIfMissing = true)
public class HttpKycVerificationClient implements KycVerificationClient {

    private final AiWorkerHttpClient http;

    public HttpKycVerificationClient(AiWorkerHttpClient http) {
        this.http = http;
    }

    @Override
    public void requestVerification(Request r) {
        Map<String, Object> declared = new LinkedHashMap<>();
        declared.put("identity_number", r.identityNumber());
        declared.put("full_name", r.fullName());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kyc_id", r.kycId());
        body.put("documents", r.documents());
        body.put("declared", declared);
        body.put("biometric_consent", r.biometricConsent());
        body.put("callback_url", r.callbackUrl());
        http.post("/kyc/verify", body);
    }
}
