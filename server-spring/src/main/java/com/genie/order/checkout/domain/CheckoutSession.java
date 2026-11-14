package com.genie.order.checkout.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.order.order.domain.Order;

/**
 * CheckoutSession — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "checkout_sessions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CheckoutSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — khách đang thanh toán

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;           // TỔNG tiền của cả phiên (gộp nhiều shop)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;             // INITIATED / PAYMENT_PENDING / COMPLETED / FAILED / EXPIRED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String cartSnapshotJson;          // NULLABLE — Ảnh chụp JSON toàn bộ giỏ hàng lúc bắt đầu thanh toán

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Hạn chót thanh toán (thường 15 phút) — hết hạn sẽ nhả tồn kho

    private OffsetDateTime completedAt;        // NULLABLE — Thời điểm thanh toán thành công

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bắt đầu phiên

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "checkout_session_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Order.checkoutSessionId
    private List<Order> orders;               // Các đơn hàng được tách ra theo từng shop
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chuyển sang chờ thanh toán — UC-ORD-07 */
    public void markPaymentPending() {
        if (this.status != SessionStatus.INITIATED) throw new IllegalStateException("Sai trạng thái phiên");
        this.status = SessionStatus.PAYMENT_PENDING;
    }

    /** Thanh toán thành công toàn phiên — UC-PAY-03 */
    public void complete(OffsetDateTime now) {
        if (this.status != SessionStatus.INITIATED && this.status != SessionStatus.PAYMENT_PENDING) throw new IllegalStateException("Phiên không còn chờ thanh toán");
        this.status = SessionStatus.COMPLETED;
        this.completedAt = now;
    }

    /** Cổng báo thất bại — UC-PAY-03 */
    public void fail() {
        if (this.status == SessionStatus.PAYMENT_PENDING) this.status = SessionStatus.FAILED;
    }

    /** Hết hạn 15 phút — Sweeper nhả tồn kho — UC-ORD-08 */
    public void expire() {
        if (this.status == SessionStatus.INITIATED || this.status == SessionStatus.PAYMENT_PENDING) this.status = SessionStatus.EXPIRED;
    }
}
