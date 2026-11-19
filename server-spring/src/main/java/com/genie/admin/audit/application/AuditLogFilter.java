package com.genie.admin.audit.application;

import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditCategory;
import com.genie.common.audit.SystemLog;
import com.genie.common.audit.SystemLogSpecs;
import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

/**
 * Bộ lọc đã kiểm hợp lệ của màn Nhật ký hệ thống — {@code from} bao gồm, {@code to} loại trừ.
 *
 * @param cursor {@code null} = trang đầu
 */
public record AuditLogFilter(OffsetDateTime from, OffsetDateTime to, AuditCategory category, String q,
                             AuditCursor cursor, int limit) {

    Specification<SystemLog> base() {
        Set<AuditAction> actions = category == null ? null : category.actions();
        return SystemLogSpecs.filter(from, to, actions, q);
    }
}
