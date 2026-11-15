package com.genie.auth.password.event;

import com.genie.common.security.ClientInfo;
import java.time.OffsetDateTime;

/**
 * Mật khẩu vừa được đặt lại / đổi / thiết lập (UC-AUTH-04, UC-AUTH-06) — gửi email cảnh báo bảo mật.
 * {@code method} ∈ {@code RESET, CHANGE, SETUP}; {@code ipAddress}, {@code userAgent} của request thực hiện (có thể
 * {@code null}) để email cho người dùng biết thao tác đến từ thiết bị nào.
 */
public record PasswordChangedEvent(Long userId, String email, String method, OffsetDateTime changedAt,
                                   String ipAddress, String userAgent) {

    public static PasswordChangedEvent of(Long userId, String email, String method, OffsetDateTime changedAt,
                                          ClientInfo client) {
        return new PasswordChangedEvent(userId, email, method, changedAt,
                client == null ? null : client.ip(), client == null ? null : client.userAgent());
    }
}
