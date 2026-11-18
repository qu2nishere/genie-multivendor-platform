package com.genie.promotion.coin.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.promotion.coin.application.CheckInService;
import com.genie.promotion.coin.application.CoinQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điểm Genie của chính người đang đăng nhập — UC-PRO-08, UC-PRO-09 (API_SPEC 3.13). {@code userId} luôn lấy từ token,
 * không nhận từ request (chống IDOR — N9).
 */
@RestController
@RequestMapping("/api/v1/me/coins")
public class MeCoinController {

    private final CheckInService checkIns;
    private final CoinQueryService query;
    private final ApiResponseFactory responses;

    public MeCoinController(CheckInService checkIns, CoinQueryService query, ApiResponseFactory responses) {
        this.checkIns = checkIns;
        this.query = query;
        this.responses = responses;
    }

    @Operation(summary = "UC-PRO-09 — Số dư Điểm, Điểm sắp hết hạn, trạng thái điểm danh")
    @GetMapping
    public ResponseEntity<ApiResponse<CoinQueryService.Summary>> summary(@CurrentUser AuthUser me) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(query.summary(me.id())).getBody());
    }

    @Operation(summary = "UC-PRO-09 — Lịch sử biến động Điểm",
            description = "Mới nhất trước, phân trang cursor; limit 1–50 (mặc định 20); amount âm khi tiêu.")
    @GetMapping("/transactions")
    public ResponseEntity<ApiListResponse<CoinQueryService.TransactionView>> transactions(
            @CurrentUser AuthUser me, @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        var body = responses.list(query.transactions(me.id(), cursor, limit), null).getBody();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @Operation(summary = "UC-PRO-08 — Điểm danh nhận Điểm",
            description = "201 {reward, balance, streak_days}; 409 GNE-PRO-4091 khi hôm nay (giờ Việt Nam) đã điểm danh.")
    @PostMapping("/check-ins")
    public ResponseEntity<ApiResponse<CheckInService.CheckInResult>> checkIn(@CurrentUser AuthUser me) {
        return responses.created(checkIns.checkIn(me.id()));
    }
}
