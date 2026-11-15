package com.genie.auth.otp.api;

import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpPurpose;

/**
 * {@code $OtpChallenge} (API_SPEC 1): {@code { challenge_id, purpose, email_masked, expires_in, resend_after }}.
 */
public record OtpChallengeResponse(String challengeId, OtpPurpose purpose, String emailMasked, long expiresIn,
                                   long resendAfter) {

    public static OtpChallengeResponse from(IssuedOtp otp) {
        return new OtpChallengeResponse(otp.challengeId(), otp.purpose(), otp.emailMasked(), otp.expiresIn(),
                otp.resendAfter());
    }
}
