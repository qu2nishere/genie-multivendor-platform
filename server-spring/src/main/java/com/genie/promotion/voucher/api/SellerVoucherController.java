// Seller › Khuyến mãi — voucher gian hàng (API_SPEC 10.7; plan 2026-11-26 voucher V03.05)
package com.genie.promotion.voucher.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.PageResponse;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.promotion.voucher.application.VoucherAdminViews;
import com.genie.promotion.voucher.application.VoucherInput;
import com.genie.promotion.voucher.application.VoucherManagementQuery;
import com.genie.promotion.voucher.application.VoucherManagementService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /seller/**} đã yêu cầu vai trò VENDOR + shop {@code VERIFIED} (cổng KYC — {@code 403 GNE-VND-4031}). Voucher luôn
 * gắn shop của người gọi (token); voucher của shop khác → {@code 404}.
 */
@RestController
@RequestMapping("/api/v1/seller/vouchers")
public class SellerVoucherController {

    private final VoucherManagementService management;
    private final VoucherManagementQuery query;
    private final ApiResponseFactory responses;

    public SellerVoucherController(VoucherManagementService management, VoucherManagementQuery query,
                                   ApiResponseFactory responses) {
        this.management = management;
        this.query = query;
        this.responses = responses;
    }

    /** Thân {@code PATCH} (tắt). */
    public record ActiveRequest(Boolean isActive) {
    }

    @Operation(summary = "UC-PRO-01 — Danh sách voucher shop + 3 chỉ số",
            description = "status ∈ ACTIVE, SCHEDULED, ENDED, DISABLED (bỏ = tất cả); page từ 1, size 1–50 (mặc định 10).")
    @GetMapping
    public ResponseEntity<PageResponse<VoucherAdminViews.Row>> list(@CurrentUser AuthUser me,
            @RequestParam(required = false) String status, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        VoucherManagementQuery.Page p = query.shopPage(me.id(), status, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.pageBody(p.rows(), p.pagination(), p.summary()));
    }

    @Operation(summary = "UC-PRO-01 — Chi tiết voucher shop (form sửa / nhân bản)",
            description = "Kèm 4 tập phạm vi và locked_fields — trường đang khóa theo trạng thái.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> get(@CurrentUser AuthUser me,
            @PathVariable @Positive long id) {
        return responses.ok(management.getShop(me.id(), id));
    }

    @Operation(summary = "UC-PRO-01 — Tạo voucher shop",
            description = "Tiêu đề tự sinh; 409 GNE-VCH-4092 trùng mã; 422 errors[] theo trường.")
    @PostMapping
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> create(@CurrentUser AuthUser me,
            @RequestBody VoucherInput body) {
        return responses.created(management.createShop(me.id(), body));
    }

    @Operation(summary = "UC-PRO-01 — Sửa voucher shop",
            description = "Đang chạy / đã có người lưu: chỉ kéo dài hạn, tăng lượt / ngân sách — còn lại 409 GNE-VCH-4094 "
                    + "details.locked_fields.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> update(@CurrentUser AuthUser me,
            @PathVariable @Positive long id, @RequestBody VoucherInput body) {
        return responses.ok(management.updateShop(me.id(), id, body));
    }

    @Operation(summary = "UC-PRO-01 — Tắt voucher shop (một chiều)", description = "{\"is_active\": false}; true → 422.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> disable(@CurrentUser AuthUser me,
            @PathVariable @Positive long id, @RequestBody ActiveRequest body) {
        return responses.ok(management.disableShop(me.id(), id, body.isActive()));
    }
}
