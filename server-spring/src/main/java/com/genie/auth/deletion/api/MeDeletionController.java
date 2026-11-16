package com.genie.auth.deletion.api;

import com.genie.auth.deletion.application.AccountDeletionService;
import com.genie.auth.deletion.application.DeletionRequestView;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.otp.application.RequireStepUp;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xóa tài khoản (API_SPEC 3.7, UC-AUTH-10). Luồng: {@code POST /me/otp-challenges {purpose: ACCOUNT_DELETION}}
 * (còn đơn đang xử lý → {@code 409 GNE-USR-4091} trước khi gửi mã) → nhập mã → {@code PUT /me/deletion-request}.
 */
@RestController
@RequestMapping("/api/v1/me/deletion-request")
public class MeDeletionController {

    private final AccountDeletionService deletions;
    private final ApiResponseFactory responses;

    public MeDeletionController(AccountDeletionService deletions, ApiResponseFactory responses) {
        this.deletions = deletions;
        this.responses = responses;
    }

    @Operation(summary = "UC-AUTH-10 — Yêu cầu xóa tài khoản (cần OTP bước 2 ACCOUNT_DELETION); ẩn danh sau 30 ngày")
    @PutMapping
    @RequireStepUp(OtpPurpose.ACCOUNT_DELETION)
    public ResponseEntity<ApiResponse<DeletionRequestView>> request(@CurrentUser AuthUser me, ClientInfo client) {
        return responses.ok(deletions.request(me, client));
    }

    @Operation(summary = "UC-AUTH-10 — Hủy yêu cầu xóa tài khoản (trong 30 ngày)")
    @DeleteMapping
    public ResponseEntity<Void> cancel(@CurrentUser AuthUser me, ClientInfo client) {
        deletions.cancel(me, client);
        return ResponseEntity.noContent().build();
    }
}
