package com.genie.catalog.wishlist.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Sản phẩm yêu thích (UC-CAT-08; plan wishlist W01.03). Ghi / xóa idempotent theo khóa (user_id, product_id). */
public interface UserWishlistRepository extends JpaRepository<UserWishlist, Long> {

    /** Thích — chèn nếu chưa có ({@code ON CONFLICT DO NOTHING}); trả 1 nếu vừa thêm, 0 nếu đã có. */
    @Modifying
    @Query(value = "INSERT INTO user_wishlists (user_id, product_id, created_at) VALUES (:userId, :productId, :now) "
            + "ON CONFLICT (user_id, product_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId, @Param("productId") long productId,
                       @Param("now") OffsetDateTime now);

    /** Bỏ thích — trả số dòng đã xóa (0 khi vốn không có). */
    @Modifying
    @Query("DELETE FROM UserWishlist w WHERE w.userId = :userId AND w.productId = :productId")
    int deleteByUserIdAndProductId(@Param("userId") long userId, @Param("productId") long productId);

    /** ID sản phẩm đã thích, mới nhất trước. */
    @Query("SELECT w.productId FROM UserWishlist w WHERE w.userId = :userId ORDER BY w.createdAt DESC, w.id DESC")
    List<Long> findProductIds(@Param("userId") long userId, Pageable page);

    long countByUserId(Long userId);

    @Query("SELECT w FROM UserWishlist w WHERE w.userId = :userId ORDER BY w.createdAt DESC, w.id DESC")
    List<UserWishlist> findPage(@Param("userId") long userId, Pageable page);

    @Query("""
            SELECT w FROM UserWishlist w WHERE w.userId = :userId
              AND (w.createdAt < :createdAt OR (w.createdAt = :createdAt AND w.id < :id))
            ORDER BY w.createdAt DESC, w.id DESC""")
    List<UserWishlist> findPageAfter(@Param("userId") long userId, @Param("createdAt") OffsetDateTime createdAt,
                                     @Param("id") long id, Pageable page);

    /** Ai đang thích một sản phẩm (D6 — thông báo giảm giá sau này). */
    @Query("SELECT w.userId FROM UserWishlist w WHERE w.productId = :productId")
    List<Long> findUserIdsByProductId(@Param("productId") long productId);
}
