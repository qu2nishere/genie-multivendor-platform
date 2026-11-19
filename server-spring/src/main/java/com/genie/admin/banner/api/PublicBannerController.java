package com.genie.admin.banner.api;

import com.genie.admin.banner.application.BannerQueryService;
import com.genie.admin.banner.application.PublicBannerView;
import com.genie.admin.banner.domain.BannerPosition;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Banner công khai trên storefront — UC-ADM-12 (API_SPEC 5.14, 0.12). Không cần đăng nhập. */
@RestController
@RequestMapping("/api/v1/public/banners")
public class PublicBannerController {

    private final BannerQueryService query;
    private final ApiResponseFactory responses;

    public PublicBannerController(BannerQueryService query, ApiResponseFactory responses) {
        this.query = query;
        this.responses = responses;
    }

    @Operation(summary = "UC-ADM-12 — Banner đang hiển thị theo vị trí",
            description = "positions: danh sách vị trí cách nhau dấu phẩy; category_id bắt buộc khi có CATEGORY_TOP. "
                    + "Chỉ banner LIVE, sắp theo sort_order; cache 60 giây. Sai tham số → 422 GNE-VAL-4220.")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<BannerPosition, java.util.List<PublicBannerView>>>> live(
            @Parameter(description = "HOME_TOP_CAROUSEL,HOME_TOP_SIDE,HOME_MIDDLE,FLASH_SALE_TOP,POPUP,CATEGORY_TOP")
            @RequestParam String positions,
            @RequestParam(name = "category_id", required = false) Long categoryId) {
        Set<BannerPosition> wanted = new LinkedHashSet<>();
        for (String raw : positions.split(",")) {
            String p = raw.trim().toUpperCase(Locale.ROOT);
            if (p.isEmpty()) {
                continue;
            }
            try {
                wanted.add(BannerPosition.valueOf(p));
            } catch (IllegalArgumentException e) {
                throw InvalidRequestException.of("positions", "INVALID_VALUE", raw.trim(), "Vị trí banner không hợp lệ.");
            }
        }
        if (wanted.isEmpty()) {
            throw InvalidRequestException.of("positions", "REQUIRED", positions, "Cần ít nhất một vị trí.");
        }
        if (wanted.contains(BannerPosition.CATEGORY_TOP) && categoryId == null) {
            throw InvalidRequestException.of("category_id", "REQUIRED", null, "CATEGORY_TOP cần category_id.");
        }
        var body = responses.ok(query.live(wanted, categoryId)).getBody();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()).body(body);
    }
}
