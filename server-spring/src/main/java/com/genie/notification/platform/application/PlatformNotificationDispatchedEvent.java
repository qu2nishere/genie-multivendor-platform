// Thông báo sàn vừa được phát (ngay khi tạo / sửa, hoặc cron hẹn giờ) — người nghe đẩy realtime sau commit (plan 2026-11-26 thông báo N06.04, N07.01)
package com.genie.notification.platform.application;

/** Sự kiện nội bộ module {@code notification} — chỉ chứa id (TS 4.4). */
public record PlatformNotificationDispatchedEvent(long platformNotificationId) {
}
