package com.genie.auth.profile.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUserId(Long userId);

    /** Khóa hồ sơ khi đổi địa chỉ mặc định — hai request song song không tạo ra hai địa chỉ mặc định. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CustomerProfile p where p.userId = :userId")
    Optional<CustomerProfile> findByUserIdForUpdate(@Param("userId") Long userId);
}
