package com.genie.admin.staff.domain;

import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {

    Optional<AdminProfile> findByUserId(Long userId);

    List<AdminProfile> findAllByUserIdIn(Collection<Long> userIds);

    /** Khóa hồ sơ khi đổi chức danh / quyền (UC-ADM-02) — hai Admin sửa cùng lúc không ghi đè nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AdminProfile p where p.userId = :userId")
    Optional<AdminProfile> findByUserIdForUpdate(@Param("userId") Long userId);

    /** Tra chức danh nhiều nhân viên một lần (màn Nhật ký hệ thống — tránh N+1). */
    List<AdminProfile> findByUserIdIn(Collection<Long> userIds);
}
