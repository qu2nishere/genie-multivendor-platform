package com.genie.payment.ledger.domain;

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
 * FinancialLedger — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "financial_ledger")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FinancialLedger {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;                         // Số THỨ TỰ bút toán — tăng dần tuyệt đối, không bao giờ tái sử dụng

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialEventType eventType;     // ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE, PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE

    @Column(nullable = false, length = 255)
    private String referenceId;               // ID đối tượng gốc: order_id / payout_id / refund_id

    private Long orderId;                     // NULLABLE — FK → orders.id khi bút toán gắn với một đơn (ESCROW_IN, COMMISSION, REFUND…) — lọc sổ cái theo đơn

    private Long vendorId;                    // NULLABLE — FK → users.id của shop khi bút toán gắn với một shop (giải ngân, phí) — đối soát theo shop

    @Column(nullable = false, length = 100)
    private String account;                   // TÊN TÀI KHOẢN kế toán bị tác động

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debit;                 // Số tiền ghi NỢ (bên trái)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal credit;                // Số tiền ghi CÓ (bên phải)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;          // Số dư của tài khoản đó sau bút toán này

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm ghi sổ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bút toán (bảng chỉ INSERT, cấm UPDATE/DELETE) — UC-PAY-06 */
    public static FinancialLedger entry(FinancialEventType type, String account, BigDecimal debit, BigDecimal credit, String referenceId) {
        FinancialLedger l = new FinancialLedger();
        l.eventType = type; l.account = account; l.debit = debit; l.credit = credit;
        l.referenceId = referenceId; l.createdAt = OffsetDateTime.now();
        return l;
    }
}
