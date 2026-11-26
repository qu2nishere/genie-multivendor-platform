// API_SPEC 10.5 "Thương hiệu" — UC-CAT-09 bước 12 (plan C1 P03.04, D10). Cổng KYC: chỉ shop VERIFIED (API 0.9).
package com.genie.catalog.brand.api;

import com.genie.catalog.brand.application.BrandService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/brands")
public class SellerBrandController {

    private final BrandService brands;
    private final ApiResponseFactory responses;

    public SellerBrandController(BrandService brands, ApiResponseFactory responses) {
        this.brands = brands;
        this.responses = responses;
    }

    public record CreateBody(String name) {
    }

    @Operation(summary = "UC-CAT-09 — Tìm thương hiệu cho form đăng bán")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandService.BrandView>>> search(@RequestParam(required = false) String q,
                                                                          @RequestParam(required = false) Integer limit) {
        return responses.ok(brands.search(q, limit));
    }

    @Operation(summary = "UC-CAT-09 — Thêm thương hiệu mới (chưa xác minh); trùng tên trả bản có sẵn")
    @PostMapping
    public ResponseEntity<ApiResponse<BrandService.BrandView>> create(@CurrentUser AuthUser me,
                                                                    @RequestBody CreateBody body) {
        BrandService.Saved saved = brands.create(me.id(), body == null ? null : body.name());
        return responses.of(saved.created() ? HttpStatus.CREATED : HttpStatus.OK, saved.brand());
    }
}
