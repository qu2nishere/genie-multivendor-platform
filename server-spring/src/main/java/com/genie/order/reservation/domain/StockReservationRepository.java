package com.genie.order.reservation.domain;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByOrderIdInOrderByProductVariantIdAsc(Collection<Long> orderIds);

    /**
     * Đóng một lượt giữ chỗ CÒN {@code ACTIVE} (cron UC-ORD-08 / hủy nhóm đơn) — có điều kiện để không đua với IPN (C3)
     * đã chốt trước: 0 dòng = lượt này không còn ACTIVE, KHÔNG được nhả tồn (plan C2 D13).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE stock_reservations SET status = :status, released_at = :now, version = version + 1
            WHERE id = :id AND status = 'ACTIVE'""", nativeQuery = true)
    int closeIfActive(@Param("id") long id, @Param("status") String status, @Param("now") OffsetDateTime now);
}
