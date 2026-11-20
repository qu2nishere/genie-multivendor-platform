package com.genie.admin.rbac.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffPermissionRepository extends JpaRepository<StaffPermission, Long> {

    /** Quyền còn hiệu lực của một nhân viên (revoked_at IS NULL) — nạp authority khi xác thực. */
    List<StaffPermission> findByStaffUserIdAndRevokedAtIsNull(Long staffUserId);

    /** Quyền còn hiệu lực của nhiều nhân viên — một truy vấn cho danh sách nhân viên (UC-ADM-01). */
    List<StaffPermission> findByStaffUserIdInAndRevokedAtIsNull(Collection<Long> staffUserIds);
}
