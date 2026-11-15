package com.genie.auth.otp.application;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu endpoint cần OTP bước 2 🔐 (API_SPEC 0.7): request phải kèm {@code X-OTP-Challenge-Id} + {@code X-OTP-Code}
 * của challenge cùng {@link #value() mục đích}, do chính người đang đăng nhập xin qua {@code POST /me/otp-challenges}.
 * Đặt ở tầng application để module khác (payment — rút tiền, vendor — tài khoản nhận tiền) dùng được mà không chạm
 * {@code auth..api} (ArchUnit). Kiểm tra do {@code auth.otp.api.StepUpAspect} thực hiện.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireStepUp {

    OtpPurpose value();
}
