package com.genie.promotion.coin.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCoinRepository extends JpaRepository<UserCoin, Long> {

    /** Khóa dòng ví Điểm ({@code SELECT … FOR UPDATE}) trước khi cộng / trừ — TS 10.3. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM UserCoin c WHERE c.userId = :userId")
    Optional<UserCoin> findByIdForUpdate(@Param("userId") Long userId);

    /** Khóa ví Điểm cho cron, BỎ QUA nếu đang bị khóa ({@code FOR UPDATE SKIP LOCKED} — TS 10.3). */
    @Query(value = "SELECT * FROM user_coins WHERE user_id = :userId FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<UserCoin> lockSkipLocked(@Param("userId") Long userId);
}
