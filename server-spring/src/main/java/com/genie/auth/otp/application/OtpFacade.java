package com.genie.auth.otp.application;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Cửa công khai của OTP cho module khác (TS 4.4):
 * <ul>
 *   <li>{@link #verifyStepUp} — xác minh OTP bước 2 khi luồng không đi qua {@link RequireStepUp};</li>
 *   <li>{@link #issue} / {@link #verify} — module khác tự phát và xác minh OTP có kèm dữ liệu của luồng (vd. mở shop:
 *       dữ liệu form được giữ trong challenge tới khi nhập đúng mã — plan Mở shop & KYC K02.04). Giữ nguyên luật chung
 *       của {@link OtpChallengeService}: 60 giây / lần gửi, 5 lần / giờ, sai 5 lần hủy, mã sống 5 phút.</li>
 * </ul>
 * Chỉ mở cho mục đích thuộc module khác ({@link #FOREIGN_PURPOSES}) để module ngoài không thể phát / dùng mã của luồng
 * đăng nhập, quên mật khẩu, step-up…
 */
@Component
public class OtpFacade {

    /** Mục đích OTP do module khác sở hữu luồng. */
    public static final Set<OtpPurpose> FOREIGN_PURPOSES = Set.of(OtpPurpose.SHOP_REGISTRATION);

    private final OtpChallengeService otps;

    public OtpFacade(OtpChallengeService otps) {
        this.otps = otps;
    }

    /** Mã đúng → trả bình thường; sai / hết hạn / quá 5 lần → ném {@code BusinessException} GNE-AUTH-*. */
    public void verifyStepUp(long userId, OtpPurpose purpose, String challengeId, String code) {
        otps.verifyForUser(challengeId, code, purpose, userId);
    }

    /**
     * Gửi mã tới {@code email} cho người đang đăng nhập {@code userId}, giữ {@code payload} trong challenge (Redis, 5 phút).
     * Trả {@code $OtpChallenge}. Quá tần suất → {@code 429 GNE-RAT-4290}.
     */
    public IssuedOtp issue(OtpPurpose purpose, String email, long userId, Map<String, String> payload) {
        requireForeign(purpose);
        return otps.issue(purpose, email, userId, payload);
    }

    /**
     * Xác minh mã của challenge do chính {@code userId} xin với đúng {@code expectedPurpose}; đúng → trả {@code payload}
     * đã giữ (mã chỉ dùng được 1 lần). Sai → {@code 422 GNE-AUTH-4222}; sai lần 5 → {@code 422 GNE-AUTH-4221};
     * hết hạn / không thuộc người gọi → {@code 410 GNE-AUTH-4101}.
     */
    public Map<String, String> verify(String challengeId, String code, OtpPurpose expectedPurpose, long userId) {
        requireForeign(expectedPurpose);
        return otps.verifyForUser(challengeId, code, expectedPurpose, userId).data();
    }

    private static void requireForeign(OtpPurpose purpose) {
        if (!FOREIGN_PURPOSES.contains(purpose)) {
            throw new IllegalArgumentException("Mục đích OTP " + purpose + " không mở cho module khác");
        }
    }
}
