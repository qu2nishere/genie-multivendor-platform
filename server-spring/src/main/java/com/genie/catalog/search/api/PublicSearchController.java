package com.genie.catalog.search.api;

import com.genie.catalog.search.application.SuggestionService;
import com.genie.catalog.search.application.TrendingService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gợi ý khi gõ & từ khóa xu hướng (UC-CAT-04; API_SPEC 5.4, 5.4b; plan 2026-11-17 C03.05). */
@RestController
@RequestMapping("/api/v1/public/search")
@Tag(name = "Catalog — tìm kiếm")
public class PublicSearchController {

    private final SuggestionService suggestions;
    private final TrendingService trending;
    private final RateLimiter rateLimiter;
    private final ApiResponseFactory responses;

    public PublicSearchController(SuggestionService suggestions, TrendingService trending, RateLimiter rateLimiter,
                                  ApiResponseFactory responses) {
        this.suggestions = suggestions;
        this.trending = trending;
        this.rateLimiter = rateLimiter;
        this.responses = responses;
    }

    @GetMapping("/suggestions")
    @Operation(summary = "UC-CAT-04 Gợi ý khi gõ: tối đa 2 danh mục, 2 shop, 5 sản phẩm")
    public ResponseEntity<ApiResponse<SuggestionService.Suggestions>> suggestions(
            @RequestParam(required = false, defaultValue = "") String q, ClientInfo client) {
        rateLimiter.check("rl:search:" + client.ip(), PublicProductSearchController.RATE_LIMIT_PER_MINUTE, Duration.ofMinutes(1));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(responses.body(suggestions.suggest(q)));
    }

    @GetMapping("/trending")
    @Operation(summary = "UC-CAT-04 Từ khóa xu hướng 24 giờ qua (đã lọc từ bị chặn)")
    public ResponseEntity<ApiResponse<List<TrendingService.TrendingKeyword>>> trending(
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 10) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "limit phải từ 1 đến 10.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(responses.body(trending.top(limit)));
    }
}
