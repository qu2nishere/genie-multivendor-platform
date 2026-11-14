package com.genie.payment.vnpay.domain;

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
 * PaymentRefund — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "payment_refunds")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PaymentRefund {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long paymentTransactionId;        // FK → payment_transactions.id — hoàn cho giao dịch gốc nào

    @Column(nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id — xuất phát từ yêu cầu hoàn nào

    @Column(nullable = false, length = 255)
    private String gatewayRefundId;           // Mã hoàn tiền bên cổng thanh toán cấp

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền hoàn của lần này

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRefundStatus status;       // PENDING / SUCCESS / FAILED
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cổng xác nhận hoàn tiền — UC-SUP-04 */
    public void markSuccess(String gatewayRefundId) {
        this.status = PaymentRefundStatus.SUCCESS;
        this.gatewayRefundId = gatewayRefundId;
    }

    /** Hoàn tiền thất bại — chuyển xử lý tay — UC-SUP-04 */
    public void markFailed() {
        this.status = PaymentRefundStatus.FAILED;
    }
}
