package com.genie.auth.deletion.event;

import java.time.OffsetDateTime;

/**
 * Tài khoản vừa được ẩn danh sau 30 ngày (UC-AUTH-10). Sau commit: xóa ảnh đại diện trên Silo; module khác
 * (social, chat…) nghe sự kiện này để hiện "Người dùng đã xóa".
 * {@code avatarUrl}: ảnh đại diện trước khi ẩn danh (có thể {@code null}).
 */
public record AccountAnonymizedEvent(Long userId, String avatarUrl, OffsetDateTime anonymizedAt) {
}
