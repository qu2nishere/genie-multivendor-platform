package com.genie.order.checkout.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, Long> {

    /** Id phiên còn mở đã quá hạn, cũ trước (UC-ORD-08 — index {@code idx_checkout_sessions_open_expires}). */
    @Query(value = """
            SELECT id FROM checkout_sessions
            WHERE status IN ('INITIATED', 'PAYMENT_PENDING') AND expires_at <= :now
            ORDER BY expires_at, id LIMIT :limit""", nativeQuery = true)
    List<Long> findExpiredIds(@Param("now") OffsetDateTime now, @Param("limit") int limit);

    /**
     * Khóa một phiên còn mở + quá hạn để hủy; đang bị luồng khác khóa (cron instance khác / IPN) → rỗng, để lượt sau.
     */
    @Query(value = """
            SELECT * FROM checkout_sessions
            WHERE id = :id AND status IN ('INITIATED', 'PAYMENT_PENDING') AND expires_at <= :now
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    Optional<CheckoutSession> lockIfExpired(@Param("id") long id, @Param("now") OffsetDateTime now);
}
