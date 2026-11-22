package com.genie.admin.audit.application;

import com.genie.common.audit.AuditCategory;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Một dòng màn Nhật ký hệ thống (API_SPEC 11.10).
 *
 * @param actorEmail {@code "system"} cho tác vụ hệ thống / định kỳ
 * @param actorRole  vai trò chụp lúc ghi; {@code STAFF} được thay bằng chức danh hiện tại (D4)
 * @param target     {@code details.target_label} → {@code reference_id} → {@code null} (D3)
 * @param result     {@code SUCCESS}, {@code FAILED}, {@code DENIED} (D5)
 * @param referenceId cột {@code reference_id} gốc (có thể {@code null}) — hộp chi tiết (plan 2026-11-22 N4)
 * @param userAgent   cột {@code user_agent} gốc (có thể {@code null})
 * @param changes     {@code details.changes} dạng {@code {trường: {from, to}}}; {@code null} nếu không có
 */
public record AuditLogView(String id, OffsetDateTime createdAt, String actorEmail, String actorRole, String action,
                           AuditCategory category, String target, String ipAddress, String result,
                           String referenceId, String userAgent, Map<String, AuditChange> changes) {
}
