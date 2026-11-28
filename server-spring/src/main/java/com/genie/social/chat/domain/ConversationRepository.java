package com.genie.social.chat.domain;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Hội thoại khách ↔ shop (plan chat M03.03; API_SPEC 8.1–8.2). */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** Tạo nếu chưa có (cặp UNIQUE) — trả 1 khi vừa tạo, 0 khi đã có (hai luồng mở cùng lúc vẫn ra một dòng). */
    @Modifying
    @Query(value = """
            INSERT INTO conversations (customer_id, vendor_id, customer_unread_count, vendor_unread_count, created_at,
                                       updated_at)
            VALUES (:customerId, :vendorId, 0, 0, now(), now())
            ON CONFLICT (customer_id, vendor_id) DO NOTHING""", nativeQuery = true)
    int insertIgnore(@Param("customerId") long customerId, @Param("vendorId") long vendorId);

    Optional<Conversation> findByCustomerIdAndVendorId(Long customerId, Long vendorId);

    /** Khóa dòng khi ghi tin / đánh dấu đọc để bộ đếm đúng khi hai bên thao tác cùng lúc (N3). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conversation c WHERE c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") long id);

    // ===== Danh sách theo phía (D16): chỉ hội thoại đã có tin, mới nhất trước =====

    @Query("""
            SELECT c FROM Conversation c WHERE c.customerId = :uid AND c.lastMessageAt IS NOT NULL
            ORDER BY c.lastMessageAt DESC, c.id DESC""")
    List<Conversation> pageForCustomer(@Param("uid") long uid, Pageable page);

    @Query("""
            SELECT c FROM Conversation c WHERE c.customerId = :uid AND c.lastMessageAt IS NOT NULL
              AND (c.lastMessageAt < :t OR (c.lastMessageAt = :t AND c.id < :id))
            ORDER BY c.lastMessageAt DESC, c.id DESC""")
    List<Conversation> pageForCustomerAfter(@Param("uid") long uid, @Param("t") OffsetDateTime t,
                                            @Param("id") long id, Pageable page);

    @Query("""
            SELECT c FROM Conversation c WHERE c.vendorId = :uid AND c.lastMessageAt IS NOT NULL
            ORDER BY c.lastMessageAt DESC, c.id DESC""")
    List<Conversation> pageForVendor(@Param("uid") long uid, Pageable page);

    @Query("""
            SELECT c FROM Conversation c WHERE c.vendorId = :uid AND c.lastMessageAt IS NOT NULL
              AND (c.lastMessageAt < :t OR (c.lastMessageAt = :t AND c.id < :id))
            ORDER BY c.lastMessageAt DESC, c.id DESC""")
    List<Conversation> pageForVendorAfter(@Param("uid") long uid, @Param("t") OffsetDateTime t,
                                          @Param("id") long id, Pageable page);

    /** Badge nút chat (khách) — tổng chưa đọc phía khách. */
    @Query("SELECT COALESCE(SUM(c.customerUnread), 0) FROM Conversation c WHERE c.customerId = :uid")
    long sumUnreadForCustomer(@Param("uid") long uid);

    /** Badge nav Chat (Seller Center) — tổng chưa đọc phía shop. */
    @Query("SELECT COALESCE(SUM(c.vendorUnread), 0) FROM Conversation c WHERE c.vendorId = :uid")
    long sumUnreadForVendor(@Param("uid") long uid);
}
