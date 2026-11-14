package com.genie.support.refund.domain;

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
 * RefundRequestItem — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "refund_request_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundRequestItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "refund_request_id", nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id

    @Column(nullable = false)
    private Long orderItemId;                 // FK → order_items.id — hoàn dòng hàng nào của đơn

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng muốn trả (có thể ÍT HƠN số đã mua)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal itemRefundAmount;      // Số tiền hoàn cho dòng này (đã tính cả phân bổ giảm giá)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Số tiền hoàn của dòng (đã trừ chiết khấu phân bổ) — UC-SUP-01 */
    public BigDecimal amount() {
        return this.itemRefundAmount;
    }
}
