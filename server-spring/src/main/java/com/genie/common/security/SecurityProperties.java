package com.genie.common.security;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình {@code genie.security.*} (application.yml; bí mật từ biến môi trường — TECH_STACK 16.6, plan AUTH A04.08).
 *
 * @param jwtSecret       khóa HS256, ≥ 32 byte (kiểm khi khởi động ở JwtService)
 * @param accessTokenTtl  hạn access token (15 phút — NFR-05)
 * @param refreshTokenTtl hạn refresh token (7 ngày)
 * @param cookieSecure    cờ {@code Secure} của cookie {@code genie_rt}; false khi chạy http://localhost
 * @param corsOrigins     origin web được gọi API kèm cookie
 * @param trustedProxies  IP / CIDR của reverse proxy được tin header {@code X-Forwarded-For}
 * @param otpPepper       chuỗi bí mật trộn vào bản băm OTP / token tạm lưu Redis
 * @param internalToken   token nội bộ AI Worker gọi {@code /api/v1/internal/**}
 */
@ConfigurationProperties(prefix = "genie.security")
public record SecurityProperties(
        String jwtSecret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("7d") Duration refreshTokenTtl,
        @DefaultValue("true") boolean cookieSecure,
        @DefaultValue("http://localhost:4321") List<String> corsOrigins,
        @DefaultValue({"127.0.0.1", "::1", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"}) List<String> trustedProxies,
        String otpPepper,
        String internalToken) {
}
