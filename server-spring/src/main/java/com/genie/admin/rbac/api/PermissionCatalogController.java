// API 11.18 GET /admin/permission-catalog — 13 mã quyền + bộ mặc định theo chức danh (UC-ADM-02; plan B1 R03.02)
package com.genie.admin.rbac.api;

import com.genie.admin.rbac.application.PermissionCatalog;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/permission-catalog")
@PreAuthorize("hasAuthority('MANAGE_RBAC')")
public class PermissionCatalogController {

    private final ApiResponseFactory responses;

    public PermissionCatalogController(ApiResponseFactory responses) {
        this.responses = responses;
    }

    @Operation(summary = "UC-ADM-02 — Danh mục 13 mã quyền và bộ quyền mặc định theo chức danh")
    @GetMapping
    public ResponseEntity<ApiResponse<PermissionCatalog.View>> get() {
        return responses.ok(PermissionCatalog.view());
    }
}
