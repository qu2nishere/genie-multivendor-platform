package com.genie.vendor.kyc.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.vendor.kyc.application.KycAiResult;
import com.genie.vendor.kyc.application.KycDecisionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Callback của AI Worker — {@code PATCH /api/v1/internal/kyc/{id}/result} (API_SPEC 12; ADR 0011 mục 5). Chỉ đi qua
 * {@code InternalTokenFilter} (header {@code X-Internal-Token}; thiếu / sai → 401). Idempotent: hồ sơ không còn
 * {@code AI_PROCESSING} thì trả trạng thái hiện tại, không đổi gì.
 */
@RestController
@RequestMapping("/api/v1/internal/kyc")
public class InternalKycController {

    private final KycDecisionService decisions;
    private final ApiResponseFactory responses;

    public InternalKycController(KycDecisionService decisions, ApiResponseFactory responses) {
        this.decisions = decisions;
        this.responses = responses;
    }

    public record ChecksDto(Boolean qrCode, Boolean mrz, Boolean mrzQrMatch, Boolean cccdNumberValid, Boolean expiryRuleValid,
                            Boolean faceMatch, Boolean faceDuplicate, Boolean nationalEmblem, Boolean fingerprint) {
    }

    public record SignalDto(@NotBlank @Size(max = 50) String code) {
    }

    public record ExtractedDto(@Size(max = 20) String identityNumber, @Size(max = 255) String fullName,
                               @Size(max = 20) String dateOfBirth, @Size(max = 10) String gender, String address,
                               @Size(max = 20) String issueDate, @Size(max = 20) String expiryDate,
                               @Size(max = 20) String legacyIdentityNumber) {
    }

    public record ResultRequest(ChecksDto checks,
                                @DecimalMin("0") @DecimalMax("1") BigDecimal faceMatchScore,
                                @DecimalMin("0") @DecimalMax("1") BigDecimal livenessScore,
                                List<@Valid SignalDto> signals,
                                ExtractedDto extracted,
                                List<@Size(max = 500) String> errors,
                                Map<String, String> model) {

        KycAiResult toResult() {
            KycAiResult.Checks c = checks == null ? KycAiResult.Checks.empty()
                    : new KycAiResult.Checks(checks.qrCode(), checks.mrz(), checks.mrzQrMatch(), checks.cccdNumberValid(),
                    checks.expiryRuleValid(), checks.faceMatch(), checks.faceDuplicate(), checks.nationalEmblem(),
                    checks.fingerprint());
            KycAiResult.Extracted e = extracted == null ? null
                    : new KycAiResult.Extracted(extracted.identityNumber(), extracted.fullName(), extracted.dateOfBirth(),
                    extracted.gender(), extracted.address(), extracted.issueDate(), extracted.expiryDate(),
                    extracted.legacyIdentityNumber());
            return new KycAiResult(c, faceMatchScore, livenessScore,
                    signals == null ? List.of() : signals.stream().map(SignalDto::code).toList(), e, errors, model);
        }
    }

    public record ResultResponse(String kycStatus) {
    }

    @Operation(summary = "UC-VND-03 — Nhận kết quả thẩm định KYC từ AI Worker")
    @PatchMapping("/{id}/result")
    public ResponseEntity<ApiResponse<ResultResponse>> result(@PathVariable long id, @Valid @RequestBody ResultRequest body) {
        return responses.ok(new ResultResponse(decisions.apply(id, body.toResult()).name()));
    }
}
