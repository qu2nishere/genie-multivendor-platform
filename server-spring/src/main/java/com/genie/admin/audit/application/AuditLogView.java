package com.genie.admin.audit.application;

import com.genie.common.audit.AuditCategory;
import java.time.OffsetDateTime;

/**
 * Một dòng màn Nhật ký hệ thống (API_SPEC 11.10).
 *
 * @param actorEmail {@code "system"} cho tác vụ hệ thống / định kỳ
 * @param actorRole  vai trò chụp lúc ghi; {@code STAFF} được thay bằng chức danh hiện tại (D4)
 * @param target     {@code details.target_label} → {@code reference_id} → {@code null} (D3)
 * @param result     {@code SUCCESS}, {@code FAILED}, {@code DENIED} (D5)
 */
public record AuditLogView(String id, OffsetDateTime createdAt, String actorEmail, String actorRole, String action,
                           AuditCategory category, String target, String ipAddress, String result) {
}
