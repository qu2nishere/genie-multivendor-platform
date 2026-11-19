package com.genie.promotion.coin.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.promotion.coin.application.CoinRewardCatalog;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục quà đổi Điểm — UC-PRO-09 (API_SPEC 3.13). Công khai, cache 5 phút. */
@RestController
public class PublicCoinRewardController {

    private final CoinRewardCatalog catalog;
    private final ApiResponseFactory responses;

    public PublicCoinRewardController(CoinRewardCatalog catalog, ApiResponseFactory responses) {
        this.catalog = catalog;
        this.responses = responses;
    }

    @Operation(summary = "UC-PRO-09 — Danh mục quà đổi Điểm")
    @GetMapping("/api/v1/public/coin-rewards")
    public ResponseEntity<ApiResponse<List<CoinRewardCatalog.RewardView>>> list() {
        var body = responses.ok(catalog.list()).getBody();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(body);
    }
}
