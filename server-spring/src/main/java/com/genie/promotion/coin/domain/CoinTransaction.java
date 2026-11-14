package com.genie.promotion.coin.domain;

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
 * CoinTransaction — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "coin_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CoinTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — Điểm của khách hàng nào

    @Column(nullable = false)
    private Integer amount;                   // Số Điểm biến động — DƯƠNG là cộng, ÂM là trừ (cùng quy ước dấu với các sổ ví tiền)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoinTransactionType type;         // CHECK_IN / ORDER_CASHBACK / REVIEW_BONUS / USED_AT_CHECKOUT / ORDER_REFUND / VOUCHER_REDEEM

    @Column(length = 50)
    private String referenceId;               // NULLABLE — ID đối tượng liên quan (thường là order_id)

    @Column(length = 200)
    private String label;                     // NULLABLE — Mô tả hiển thị: "Hoàn Điểm đơn hàng #1234"

    private OffsetDateTime expiresAt;          // NULLABLE — Điểm HẾT HẠN sau 12 tháng kể từ ngày nhận (giao diện: "Điểm hết hạn sau 12 tháng")

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Lô Điểm đã hết hạn (12 tháng) — UC-PRO-09 */
    public boolean isExpired(OffsetDateTime now) {
        return this.expiresAt != null && this.expiresAt.isBefore(now);
    }

    /** Giao dịch cộng Điểm — UC-PRO-09 */
    public boolean isCredit() {
        return this.amount > 0;
    }
}
