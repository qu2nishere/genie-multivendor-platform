package com.genie.auth.otp.api;

import com.genie.auth.otp.application.StepUpOtpService;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OTP bước 2 cho thao tác nhạy cảm của người đang đăng nhập (API_SPEC 3.5).
 */
@RestController
@RequestMapping("/api/v1/me/otp-challenges")
public class MeOtpController {

    private final StepUpOtpService stepUpOtps;
    private final ApiResponseFactory responses;

    public MeOtpController(StepUpOtpService stepUpOtps, ApiResponseFactory responses) {
        this.stepUpOtps = stepUpOtps;
        this.responses = responses;
    }

    public record StepUpRequest(@NotNull OtpPurpose purpose) {
    }

    @Operation(summary = "UC-AUTH-06 / UC-AUTH-10 — Gửi mã OTP bước 2 tới email của tài khoản")
    @PostMapping
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> request(@CurrentUser AuthUser me,
                                                                     @Valid @RequestBody StepUpRequest body) {
        return responses.accepted(OtpChallengeResponse.from(stepUpOtps.request(me.id(), body.purpose())));
    }

    @Operation(summary = "UC-AUTH-06 — Gửi lại mã OTP bước 2 (sau 60 giây)")
    @PostMapping("/{challengeId}/resend")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> resend(@CurrentUser AuthUser me,
                                                                    @PathVariable String challengeId) {
        return responses.accepted(OtpChallengeResponse.from(stepUpOtps.resend(me.id(), challengeId)));
    }
}
