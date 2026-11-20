package com.genie.catalog.product.api;

import com.genie.catalog.product.application.ProductDetailService;
import com.genie.catalog.product.application.ProductDetailView;
import com.genie.catalog.product.application.VariantChoices;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trang chi tiết sản phẩm (UC-CAT-06; API_SPEC 5.6, 5.10; plan 2026-11-17 C04.03).
 * HTTP: {@code Cache-Control: no-cache} + ETag — trình duyệt / CDN luôn hỏi lại để tồn kho không cũ (D11);
 * phần tĩnh đã cache 5 phút phía máy chủ.
 */
@RestController
@Validated
@RequestMapping("/api/v1/public/products/{id}")
@Tag(name = "Catalog — sản phẩm")
public class PublicProductController {

    private final ProductDetailService details;
    private final ApiResponseFactory responses;

    public PublicProductController(ProductDetailService details, ApiResponseFactory responses) {
        this.details = details;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "UC-CAT-06 Xem chi tiết sản phẩm, ma trận biến thể SKU & tồn kho")
    public ResponseEntity<ApiResponse<ProductDetailView>> detail(@PathVariable @Positive long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(responses.body(details.detail(id)));
    }

    @GetMapping("/variants")
    @Operation(summary = "UC-CAT-06 Phân loại & tồn kho để đổi phân loại trong giỏ")
    public ResponseEntity<ApiResponse<VariantChoices>> variants(@PathVariable @Positive long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(responses.body(details.variants(id)));
    }
}
