package com.genie.auth.otp.api;

import com.genie.auth.login.application.AuthResult;
import com.genie.auth.login.application.LoginOtpService;
import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpChallengeService;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.password.application.PasswordResetService;
import com.genie.auth.register.application.RegistrationService;
import com.genie.auth.token.api.AuthTokensResponse;
import com.genie.auth.token.api.RefreshCookies;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mã OTP của khách vãng lai (API_SPEC 2.2–2.4): gửi, gửi lại, xác minh — một màn OTP dùng chung cho đăng ký, đăng nhập
 * bằng mã, quên mật khẩu và mở khóa. Kết quả xác minh tùy {@code purpose} của challenge:
 * REGISTER → {@code 201 $AuthTokens}; LOGIN, ACCOUNT_UNLOCK → {@code 200 $AuthTokens}; PASSWORD_RESET → {@code 200 {reset_token, expires_in}}.
 */
@RestController
@RequestMapping("/api/v1/auth/otp-challenges")
public class AuthOtpController {

    private final OtpChallengeService otps;
    private final LoginOtpService loginOtps;
    private final PasswordResetService passwordResets;
    private final RegistrationService registrations;
    private final RefreshCookies cookies;
    private final ApiResponseFactory responses;

    public AuthOtpController(OtpChallengeService otps, LoginOtpService loginOtps, PasswordResetService passwordResets,
                             RegistrationService registrations, RefreshCookies cookies, ApiResponseFactory responses) {
        this.otps = otps;
        this.loginOtps = loginOtps;
        this.passwordResets = passwordResets;
        this.registrations = registrations;
        this.cookies = cookies;
        this.responses = responses;
    }

    public record SendRequest(@NotNull OtpPurpose purpose, @NotBlank @Email @Size(max = 255) String email) {
    }

    public record VerifyRequest(@NotBlank @Size(max = 10) String otp) {
    }

    @Operation(summary = "UC-AUTH-02 / UC-AUTH-04 — Gửi OTP đăng nhập, quên mật khẩu, mở khóa tài khoản")
    @PostMapping
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> send(@Valid @RequestBody SendRequest body) {
        IssuedOtp otp = switch (body.purpose()) {
            case LOGIN -> loginOtps.requestLogin(body.email());
            case PASSWORD_RESET -> passwordResets.request(body.email());
            case ACCOUNT_UNLOCK -> loginOtps.requestUnlock(body.email());
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mục đích OTP không hợp lệ.");
        };
        return responses.accepted(OtpChallengeResponse.from(otp));
    }

    @Operation(summary = "UC-AUTH-01 / 02 / 04 — Gửi lại OTP (sau 60 giây)")
    @PostMapping("/{challengeId}/resend")
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> resend(@PathVariable String challengeId) {
        otps.peekPurpose(challengeId)
                .filter(p -> p.isGuestRequestable() || p == OtpPurpose.REGISTER)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED));
        return responses.accepted(OtpChallengeResponse.from(otps.resend(challengeId)));
    }

    @Operation(summary = "UC-AUTH-01 / 02 / 04 — Xác minh OTP")
    @PostMapping("/{challengeId}/verify")
    public ResponseEntity<?> verify(@PathVariable String challengeId, @Valid @RequestBody VerifyRequest body,
                                    ClientInfo client) {
        OtpPurpose purpose = otps.peekPurpose(challengeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED));
        return switch (purpose) {
            case REGISTER -> tokens(HttpStatus.CREATED, registrations.verify(challengeId, body.otp(), client));
            case LOGIN -> tokens(HttpStatus.OK, loginOtps.verifyLogin(challengeId, body.otp(), client));
            case ACCOUNT_UNLOCK -> tokens(HttpStatus.OK, loginOtps.verifyUnlock(challengeId, body.otp(), client));
            case PASSWORD_RESET -> responses.ok(passwordResets.verify(challengeId, body.otp()));
            default -> throw new BusinessException(ErrorCode.OTP_EXPIRED);
        };
    }

    private ResponseEntity<ApiResponse<AuthTokensResponse>> tokens(HttpStatus status, AuthResult result) {
        ResponseEntity<ApiResponse<AuthTokensResponse>> base =
                responses.of(status, AuthTokensResponse.of(result.session(), result.user()));
        return ResponseEntity.status(base.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, cookies.issue(result.session()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(base.getBody());
    }
}
