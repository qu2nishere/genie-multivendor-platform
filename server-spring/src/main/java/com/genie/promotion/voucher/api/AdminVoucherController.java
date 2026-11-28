// Admin › Voucher sàn (API_SPEC 11.14; plan 2026-11-26 voucher V04.03)
package com.genie.promotion.voucher.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.PageResponse;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import com.genie.promotion.voucher.application.VoucherAdminViews;
import com.genie.promotion.voucher.application.VoucherInput;
import com.genie.promotion.voucher.application.VoucherManagementQuery;
import com.genie.promotion.voucher.application.VoucherManagementService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code MANAGE_VOUCHER} (Admin + Finance Officer). Mọi thao tác ghi nhật ký {@code VOUCHER_*}. */
@RestController
@RequestMapping("/api/v1/admin/vouchers")
@PreAuthorize("hasAuthority('MANAGE_VOUCHER')")
public class AdminVoucherController {

    private final VoucherManagementService management;
    private final VoucherManagementQuery query;
    private final ApiResponseFactory responses;

    public AdminVoucherController(VoucherManagementService management, VoucherManagementQuery query,
                                  ApiResponseFactory responses) {
        this.management = management;
        this.query = query;
        this.responses = responses;
    }

    @Operation(summary = "UC-PRO-02 — Danh sách voucher sàn + 5 chỉ số",
            description = "status ∈ ACTIVE, SCHEDULED, ENDED, DISABLED; page từ 1, size 1–50; is_coin_reward = quà đổi Điểm.")
    @GetMapping
    public ResponseEntity<PageResponse<VoucherAdminViews.Row>> list(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        VoucherManagementQuery.Page p = query.platformPage(status, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.pageBody(p.rows(), p.pagination(), p.summary()));
    }

    @Operation(summary = "UC-PRO-02 — Chi tiết voucher sàn (form sửa / nhân bản)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> get(@PathVariable @Positive long id) {
        return responses.ok(management.getPlatform(id));
    }

    @Operation(summary = "UC-PRO-02 — Tạo voucher sàn / freeship",
            description = "funded_by PLATFORM | CO_SPONSOR (platform_share_percent 1–99); 409 GNE-VCH-4092 trùng mã.")
    @PostMapping
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> create(@CurrentUser AuthUser me,
            @RequestBody VoucherInput body, ClientInfo client) {
        return responses.created(management.createPlatform(me.id(), body, client));
    }

    @Operation(summary = "UC-PRO-02 — Sửa voucher sàn", description = "Luật khóa trường như 10.7 — 409 GNE-VCH-4094.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> update(@CurrentUser AuthUser me,
            @PathVariable @Positive long id, @RequestBody VoucherInput body, ClientInfo client) {
        return responses.ok(management.updatePlatform(me.id(), id, body, client));
    }

    @Operation(summary = "UC-PRO-02 — Tắt / Huỷ lịch voucher sàn (một chiều)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherAdminViews.Detail>> disable(@CurrentUser AuthUser me,
            @PathVariable @Positive long id, @RequestBody SellerVoucherController.ActiveRequest body, ClientInfo client) {
        return responses.ok(management.disablePlatform(me.id(), id, body.isActive(), client));
    }

    @Operation(summary = "UC-PRO-02 — Xoá vĩnh viễn voucher sàn",
            description = "Chỉ khi đã kết thúc / đã tắt và chưa có lượt dùng; 409 GNE-VCH-4093 / 4094.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthUser me, @PathVariable @Positive long id, ClientInfo client) {
        management.deletePlatform(me.id(), id, client);
        return ResponseEntity.noContent().build();
    }
}
