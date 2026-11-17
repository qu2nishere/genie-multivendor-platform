package com.genie.admin.audit.api;

import com.genie.admin.audit.application.AuditLogQuery;
import com.genie.admin.audit.application.AuditLogQueryService;
import com.genie.admin.audit.application.AuditLogView;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nhật ký hệ thống của Admin Portal — UC-ADM-07 (API_SPEC 11.10). Chỉ đọc; quyền {@code VIEW_AUDIT_LOG}
 * (Admin có sẵn; nhân viên thiếu quyền → {@code 403 GNE-SEC-4030} + dòng {@code ACCESS_DENIED}).
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAuthority('VIEW_AUDIT_LOG')")
public class AdminAuditLogController {

    private final AuditLogQueryService service;
    private final ApiResponseFactory responses;

    public AdminAuditLogController(AuditLogQueryService service, ApiResponseFactory responses) {
        this.service = service;
        this.responses = responses;
    }

    @Operation(summary = "UC-ADM-07 — Tra cứu nhật ký kiểm toán",
            description = "Lọc theo thời gian (mặc định 7 ngày, tối đa 90 ngày), nhóm hành động, từ khóa (email / IP / mã "
                    + "tham chiếu); phân trang cursor; summary.denied_count = số lần truy cập bị từ chối trong bộ lọc. "
                    + "Lỗi: 401 GNE-SEC-4010, 403 GNE-SEC-4030, 422 GNE-VAL-4220.")
    @GetMapping
    public ResponseEntity<ApiListResponse<AuditLogView>> search(
            @Parameter(description = "ISO-8601 hoặc YYYY-MM-DD (giờ Việt Nam), bao gồm") @RequestParam(required = false) String from,
            @Parameter(description = "ISO-8601 (loại trừ) hoặc YYYY-MM-DD (tính hết ngày)") @RequestParam(required = false) String to,
            @Parameter(description = "AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG") @RequestParam(required = false) String category,
            @Parameter(description = "Email / IP / mã tham chiếu") @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor,
            @Parameter(description = "1–100, mặc định 50") @RequestParam(required = false) Integer limit) {
        var result = service.search(new AuditLogQuery(from, to, category, q, cursor, limit));
        ResponseEntity<ApiListResponse<AuditLogView>> body =
                responses.list(result.page(), Map.of("denied_count", result.deniedCount()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body.getBody());
    }
}
