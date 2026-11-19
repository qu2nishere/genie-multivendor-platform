package com.genie.vendor.settings.api;

import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.otp.application.RequireStepUp;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import com.genie.vendor.settings.application.PayoutAccountService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tài khoản nhận tiền — UC-VND-07 (API_SPEC 10.11). Lưu / xóa cần OTP bước 2 (API 0.7). */
@RestController
@RequestMapping("/api/v1/seller/payout-account")
public class SellerPayoutAccountController {

    private final PayoutAccountService payouts;
    private final ApiResponseFactory responses;

    public SellerPayoutAccountController(PayoutAccountService payouts, ApiResponseFactory responses) {
        this.payouts = payouts;
        this.responses = responses;
    }

    public record AccountRequest(@NotBlank @Size(max = 20) String bankCode,
                                 @NotBlank @Pattern(regexp = "^\\d{6,20}$") String accountNumber,
                                 @NotBlank @Size(max = 255) String accountHolder) {
        PayoutAccountService.AccountInput toInput() {
            return new PayoutAccountService.AccountInput(bankCode, accountNumber, accountHolder);
        }
    }

    @Operation(summary = "UC-VND-07 — Tài khoản nhận tiền hiện tại (số tài khoản đã che; null nếu chưa có)")
    @GetMapping
    public ResponseEntity<ApiResponse<PayoutAccountService.PayoutAccountView>> current(@CurrentUser AuthUser me) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(payouts.current(me.id())).getBody());
    }

    @Operation(summary = "UC-VND-07 — Xác minh chính chủ tài khoản nhận tiền")
    @PostMapping("/verification")
    public ResponseEntity<ApiResponse<PayoutAccountService.Verification>> verify(@CurrentUser AuthUser me,
                                                                                @Valid @RequestBody AccountRequest body) {
        return responses.ok(payouts.verify(me.id(), body.toInput()));
    }

    @Operation(summary = "UC-VND-07 — Lưu tài khoản nhận tiền (OTP bước 2 PAYOUT_ACCOUNT_UPDATE)")
    @PutMapping
    @RequireStepUp(OtpPurpose.PAYOUT_ACCOUNT_UPDATE)
    public ResponseEntity<ApiResponse<PayoutAccountService.PayoutAccountView>> save(@CurrentUser AuthUser me,
                                                                                   @Valid @RequestBody AccountRequest body,
                                                                                   ClientInfo client) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.ok(payouts.save(me.id(), body.toInput(), client)).getBody());
    }

    @Operation(summary = "UC-VND-07 — Xóa tài khoản nhận tiền (OTP bước 2 PAYOUT_ACCOUNT_DELETE)")
    @DeleteMapping
    @RequireStepUp(OtpPurpose.PAYOUT_ACCOUNT_DELETE)
    public ResponseEntity<Void> delete(@CurrentUser AuthUser me, ClientInfo client) {
        payouts.delete(me.id(), client);
        return ResponseEntity.noContent().build();
    }
}
