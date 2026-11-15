package com.genie.auth.password.api;

import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.otp.application.RequireStepUp;
import com.genie.auth.password.application.PasswordChangeService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import com.genie.common.security.StrongPassword;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đổi / thiết lập mật khẩu (API_SPEC 3.4, UC-AUTH-06) — dùng chung cho Tài khoản, Seller › Bảo mật, Admin › Tài khoản.
 */
@RestController
@RequestMapping("/api/v1/me/password")
public class MePasswordController {

    private final PasswordChangeService passwords;
    private final ApiResponseFactory responses;

    public MePasswordController(PasswordChangeService passwords, ApiResponseFactory responses) {
        this.passwords = passwords;
        this.responses = responses;
    }

    public record VerifyRequest(@NotBlank @Size(max = 128) String password) {
    }

    public record VerifyResponse(boolean valid) {
    }

    public record ChangeRequest(@Size(max = 128) String currentPassword, @NotBlank @StrongPassword String newPassword) {
    }

    @Operation(summary = "UC-AUTH-06 — Kiểm mật khẩu hiện tại khi đang gõ (10 lần / phút)")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyResponse>> verify(@CurrentUser AuthUser me,
                                                              @Valid @RequestBody VerifyRequest body) {
        return responses.ok(new VerifyResponse(passwords.verifyCurrent(me.id(), body.password())));
    }

    @Operation(summary = "UC-AUTH-06 — Đổi / thiết lập mật khẩu (cần OTP bước 2 PASSWORD_CHANGE)")
    @PutMapping
    @RequireStepUp(OtpPurpose.PASSWORD_CHANGE)
    public ResponseEntity<Void> change(@CurrentUser AuthUser me, @Valid @RequestBody ChangeRequest body,
                                       ClientInfo client) {
        passwords.change(me, body.currentPassword(), body.newPassword(), client);
        return ResponseEntity.noContent().build();
    }
}
