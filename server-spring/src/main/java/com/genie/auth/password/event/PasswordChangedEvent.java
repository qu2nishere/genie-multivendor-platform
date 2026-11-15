package com.genie.auth.password.event;

import java.time.OffsetDateTime;

/**
 * Mật khẩu vừa được đặt lại / đổi / thiết lập (UC-AUTH-04, UC-AUTH-06) — gửi email cảnh báo bảo mật.
 * {@code method} ∈ {@code RESET, CHANGE, SETUP}.
 */
public record PasswordChangedEvent(Long userId, String email, String method, OffsetDateTime changedAt) {
}
