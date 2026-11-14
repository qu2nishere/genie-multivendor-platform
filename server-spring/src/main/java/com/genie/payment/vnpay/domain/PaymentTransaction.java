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
import com.genie.common.domain.PaymentMethod;
import com.genie.common.domain.PaymentStatus;

/**
 * PaymentTransaction — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "payment_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PaymentTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    private Long orderId;                     // NULLABLE — FK → orders.id (khi thanh toán cho 1 đơn lẻ)

    private Long checkoutSessionId;           // NULLABLE — FK → checkout_sessions.id (khi trả gộp nhiều shop)

    @Column(unique = true, nullable = false, length = 255)
    private String idempotencyKey;            // KHOÁ CHỐNG TRÙNG — chìa khoá ngăn trừ tiền 2 lần

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền giao dịch

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // Tổng đã hoàn (cho phép hoàn nhiều lần từng phần)

    @Column(length = 255)
    private String gatewayTransactionId;      // NULLABLE — VNPAY = vnp_TransactionNo; VIETQR = `id` giao dịch SePay (UNIQUE — chống trùng webhook); COD = NULL

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;    // NULLABLE — VNPAY / MOMO / ZALOPAY / VIETQR / INTERNAL_WALLET / COD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;             // PENDING / REQUIRES_ACTION / SUCCESS / FAILED / EXPIRED / REFUNDED / PARTIALLY_REFUNDED / CANCELLED

    @Column(columnDefinition = "TEXT")
    private String webhookPayload;            // NULLABLE — JSON THÔ cổng thanh toán gửi về, giữ nguyên để đối soát

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống xử lý song song 1 giao dịch

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm khởi tạo giao dịch
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Ghi nhận IPN thành công (idempotent) — UC-PAY-03 */
    public void markSuccess(String gatewayTxId, String payload) {
        if (this.status == PaymentStatus.SUCCESS) return;   // IPN lặp → idempotent
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.REQUIRES_ACTION) throw new IllegalStateException("Giao dịch đã kết thúc: " + this.status);
        this.status = PaymentStatus.SUCCESS;
        this.gatewayTransactionId = gatewayTxId;
        this.webhookPayload = payload;
    }

    /** Ghi nhận thất bại (service gọi Order.markPaymentFailed) — UC-PAY-03 */
    public void markFailed(String payload) {
        if (this.status == PaymentStatus.SUCCESS) throw new IllegalStateException("Không ghi đè giao dịch đã thành công");
        this.status = PaymentStatus.FAILED;
        this.webhookPayload = payload;
    }

    /** Số tiền còn có thể hoàn — UC-SUP-04 */
    public BigDecimal refundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }

    /** Cập nhật sau khi hoàn qua cổng — UC-SUP-04 */
    public void recordRefund(BigDecimal amt) {
        if (this.status != PaymentStatus.SUCCESS && this.status != PaymentStatus.PARTIALLY_REFUNDED) throw new IllegalStateException("Chỉ hoàn giao dịch đã thành công");
        if (amt.compareTo(refundableAmount()) > 0) throw new IllegalArgumentException("Vượt số tiền có thể hoàn");
        this.refundedAmount = this.refundedAmount.add(amt);
        this.status = refundableAmount().signum() == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }
}
