package com.genie.vendor.shop.api;

import com.genie.auth.otp.application.IssuedOtp;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.vendor.shop.application.ShopRegistrationService;
import com.genie.vendor.shop.domain.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mở shop — UC-VND-01 (API_SPEC 9.1–9.2). Người gọi lấy từ token; sau 9.2 client làm mới token để có quyền {@code /seller}. */
@RestController
@RequestMapping("/api/v1/me/shop-registrations")
public class ShopRegistrationController {

    private final ShopRegistrationService registrations;
    private final ApiResponseFactory responses;

    public ShopRegistrationController(ShopRegistrationService registrations, ApiResponseFactory responses) {
        this.registrations = registrations;
        this.responses = responses;
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 100) String shopName,
            @NotBlank @Size(min = 2, max = 255) String ownerName,
            @NotBlank @Pattern(regexp = "^0\\d{9,10}$") String phoneNumber,
            @NotBlank @Email @Size(max = 255) String email,
            @NotNull @Positive Long mainCategoryId,
            @NotNull BusinessType businessType,
            @NotNull @AssertTrue Boolean acceptTerms) {
    }

    public record ConfirmRequest(@NotBlank @Size(max = 10) String otp) {
    }

    /** {@code $OtpChallenge} (API_SPEC 1). */
    public record OtpChallengeView(String challengeId, String purpose, String emailMasked, long expiresIn,
                                   long resendAfter) {
        static OtpChallengeView of(IssuedOtp o) {
            return new OtpChallengeView(o.challengeId(), o.purpose().name(), o.emailMasked(), o.expiresIn(), o.resendAfter());
        }
    }

    @Operation(summary = "UC-VND-01 — Gửi đăng ký mở shop (OTP tới email của shop)")
    @PostMapping
    public ResponseEntity<ApiResponse<OtpChallengeView>> register(@CurrentUser AuthUser me,
                                                                  @Valid @RequestBody RegisterRequest body) {
        IssuedOtp otp = registrations.register(me.id(), new ShopRegistrationService.Registration(body.shopName(),
                body.ownerName(), body.phoneNumber(), body.email(), body.mainCategoryId(), body.businessType()));
        return responses.accepted(OtpChallengeView.of(otp));
    }

    @Operation(summary = "UC-VND-01 — Xác nhận OTP, tạo shop và nâng tài khoản lên VENDOR")
    @PostMapping("/{challengeId}/confirm")
    public ResponseEntity<ApiResponse<ShopRegistrationService.Registered>> confirm(
            @CurrentUser AuthUser me, @PathVariable String challengeId, @Valid @RequestBody ConfirmRequest body) {
        return responses.created(registrations.confirm(me.id(), challengeId, body.otp()));
    }
}
