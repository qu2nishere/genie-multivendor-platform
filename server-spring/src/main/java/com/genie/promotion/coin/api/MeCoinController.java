package com.genie.promotion.coin.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.promotion.coin.application.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điểm Genie của chính người đang đăng nhập — UC-PRO-08, UC-PRO-09 (API_SPEC 3.13). {@code userId} luôn lấy từ token,
 * không nhận từ request (chống IDOR — N9).
 */
@RestController
@RequestMapping("/api/v1/me/coins")
public class MeCoinController {

    private final CheckInService checkIns;
    private final ApiResponseFactory responses;

    public MeCoinController(CheckInService checkIns, ApiResponseFactory responses) {
        this.checkIns = checkIns;
        this.responses = responses;
    }

    @Operation(summary = "UC-PRO-08 — Điểm danh nhận Điểm",
            description = "201 {reward, balance, streak_days}; 409 GNE-PRO-4091 khi hôm nay (giờ Việt Nam) đã điểm danh.")
    @PostMapping("/check-ins")
    public ResponseEntity<ApiResponse<CheckInService.CheckInResult>> checkIn(@CurrentUser AuthUser me) {
        return responses.created(checkIns.checkIn(me.id()));
    }
}
