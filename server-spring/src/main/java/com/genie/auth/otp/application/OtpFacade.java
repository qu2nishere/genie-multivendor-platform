package com.genie.auth.otp.application;

import org.springframework.stereotype.Component;

/**
 * Cửa công khai của OTP cho module khác (TS 4.4): xác minh OTP bước 2 khi luồng không đi qua {@link RequireStepUp}.
 */
@Component
public class OtpFacade {

    private final OtpChallengeService otps;

    public OtpFacade(OtpChallengeService otps) {
        this.otps = otps;
    }

    /** Mã đúng → trả bình thường; sai / hết hạn / quá 5 lần → ném {@code BusinessException} GNE-AUTH-*. */
    public void verifyStepUp(long userId, OtpPurpose purpose, String challengeId, String code) {
        otps.verifyForUser(challengeId, code, purpose, userId);
    }
}
