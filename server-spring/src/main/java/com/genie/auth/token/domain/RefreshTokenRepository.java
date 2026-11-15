package com.genie.auth.token.domain;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Khóa dòng khi xoay vòng để hai request làm mới đồng thời không cùng dùng một token. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("hash") String tokenHash);

    /** Mã phiên mới (V14 tạo sequence {@code refresh_token_family_seq}). */
    @Query(value = "SELECT nextval('refresh_token_family_seq')", nativeQuery = true)
    Long nextFamilyId();

    /** Token còn hiệu lực của user, mới nhất trước — mỗi family lấy dòng đầu tiên gặp là phiên đang dùng. */
    @Query("select t from RefreshToken t where t.userId = :userId and t.revokedAt is null and t.expiresAt > :now "
            + "order by t.createdAt desc, t.id desc")
    List<RefreshToken> findActiveTokens(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    /** Lịch sử đăng nhập gần đây (kể cả đã thu hồi) — nhận diện thiết bị lạ. */
    List<RefreshToken> findTop50ByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, OffsetDateTime after);

    boolean existsByUserIdAndFamilyIdAndRevokedAtIsNull(Long userId, Long familyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokedReason = :reason "
            + "where t.userId = :userId and t.revokedAt is null")
    int revokeAllOfUser(@Param("userId") Long userId, @Param("reason") String reason, @Param("now") OffsetDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokedReason = :reason "
            + "where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") Long familyId, @Param("reason") String reason, @Param("now") OffsetDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now, t.revokedReason = :reason "
            + "where t.userId = :userId and t.familyId <> :keepFamilyId and t.revokedAt is null")
    int revokeOtherFamilies(@Param("userId") Long userId, @Param("keepFamilyId") Long keepFamilyId,
                            @Param("reason") String reason, @Param("now") OffsetDateTime now);

    /** Dọn token hết hạn / đã thu hồi trước {@code cutoff} (Cron). */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff or (t.revokedAt is not null and t.revokedAt < :cutoff)")
    int deleteStale(@Param("cutoff") OffsetDateTime cutoff);
}
