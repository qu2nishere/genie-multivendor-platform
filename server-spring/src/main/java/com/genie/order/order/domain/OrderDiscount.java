package com.genie.order.order.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.common.domain.FundingParty;
import com.genie.common.domain.VoucherType;

/**
 * OrderDiscount — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "order_discounts")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class OrderDiscount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "order_id", nullable = false)
    private Long orderId;                     // FK → orders.id

    private Long voucherId;                   // NULLABLE — FK → vouchers.id. NULL khi là giảm giá từ Điểm

    @Column(length = 50)
    private String voucherCodeSnapshot;       // NULLABLE — Mã voucher CHỤP LẠI, phòng khi voucher bị xoá

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;        // Số tiền mã này đã giảm

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VoucherType voucherType;          // NULLABLE — PLATFORM / SHOP / FREESHIP / PAYMENT. NULL = dòng giảm giá từ Điểm (xem Order.coinDiscountAmount)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FundingParty fundedBy;            // NULLABLE — AI TRẢ TIỀN: PLATFORM hay VENDOR (enum, cùng enum với Voucher.sponsorType)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Voucher do sàn tài trợ (không trừ tiền shop) — UC-PRO-03 */
    public boolean isPlatformFunded() {
        return this.fundedBy == FundingParty.PLATFORM;
    }
}
