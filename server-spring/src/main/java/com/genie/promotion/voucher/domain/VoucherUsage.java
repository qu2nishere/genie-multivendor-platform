package com.genie.promotion.voucher.domain;

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
 * VoucherUsage — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "voucher_usages", uniqueConstraints = {
    @UniqueConstraint(name = "uq_voucher_usage_session", columnNames = {"voucher_id", "checkout_session_id"})   // 1 voucher chỉ trừ 1 lượt / phiên thanh toán
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherUsage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;                   // FK → vouchers.id — mã nào đã được dùng

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — ai dùng

    @Column(nullable = false)
    private Long checkoutSessionId;           // FK → checkout_sessions.id — phiên thanh toán đã dùng mã (đơn vị tính lượt dùng)

    private Long orderId;                     // NULLABLE — FK → orders.id. CÓ giá trị với voucher SHOP (đúng 1 đơn con của shop đó); NULL với voucher PLATFORM / FREESHIP / PAYMENT áp toàn phiên (nhiều đơn con)

    @Column(nullable = false)
    private OffsetDateTime usedAt;             // Thời điểm sử dụng

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private Voucher voucher;                  // Voucher được dùng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bản ghi lượt dùng — 1 bản ghi / voucher / phiên (UNIQUE voucher+session); shopOrderId chỉ truyền với voucher SHOP — UC-PRO-03 */
    public static VoucherUsage of(Long voucherId, Long userId, Long checkoutSessionId, Long shopOrderId, OffsetDateTime now) {
        VoucherUsage u = new VoucherUsage();
        u.voucherId = voucherId; u.userId = userId; u.checkoutSessionId = checkoutSessionId; u.orderId = shopOrderId; u.usedAt = now;
        return u;
    }
}
