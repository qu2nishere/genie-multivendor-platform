package com.genie.auth.register.event;

import java.time.OffsetDateTime;

/**
 * Người mua vừa tạo tài khoản (UC-AUTH-01 / UC-AUTH-03). Module promotion có thể nghe để tặng voucher chào mừng.
 * {@code method} ∈ {@code EMAIL, GOOGLE, APPLE}.
 */
public record UserRegisteredEvent(Long userId, String method, OffsetDateTime registeredAt) {
}
