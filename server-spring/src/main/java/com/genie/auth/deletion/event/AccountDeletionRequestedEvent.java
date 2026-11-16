package com.genie.auth.deletion.event;

import java.time.OffsetDateTime;

/** Khách vừa xác nhận xóa tài khoản (UC-AUTH-10) — gửi email báo ngày ẩn danh và cách hủy. */
public record AccountDeletionRequestedEvent(Long userId, String email, OffsetDateTime requestedAt,
                                            OffsetDateTime anonymizeAt) {
}
