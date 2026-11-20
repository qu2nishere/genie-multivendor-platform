// API 2.11 — Nhân viên kích hoạt lời mời (UC-ADM-01 bước 5; plan B1 R02.05, D2)
package com.genie.auth.staff.api;

import com.genie.auth.login.application.AuthResult;
import com.genie.auth.otp.api.OtpChallengeResponse;
import com.genie.auth.staff.application.StaffActivationService;
import com.genie.auth.token.api.AuthTokensResponse;
import com.genie.auth.token.api.RefreshCookies;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.StrongPassword;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mở từ liên kết trong email mời {@code /auth/staff-activation?token=inv_…}: bước 1 xin OTP gửi tới email được mời,
 * bước 2 nhập OTP + mật khẩu mới → {@code 201 $AuthTokens} (cookie refresh như đăng nhập). Gửi lại OTP dùng
 * {@code POST /auth/otp-challenges/{id}/resend}.
 */
@RestController
@RequestMapping("/api/v1/auth/staff-activations")
public class StaffActivationController {

    private final StaffActivationService activations;
    private final RefreshCookies cookies;
    private final ApiResponseFactory responses;

    public StaffActivationController(StaffActivationService activations, RefreshCookies cookies,
                                     ApiResponseFactory responses) {
        this.activations = activations;
        this.cookies = cookies;
        this.responses = responses;
    }

    public record OtpRequest(@NotBlank @Size(max = 200) String invitationToken) {
    }

    public record ActivationRequest(@NotBlank @Size(max = 200) String invitationToken,
                                    @NotBlank @Size(max = 100) String challengeId,
                                    @NotBlank @Size(max = 10) String otp,
                                    @NotBlank @StrongPassword String password) {
    }

    @Operation(summary = "UC-ADM-01 — Nhân viên được mời xin mã OTP kích hoạt")
    @PostMapping("/otp-challenges")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> requestOtp(@Valid @RequestBody OtpRequest body) {
        return responses.accepted(OtpChallengeResponse.from(activations.requestOtp(body.invitationToken())));
    }

    @Operation(summary = "UC-ADM-01 — Nhân viên kích hoạt lời mời: xác minh OTP và đặt mật khẩu")
    @PostMapping
    public ResponseEntity<ApiResponse<AuthTokensResponse>> activate(@Valid @RequestBody ActivationRequest body,
                                                                    ClientInfo client) {
        AuthResult result = activations.activate(body.invitationToken(), body.challengeId(), body.otp(),
                body.password(), client);
        ResponseEntity<ApiResponse<AuthTokensResponse>> base =
                responses.of(HttpStatus.CREATED, AuthTokensResponse.of(result.session(), result.user()));
        return ResponseEntity.status(base.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, cookies.issue(result.session()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(base.getBody());
    }
}
