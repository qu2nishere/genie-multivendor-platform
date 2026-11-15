package com.genie.auth.register.api;

import com.genie.auth.otp.api.OtpChallengeResponse;
import com.genie.auth.register.application.RegistrationService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.StrongPassword;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đăng ký người mua (API_SPEC 2.1). Xác minh OTP đi qua {@code POST /auth/otp-challenges/{id}/verify}.
 */
@RestController
@RequestMapping("/api/v1/auth/registrations")
public class RegistrationController {

    private final RegistrationService registrations;
    private final ApiResponseFactory responses;

    public RegistrationController(RegistrationService registrations, ApiResponseFactory responses) {
        this.registrations = registrations;
        this.responses = responses;
    }

    public record RegistrationRequest(
            @NotBlank @Size(max = 255) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại phải gồm 10–11 chữ số, bắt đầu bằng 0.") String phoneNumber,
            @NotBlank @StrongPassword String password,
            @AssertTrue(message = "Cần đồng ý Điều khoản sử dụng.") Boolean acceptTerms) {
    }

    @Operation(summary = "UC-AUTH-01 — Đăng ký tài khoản người mua, gửi OTP xác minh email")
    @PostMapping
    public ResponseEntity<ApiResponse<OtpChallengeResponse>> register(@Valid @RequestBody RegistrationRequest body) {
        var otp = registrations.register(body.fullName(), body.email(), body.phoneNumber(), body.password(),
                LocaleContextHolder.getLocale().getLanguage());
        return responses.accepted(OtpChallengeResponse.from(otp));
    }
}
