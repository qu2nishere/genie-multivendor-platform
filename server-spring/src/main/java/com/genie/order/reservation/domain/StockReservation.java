package com.genie.order.reservation.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * StockReservation — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "stock_reservations")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class StockReservation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — giữ chỗ cho biến thể nào

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — giữ cho đơn hàng nào

    @Column(nullable = false)
    private Integer reservedQuantity;         // Số lượng đang giữ

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockReservationStatus status;    // ACTIVE (đang giữ) / CONFIRMED (đã chốt) / EXPIRED / RELEASED

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống Sweeper và Webhook cùng đổi trạng thái 1 lượt giữ chỗ

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Hạn giữ chỗ — quá hạn sẽ tự nhả

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bắt đầu giữ

    private OffsetDateTime releasedAt;         // NULLABLE — Thời điểm thực sự nhả chỗ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đã quá hạn giữ chỗ — UC-ORD-08 */
    public boolean isExpired(OffsetDateTime now) {
        return this.status == StockReservationStatus.ACTIVE && this.expiresAt.isBefore(now);
    }

    /** Chốt khi thanh toán thành công — UC-PAY-03 */
    public void confirm() {
        if (this.status != StockReservationStatus.ACTIVE) throw new IllegalStateException("GNE-INV-4093: giữ chỗ đã hết hạn");
        this.status = StockReservationStatus.CONFIRMED;   // @Version: IPN và Sweeper chỉ một bên thắng
    }

    /** Nhả giữ chỗ (idempotent) — UC-ORD-08 */
    public boolean release(OffsetDateTime now, boolean expired) {
        if (this.status != StockReservationStatus.ACTIVE) return false;   // đã CONFIRMED/RELEASED → không nhả lần 2
        this.status = expired ? StockReservationStatus.EXPIRED : StockReservationStatus.RELEASED;
        this.releasedAt = now;
        return true;   // true → service mới được giảm reservedCount
    }
}
