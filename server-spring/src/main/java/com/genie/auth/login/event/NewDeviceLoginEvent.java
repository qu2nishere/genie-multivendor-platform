package com.genie.auth.login.event;

import java.time.OffsetDateTime;

/**
 * Đăng nhập từ thiết bị chưa từng dùng (UC-AUTH-02 luồng 8a) — gửi email cảnh báo; module notification thêm thông báo
 * in-app {@code NEW_DEVICE_LOGIN} khi có.
 */
public record NewDeviceLoginEvent(Long userId, String email, String deviceName, String ipAddress, OffsetDateTime loginAt) {
}
