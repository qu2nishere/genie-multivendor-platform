package com.genie.admin.staff.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {

    Optional<AdminProfile> findByUserId(Long userId);

    /** Tra chức danh nhiều nhân viên một lần (màn Nhật ký hệ thống — tránh N+1). */
    List<AdminProfile> findByUserIdIn(Collection<Long> userIds);
}
