package com.genie.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpFacade;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.common.error.BusinessException;
import com.genie.support.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** OTP cho module khác — phát kèm dữ liệu và xác minh (plan Mở shop & KYC K02.04). */
class OtpFacadeIT extends IntegrationTestBase {

    @Autowired
    OtpFacade otpFacade;

    @Test
    void issuesWithPayloadAndReturnsItOnceOnCorrectCode() {
        long userId = createCustomer(uniqueEmail(), PASSWORD).getId();
        IssuedOtp otp = otpFacade.issue(OtpPurpose.SHOP_REGISTRATION, "shop@techzone.vn", userId,
                Map.of("shop_name", "TechZone"));
        assertThat(otp.purpose()).isEqualTo(OtpPurpose.SHOP_REGISTRATION);
        assertThat(otp.emailMasked()).isEqualTo("sh****@techzone.vn");
        assertThat(otp.resendAfter()).isEqualTo(60);
        String code = mails.lastOtp("shop@techzone.vn");

        Map<String, String> payload = otpFacade.verify(otp.challengeId(), code, OtpPurpose.SHOP_REGISTRATION, userId);
        assertThat(payload).containsEntry("shop_name", "TechZone");
        // mã chỉ dùng được 1 lần
        assertThatThrownBy(() -> otpFacade.verify(otp.challengeId(), code, OtpPurpose.SHOP_REGISTRATION, userId))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode",
                        com.genie.common.error.ErrorCode.OTP_EXPIRED);
    }

    @Test
    void rejectsOtherUsersWrongCodesAndAuthPurposes() {
        long userId = createCustomer(uniqueEmail(), PASSWORD).getId();
        long other = createCustomer(uniqueEmail(), PASSWORD).getId();
        IssuedOtp otp = otpFacade.issue(OtpPurpose.SHOP_REGISTRATION, "a@shop.vn", userId, Map.of());
        String code = mails.lastOtp("a@shop.vn");
        assertThatThrownBy(() -> otpFacade.verify(otp.challengeId(), code, OtpPurpose.SHOP_REGISTRATION, other))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> otpFacade.verify(otp.challengeId(), "000000".equals(code) ? "111111" : "000000",
                OtpPurpose.SHOP_REGISTRATION, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.genie.common.error.ErrorCode.OTP_INVALID);
        assertThatThrownBy(() -> otpFacade.issue(OtpPurpose.LOGIN, "a@shop.vn", userId, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> otpFacade.verify(otp.challengeId(), code, OtpPurpose.PASSWORD_RESET, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
