package com.genie.vendor.kyc.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import com.genie.vendor.kyc.application.KycSubmissionService;
import com.genie.vendor.kyc.domain.KycDocumentType;
import com.genie.vendor.shop.domain.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ KYC của người bán — UC-VND-02 (API_SPEC 10.12). Mở cả khi shop chưa VERIFIED (cổng KYC — API 0.9). Phản hồi
 * {@code Cache-Control: no-store} (dữ liệu định danh — K12.02).
 */
@RestController
@RequestMapping("/api/v1/seller/kyc")
public class SellerKycController {

    private final KycSubmissionService kyc;
    private final ApiResponseFactory responses;

    public SellerKycController(KycSubmissionService kyc, ApiResponseFactory responses) {
        this.kyc = kyc;
        this.responses = responses;
    }

    /**
     * Thân lưu nháp / gửi duyệt (API 10.12). {@code document_object_keys}: khóa {@code tmp/…} vừa tải lên
     * ({@code purpose = KYC_DOCUMENT}); giấy tờ không gửi = giữ bản cũ. {@code document_file_names} (tùy chọn): tên tệp
     * gốc để màn hình hiển thị lại.
     */
    public record KycRequest(
            @Size(max = 100) String shopName,
            BusinessType businessType,
            @Size(max = 12) String identityNumber,
            @Size(max = 20) String taxCode,
            @Size(max = 255) String representative,
            Map<KycDocumentType, @Size(max = 300) String> documentObjectKeys,
            Map<KycDocumentType, @Size(max = 255) String> documentFileNames,
            Boolean biometricConsent) {

        KycSubmissionService.KycForm toForm() {
            return new KycSubmissionService.KycForm(shopName, businessType, identityNumber, taxCode, representative,
                    documentObjectKeys, documentFileNames, biometricConsent);
        }
    }

    @Operation(summary = "UC-VND-02 — Xem hồ sơ KYC của shop (không trả URL ảnh)")
    @GetMapping
    public ResponseEntity<ApiResponse<KycSubmissionService.KycDossierView>> get(@CurrentUser AuthUser me) {
        return noStore(HttpStatus.OK, kyc.get(me.id()));
    }

    @Operation(summary = "UC-VND-02 — Lưu nháp hồ sơ KYC")
    @PutMapping("/draft")
    public ResponseEntity<ApiResponse<KycSubmissionService.KycDossierView>> draft(@CurrentUser AuthUser me,
                                                                                  @Valid @RequestBody KycRequest body) {
        return noStore(HttpStatus.OK, kyc.saveDraft(me.id(), body.toForm()));
    }

    @Operation(summary = "UC-VND-02 — Gửi duyệt / gửi lại hồ sơ KYC (AI thẩm định bất đồng bộ)")
    @PostMapping("/submissions")
    public ResponseEntity<ApiResponse<KycSubmissionService.SubmitResult>> submit(@CurrentUser AuthUser me,
                                                                                 @Valid @RequestBody KycRequest body,
                                                                                 ClientInfo client) {
        return noStore(HttpStatus.ACCEPTED, kyc.submit(me.id(), body.toForm(), client));
    }

    @Operation(summary = "UC-VND-03 — Hồ sơ bị từ chối: yêu cầu nhân viên xem xét (1 lần)")
    @PostMapping("/manual-review-requests")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestManualReview(@CurrentUser AuthUser me) {
        kyc.requestManualReview(me.id());
        return noStore(HttpStatus.ACCEPTED, Map.of("status", "MANUAL_REVIEW"));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(HttpStatus status, T data) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(responses.ok(data).getBody());
    }
}
