package com.genie.auth.otp.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.Map;

/**
 * Nhập sai mã lần thứ 5 → challenge đã bị hủy ({@code 422 GNE-AUTH-4221}). Mang theo challenge để luồng gọi xử lý hệ quả
 * riêng (khóa đăng nhập 15 phút, khóa khôi phục 30 phút, hủy đăng ký) rồi ném lại kèm {@code details}.
 */
public class OtpExhaustedException extends BusinessException {

    private final transient OtpChallenge challenge;

    public OtpExhaustedException(OtpChallenge challenge) {
        super(ErrorCode.OTP_TOO_MANY_ATTEMPTS, Map.of("remaining_attempts", 0));
        this.challenge = challenge;
    }

    public OtpChallenge getChallenge() {
        return challenge;
    }
}
