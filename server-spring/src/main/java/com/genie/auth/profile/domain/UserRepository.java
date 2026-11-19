package com.genie.auth.profile.domain;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /** {@code email} phải đã chuẩn hóa bằng {@link User#normalizeEmail(String)}. */
    Optional<User> findByEmail(String email);

    /** Trong số các id đưa vào, id nào đang ACTIVE — một truy vấn (module khác lọc danh sách — plan theo dõi gian hàng). */
    @Query("select u.id from User u where u.id in :ids and u.status = com.genie.auth.profile.domain.AccountStatus.ACTIVE")
    List<Long> findActiveIds(@Param("ids") java.util.Collection<Long> ids);

    boolean existsByEmail(String email);

    Optional<User> findByOauthProviderAndOauthProviderId(String oauthProvider, String oauthProviderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    /** Tài khoản đã yêu cầu xóa trước {@code cutoff}, chưa ẩn danh — Cron UC-AUTH-10 (an toàn khi chạy nhiều instance). */
    @Query(value = "SELECT * FROM users WHERE deletion_requested_at IS NOT NULL AND deletion_requested_at < :cutoff "
            + "AND status NOT IN ('ANONYMIZED', 'DELETED') ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<User> findDueForAnonymization(@Param("cutoff") OffsetDateTime cutoff, @Param("limit") int limit);
}
