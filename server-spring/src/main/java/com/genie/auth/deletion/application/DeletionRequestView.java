package com.genie.auth.deletion.application;

import java.time.OffsetDateTime;

/** Yêu cầu xóa tài khoản đang chờ — API_SPEC 3.7: {@code { requested_at, anonymize_at }}. */
public record DeletionRequestView(OffsetDateTime requestedAt, OffsetDateTime anonymizeAt) {
}
