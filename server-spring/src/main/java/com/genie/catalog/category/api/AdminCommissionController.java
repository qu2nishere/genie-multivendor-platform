// API 11.8 — Cấu hình hoa hồng (UC-CAT-14 bước 8–9; plan B2 C05.04). Chỉ UPDATE_COMMISSION.
package com.genie.catalog.category.api;

import com.genie.catalog.category.application.CommissionService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tỷ lệ tính bằng phần trăm, khung 3,5–8 % (plan B2 D1) — ngoài khung {@code 422 GNE-VAL-4220}. */
@RestController
@RequestMapping("/api/v1/admin/commission-settings")
@PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
public class AdminCommissionController {

    private final CommissionService commissions;
    private final ApiResponseFactory responses;

    public AdminCommissionController(CommissionService commissions, ApiResponseFactory responses) {
        this.commissions = commissions;
        this.responses = responses;
    }

    /** {@code { "default_rate": 5.0 }} và / hoặc {@code { "categories": [{ category_id, rate, is_enabled }] }}. */
    public record CommissionPatch(BigDecimal defaultRate, List<CommissionService.CategoryRateChange> categories) {
    }

    @Operation(summary = "UC-CAT-14 — Hoa hồng mặc định toàn sàn + theo ngành gốc")
    @GetMapping
    public ResponseEntity<ApiResponse<CommissionService.CommissionView>> get() {
        return responses.ok(commissions.settings());
    }

    @Operation(summary = "UC-CAT-14 — Đổi hoa hồng mặc định / theo ngành (chỉ áp cho đơn mới)")
    @PatchMapping
    public ResponseEntity<ApiResponse<CommissionService.CommissionView>> patch(@CurrentUser AuthUser me,
                                                                              @RequestBody CommissionPatch body,
                                                                              ClientInfo client) {
        return responses.ok(commissions.update(body.defaultRate(), body.categories(), me.id(), client));
    }
}
