package com.genie.vendor.shop.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.vendor.shop.application.SellerShellService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /seller/me} — khung Seller Center (API_SPEC 10.1). Mở cả khi shop chưa VERIFIED (cổng KYC — API 0.9). */
@RestController
@RequestMapping("/api/v1/seller/me")
public class SellerMeController {

    private final SellerShellService shells;
    private final ApiResponseFactory responses;

    public SellerMeController(SellerShellService shells, ApiResponseFactory responses) {
        this.shells = shells;
        this.responses = responses;
    }

    @Operation(summary = "UC-VND-01 — Khung Seller Center: shop, trạng thái KYC, badge")
    @GetMapping
    public ResponseEntity<ApiResponse<SellerShellService.SellerShell>> me(@CurrentUser AuthUser me) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(shells.shell(me.id())).getBody());
    }
}
