package com.genie.promotion.coin.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyCheckinRepository extends JpaRepository<DailyCheckin, Long> {

    Optional<DailyCheckin> findByCustomerId(Long customerId);

    /** Khóa dòng điểm danh của khách — chống 2 lần điểm danh cùng lúc (UC-PRO-08). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyCheckin d WHERE d.customerId = :customerId")
    Optional<DailyCheckin> findByCustomerIdForUpdate(@Param("customerId") Long customerId);
}
