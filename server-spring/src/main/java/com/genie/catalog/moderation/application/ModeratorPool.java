// Cổng đảo ngược: module admin cho catalog biết ai là nhân viên kiểm duyệt sản phẩm (UC-CAT-13 bước 2, 7; plan C1 D15)
package com.genie.catalog.moderation.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * {@code admin_profiles} thuộc module {@code admin}; {@code admin/staff/application/ModeratorService} cài cổng này (admin →
 * catalog, không vòng phụ thuộc). Nhân viên kiểm duyệt = tài khoản STAFF đang ACTIVE có quyền {@code MANAGE_CATALOG}
 * (Admin không nằm trong danh sách — plan C1 D15, nhưng vẫn duyệt được).
 */
public interface ModeratorPool {

    /** @param accepting cờ "Đang nhận việc" ({@code admin_profiles.is_active_moderator}) */
    record Moderator(long staffId, String name, boolean accepting) {
    }

    /** Mọi nhân viên kiểm duyệt, xếp theo tên. */
    List<Moderator> moderators();

    /** Tên hiển thị của người duyệt (nhân viên hoặc Admin) — để hiện cột "Người duyệt". */
    Map<Long, String> names(Collection<Long> userIds);
}
