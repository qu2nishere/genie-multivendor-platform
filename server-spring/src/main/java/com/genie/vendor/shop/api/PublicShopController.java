package com.genie.vendor.shop.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.vendor.shop.application.PublicShopService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Trang gian hàng công khai (UC-CAT-16; API_SPEC 5.11, 5.12; plan 2026-11-17 C06.02). */
@RestController
@Validated
@RequestMapping("/api/v1/public/shops")
@Tag(name = "Vendor — trang shop")
public class PublicShopController {

    private final PublicShopService shops;
    private final ApiResponseFactory responses;

    public PublicShopController(PublicShopService shops, ApiResponseFactory responses) {
        this.shops = shops;
        this.responses = responses;
    }

    @GetMapping("/{id}")
    @Operation(summary = "UC-CAT-16 Xem trang gian hàng: thông tin, chỉ số uy tín, danh mục của shop")
    public ResponseEntity<ApiResponse<PublicShopService.ShopPage>> shop(@PathVariable @Positive long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .body(responses.body(shops.shop(id)));
    }

    @GetMapping
    @Operation(summary = "UC-CAT-16 Shop đã xác minh nổi bật (trang chủ)")
    public ResponseEntity<ApiResponse<List<PublicShopService.FeaturedShop>>> featured(
            @RequestParam(defaultValue = "true") boolean featured, @RequestParam(defaultValue = "6") int limit) {
        if (!featured || limit < 1 || limit > 24) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Chỉ hỗ trợ featured=true, limit từ 1 đến 24.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(responses.body(shops.featured(limit)));
    }
}
