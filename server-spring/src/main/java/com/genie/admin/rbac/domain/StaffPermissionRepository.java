package com.genie.admin.rbac.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffPermissionRepository extends JpaRepository<StaffPermission, Long> {
}
