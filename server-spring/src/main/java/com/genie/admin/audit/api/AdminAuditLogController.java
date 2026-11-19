package com.genie.admin.audit.api;

import com.genie.admin.audit.application.AuditLogFilter;
import com.genie.admin.audit.application.AuditLogQuery;
import com.genie.admin.audit.application.AuditLogQueryService;
import com.genie.admin.audit.application.AuditLogView;
import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.common.security.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

    static final String FILE_NAME = "nhat-ky-he-thong.csv";
    static final int EXPORT_PER_MINUTE = 5;

    private final AuditLogQueryService service;
    private final ApiResponseFactory responses;
    private final RateLimiter rateLimiter;
    private final int exportMax;

    public AdminAuditLogController(AuditLogQueryService service, ApiResponseFactory responses, RateLimiter rateLimiter,
                                   @Value("${genie.audit.export-max:" + AuditLogQueryService.EXPORT_MAX + "}") int exportMax) {
        this.service = service;
        this.responses = responses;
        this.rateLimiter = rateLimiter;
        this.exportMax = exportMax;
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

    @Operation(summary = "UC-ADM-07 — Xuất nhật ký kiểm toán ra CSV",
            description = "Cùng bộ lọc với tra cứu (không cursor / limit). text/csv UTF-8 có BOM, tối đa 50.000 dòng — bị cắt thì "
                    + "có header X-Truncated: true. Giới hạn 5 lần / phút / người (429 GNE-RAT-4290).")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
                       @RequestParam(required = false) String category, @RequestParam(required = false) String q,
                       @CurrentUser AuthUser me, HttpServletResponse response) throws IOException {
        rateLimiter.check("rl:auditexport:" + me.id(), EXPORT_PER_MINUTE, Duration.ofMinutes(1));
        AuditLogFilter filter = service.normalize(AuditLogQuery.forExport(from, to, category, q), false);
        boolean truncated = service.count(filter) > exportMax;
        response.setStatus(200);
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(FILE_NAME).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Disposition, X-Truncated");
        if (truncated) {
            response.setHeader("X-Truncated", "true");
        }
        service.exportCsv(filter, exportMax, response.getWriter());
    }
}
