package com.genie.catalog.product.api;

import com.genie.catalog.product.application.ProductCard;
import com.genie.catalog.product.application.RecommendationService;
import com.genie.catalog.product.application.SimilarProductService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.PageResponse;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sản phẩm tương tự & gợi ý hôm nay (UC-CAT-07; API_SPEC 5.5, 5.9; plan 2026-11-17 C05.03). */
@RestController
@Validated
@Tag(name = "Catalog — gợi ý")
public class PublicRecommendationController {

    static final int MAX_SIMILAR = 12;
    static final int MAX_PAGE_SIZE = 60;

    private final SimilarProductService similar;
    private final RecommendationService recommendations;
    private final ApiResponseFactory responses;

    public PublicRecommendationController(SimilarProductService similar, RecommendationService recommendations,
                                          ApiResponseFactory responses) {
        this.similar = similar;
        this.recommendations = recommendations;
        this.responses = responses;
    }

    @GetMapping("/api/v1/public/products/{id}/similar")
    @Operation(summary = "UC-CAT-07 Sản phẩm tương tự (cùng danh mục lá, giá ±50%, chấm điểm tên + bán chạy + đánh giá)")
    public ResponseEntity<ApiResponse<List<ProductCard>>> similar(@PathVariable @Positive long id,
                                                                  @RequestParam(defaultValue = "6") int limit) {
        if (limit < 1 || limit > MAX_SIMILAR) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "limit phải từ 1 đến " + MAX_SIMILAR + ".");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(responses.body(similar.similar(id, limit)));
    }

    @GetMapping("/api/v1/public/recommendations")
    @Operation(summary = "UC-CAT-07 Gợi ý cho bạn / Gợi ý hôm nay (có token → ưu tiên danh mục đã xem gần đây)")
    public ResponseEntity<PageResponse<ProductCard>> recommendations(@RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(defaultValue = "30") int size,
                                                                     @CurrentUser AuthUser me) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "page ≥ 1, size từ 1 đến " + MAX_PAGE_SIZE + ".");
        }
        RecommendationService.Page result = recommendations.page(me == null ? null : me.id(), page, size);
        // Có token → nội dung riêng từng người: không cho CDN / proxy cache chung
        CacheControl cc = me == null ? CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()
                : CacheControl.noCache().cachePrivate();
        return ResponseEntity.ok().cacheControl(cc)
                .body(responses.pageBody(result.items(), PageResponse.Pagination.of(page, size, result.totalItems()), null));
    }
}
