package com.genie.order.cart.domain;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    /** Tạo giỏ nếu chưa có ({@code carts.user_id} UNIQUE) — hai yêu cầu đồng thời không tạo hai giỏ (plan C2 3.1). */
    @Modifying
    @Query(value = "INSERT INTO carts (user_id, updated_at) VALUES (:userId, :now) ON CONFLICT (user_id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId, @Param("now") OffsetDateTime now);

    @Query(value = "SELECT id FROM carts WHERE user_id = :userId", nativeQuery = true)
    Optional<Long> findIdByUserId(@Param("userId") long userId);

    @Modifying
    @Query(value = "UPDATE carts SET updated_at = :now WHERE id = :cartId", nativeQuery = true)
    int touch(@Param("cartId") long cartId, @Param("now") OffsetDateTime now);
}
