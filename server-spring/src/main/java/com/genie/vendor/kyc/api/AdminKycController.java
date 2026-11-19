package com.genie.vendor.kyc.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import com.genie.vendor.kyc.application.KycQueryService;
import com.genie.vendor.kyc.application.KycReviewService;
import com.genie.vendor.kyc.domain.KycRejectReason;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Duyệt KYC — UC-VND-04 (API_SPEC 11.3), quyền {@code REVIEW_KYC}. Mỗi hành động trả {@code 200} kèm chi tiết đã cập nhật.
 * Phản hồi {@code Cache-Control: no-store} (dữ liệu định danh + URL ảnh có chữ ký).
 */
@RestController
@Validated
@RequestMapping("/api/v1/admin/kyc-submissions")
@PreAuthorize("hasAuthority('REVIEW_KYC')")
public class AdminKycController {

    private final KycQueryService queries;
    private final KycReviewService reviews;
    private final ApiResponseFactory responses;

    public AdminKycController(KycQueryService queries, KycReviewService reviews, ApiResponseFactory responses) {
        this.queries = queries;
        this.reviews = reviews;
        this.responses = responses;
    }

    public record ApproveRequest(BigDecimal shopCommissionRate, Boolean isMall) {
    }

    public record RejectRequest(@NotNull KycRejectReason reasonCode, @Size(max = 1000) String note) {
    }

    public record SupplementRequest(@NotNull @Size(min = 1, max = 6) List<String> documents, @Size(max = 1000) String note) {
    }

    @Operation(summary = "UC-VND-04 — Hàng chờ hồ sơ KYC (rủi ro cao trước)")
    @GetMapping
    public ResponseEntity<ApiListResponse<KycQueryService.SubmissionRow>> list(
            @RequestParam(required = false) @Pattern(regexp = "PENDING|SUPPLEMENT_REQUESTED|VERIFIED|REJECTED") String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        var res = responses.page(queries.search(status, page, size), Map.of("counts", queries.counts()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(res.getBody());
    }

    @Operation(summary = "UC-VND-04 — Chi tiết hồ sơ KYC (ảnh xem 5 phút, ghi nhật ký mỗi lần mở)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KycQueryService.SubmissionDetail>> detail(@CurrentUser AuthUser me,
                                                                               @PathVariable long id, ClientInfo client) {
        return noStore(queries.detail(id, me.id(), client, true));
    }

    @Operation(summary = "UC-VND-04 — Phê duyệt hồ sơ KYC (tùy chọn hoa hồng riêng, nhãn Chính hãng)")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<KycQueryService.SubmissionDetail>> approve(
            @CurrentUser AuthUser me, @PathVariable long id, @RequestBody(required = false) ApproveRequest body,
            ClientInfo client) {
        ApproveRequest b = body == null ? new ApproveRequest(null, null) : body;
        reviews.approve(id, me.id(), b.shopCommissionRate(), b.isMall(), hasAuthority("UPDATE_COMMISSION"), client);
        return noStore(queries.detail(id, me.id(), client, false));
    }

    @Operation(summary = "UC-VND-04 — Từ chối hồ sơ KYC kèm lý do")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<KycQueryService.SubmissionDetail>> reject(
            @CurrentUser AuthUser me, @PathVariable long id, @Valid @RequestBody RejectRequest body, ClientInfo client) {
        reviews.reject(id, me.id(), body.reasonCode(), body.note(), client);
        return noStore(queries.detail(id, me.id(), client, false));
    }

    @Operation(summary = "UC-VND-04 — Yêu cầu bổ sung giấy tờ (hạn 7 ngày)")
    @PostMapping("/{id}/request-supplement")
    public ResponseEntity<ApiResponse<KycQueryService.SubmissionDetail>> requestSupplement(
            @CurrentUser AuthUser me, @PathVariable long id, @Valid @RequestBody SupplementRequest body, ClientInfo client) {
        reviews.requestSupplement(id, me.id(), body.documents(), body.note(), client);
        return noStore(queries.detail(id, me.id(), client, false));
    }

    @Operation(summary = "UC-VND-04 — Đưa hồ sơ trở lại hàng chờ duyệt")
    @PostMapping("/{id}/requeue")
    public ResponseEntity<ApiResponse<KycQueryService.SubmissionDetail>> requeue(@CurrentUser AuthUser me,
                                                                                @PathVariable long id, ClientInfo client) {
        reviews.requeue(id, me.id(), client);
        return noStore(queries.detail(id, me.id(), client, false));
    }

    private static boolean hasAuthority(String authority) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(data).getBody());
    }
}
