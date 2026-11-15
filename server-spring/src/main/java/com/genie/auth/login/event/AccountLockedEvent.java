package com.genie.auth.login.event;

import java.time.OffsetDateTime;

/**
 * Tài khoản vừa bị khóa tạm (sai mật khẩu / OTP 5 lần — UC-AUTH-02). {@code lockedBy} ∈ {@code PASSWORD, OTP}.
 */
public record AccountLockedEvent(Long userId, String lockedBy, OffsetDateTime lockedUntil) {
}
