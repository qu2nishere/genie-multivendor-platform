package com.genie.auth.otp.application;

/**
 * Kết quả gửi mã — đúng hình {@code $OtpChallenge} (API_SPEC 1):
 * {@code { challenge_id, purpose, email_masked, expires_in, resend_after }}.
 */
public record IssuedOtp(String challengeId, OtpPurpose purpose, String emailMasked, long expiresIn, long resendAfter) {
}
