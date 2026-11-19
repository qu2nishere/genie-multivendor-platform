package com.genie.social.follow.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Theo dõi shop (UC-SOC-06; plan theo dõi gian hàng F01.05). Ghi / xóa idempotent theo (customer_id, vendor_id). */
public interface ShopFollowRepository extends JpaRepository<ShopFollow, Long> {

    /** Theo dõi — chèn nếu chưa có; trả 1 nếu vừa thêm (lúc đó mới cộng follower_count), 0 nếu đã có. */
    @Modifying
    @Query(value = "INSERT INTO shop_follows (customer_id, vendor_id, created_at) VALUES (:customerId, :vendorId, :now) "
            + "ON CONFLICT (customer_id, vendor_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("customerId") long customerId, @Param("vendorId") long vendorId,
                       @Param("now") OffsetDateTime now);

    /** Hủy theo dõi — trả số dòng đã xóa (1 thì mới trừ follower_count). */
    @Modifying
    @Query("DELETE FROM ShopFollow f WHERE f.customerId = :customerId AND f.vendorId = :vendorId")
    int deleteByCustomerIdAndVendorId(@Param("customerId") long customerId, @Param("vendorId") long vendorId);

    /** {@code users.id} chủ shop đang theo dõi, mới nhất trước. */
    @Query("SELECT f.vendorId FROM ShopFollow f WHERE f.customerId = :customerId ORDER BY f.createdAt DESC, f.id DESC")
    List<Long> findVendorIdsByCustomerId(@Param("customerId") long customerId, Pageable page);

    long countByCustomerId(Long customerId);

    @Query("SELECT f FROM ShopFollow f WHERE f.customerId = :customerId ORDER BY f.createdAt DESC, f.id DESC")
    List<ShopFollow> findPage(@Param("customerId") long customerId, Pageable page);

    @Query("""
            SELECT f FROM ShopFollow f WHERE f.customerId = :customerId
              AND (f.createdAt < :createdAt OR (f.createdAt = :createdAt AND f.id < :id))
            ORDER BY f.createdAt DESC, f.id DESC""")
    List<ShopFollow> findPageAfter(@Param("customerId") long customerId, @Param("createdAt") OffsetDateTime createdAt,
                                   @Param("id") long id, Pageable page);

    /** Người theo dõi một shop (D6 — thông báo voucher / Flash Sale sau này). */
    @Query("SELECT f.customerId FROM ShopFollow f WHERE f.vendorId = :vendorId")
    List<Long> findCustomerIdsByVendorId(@Param("vendorId") long vendorId);
}
