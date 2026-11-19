package com.genie.vendor.shop.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.vendor.shop.application.ShopFacade;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /admin/shops?q&limit=20} — ô chọn shop (đích banner — API 11.16; plan Mở shop & KYC K10.05), quyền MANAGE_VOUCHER. */
@RestController
@Validated
@RequestMapping("/api/v1/admin/shops")
public class AdminShopController {

    private final ShopFacade shops;
    private final ApiResponseFactory responses;

    public AdminShopController(ShopFacade shops, ApiResponseFactory responses) {
        this.shops = shops;
        this.responses = responses;
    }

    public record ShopOption(long id, String name) {
    }

    @Operation(summary = "UC-ADM-12 — Tìm shop đã xác minh cho ô chọn đích banner")
    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_VOUCHER')")
    public ResponseEntity<ApiResponse<List<ShopOption>>> search(@RequestParam(defaultValue = "") @Size(max = 100) String q,
                                                               @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        return responses.ok(shops.searchVerified(q, limit).stream().map(s -> new ShopOption(s.id(), s.name())).toList());
    }
}
