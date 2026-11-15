package com.genie.auth.password.api;

import com.genie.auth.password.application.PasswordResetService;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.StrongPassword;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đặt mật khẩu mới sau khi xác minh OTP quên mật khẩu (API_SPEC 2.8, UC-AUTH-04).
 */
@RestController
@RequestMapping("/api/v1/auth/password-resets")
public class PasswordResetController {

    private final PasswordResetService passwordResets;

    public PasswordResetController(PasswordResetService passwordResets) {
        this.passwordResets = passwordResets;
    }

    public record ResetRequest(@NotBlank @Size(max = 200) String resetToken,
                               @NotBlank @StrongPassword String newPassword) {
    }

    @Operation(summary = "UC-AUTH-04 — Đặt mật khẩu mới bằng reset_token")
    @PostMapping
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest body, ClientInfo client) {
        passwordResets.reset(body.resetToken(), body.newPassword(), client);
        return ResponseEntity.noContent().build();
    }
}
