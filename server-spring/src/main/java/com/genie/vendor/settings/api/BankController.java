package com.genie.vendor.settings.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.vendor.settings.application.BankCatalog;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /public/banks} — danh mục ngân hàng (API 10.11; dùng chung modal trả hàng của khách). Cache 1 ngày. */
@RestController
public class BankController {

    private final BankCatalog banks;
    private final ApiResponseFactory responses;

    public BankController(BankCatalog banks, ApiResponseFactory responses) {
        this.banks = banks;
        this.responses = responses;
    }

    @Operation(summary = "UC-VND-07 — Danh mục ngân hàng")
    @GetMapping("/api/v1/public/banks")
    public ResponseEntity<ApiResponse<List<BankCatalog.Bank>>> list() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(responses.ok(banks.all()).getBody());
    }
}
