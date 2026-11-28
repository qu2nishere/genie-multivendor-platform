// API_SPEC 10.5 "listing-rules", 10.6 GET /seller/categories — UC-CAT-09, 15, 17 (plan C1 P03.02–P03.03)
package com.genie.catalog.product.api;

import com.genie.catalog.product.application.SellerCatalogService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chỉ shop VERIFIED (cổng KYC — API 0.9). */
@RestController
@RequestMapping("/api/v1/seller/categories")
public class SellerCatalogController {

    private final SellerCatalogService catalog;
    private final ApiResponseFactory responses;

    public SellerCatalogController(SellerCatalogService catalog, ApiResponseFactory responses) {
        this.catalog = catalog;
        this.responses = responses;
    }

    @Operation(summary = "UC-CAT-17 — Cây danh mục toàn sàn 2 cấp (số sản phẩm đang bán, hoa hồng đang áp dụng)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerCatalogService.CatalogNode>>> tree() {
        return responses.ok(catalog.tree());
    }

    @Operation(summary = "UC-CAT-09 — Luật đăng bán theo ngành: thuộc tính (kể cả kế thừa), bắt buộc giấy tờ, hoa hồng")
    @GetMapping("/{id}/listing-rules")
    public ResponseEntity<ApiResponse<SellerCatalogService.ListingRules>> listingRules(@PathVariable long id) {
        return responses.ok(catalog.rules(id));
    }
}
