// Dòng nhân viên của API 11.18 GET /admin/staff (plan B1 D4, D5, mục 3.7)
package com.genie.admin.staff.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@code staffTitle}: {@code MODERATOR / CS_SPECIALIST / FINANCE_OFFICER / CUSTOM}, {@code null} với Admin.
 * {@code status} ∈ {@code StaffStatus}: {@code ACTIVE, LOCKED, PENDING_ACTIVATION, INVITATION_EXPIRED}.
 * {@code invitationExpiresAt} chỉ có khi đang chờ / lời mời hết hạn. {@code warnings} chỉ có sau mời / sửa quyền.
 */
public record StaffView(long id, String fullName, String email, String role, String staffTitle,
                        List<String> permissions, String status,
                        @JsonInclude(JsonInclude.Include.ALWAYS) OffsetDateTime invitationExpiresAt,
                        boolean isSelf,
                        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> warnings) {

    public StaffView withWarnings(List<String> w) {
        return new StaffView(id, fullName, email, role, staffTitle, permissions, status, invitationExpiresAt, isSelf, w);
    }
}
