package com.genie.payment.wallet.domain;

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
 * WithdrawalRequest — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "withdrawal_requests")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class WithdrawalRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long walletId;                    // FK mềm theo ownerType → vendor_wallets.id / customer_wallets.id / shippers.id

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền yêu cầu rút

    @Column(nullable = false, length = 255)
    private String bankAccountName;           // Tên chủ TK ngân hàng — CHỤP LẠI lúc gửi yêu cầu

    @Column(nullable = false, length = 50)
    private String bankAccountNumber;         // Số TK ngân hàng — chụp lại

    @Column(nullable = false, length = 100)
    private String bankName;                  // Tên ngân hàng — chụp lại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;          // PENDING / APPROVED / PROCESSING / COMPLETED / REJECTED

    @Column(length = 500)
    private String rejectionReason;           // NULLABLE — Lý do từ chối, để shop biết đường xử lý

    private Long processedByAdminId;          // NULLABLE — FK mềm → users.id, admin nào đã xử lý

    private Long walletTransactionId;         // NULLABLE — FK → wallet_transactions.id, bút toán tương ứng

    @Column(nullable = false)
    private OffsetDateTime requestedAt;        // Thời điểm shop gửi yêu cầu

    private OffsetDateTime processedAt;        // NULLABLE — Thời điểm admin xử lý xong

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nhân viên sàn phê duyệt lệnh rút (MVP duyệt một bước) — UC-ADM-05 */
    public void approve(Long adminId, OffsetDateTime now) {
        if (this.status != WithdrawalStatus.PENDING) throw new IllegalStateException("Lệnh rút không ở PENDING");
        this.status = WithdrawalStatus.APPROVED;
        this.processedByAdminId = adminId;
        this.processedAt = now;
    }

    /** Đã gửi lệnh chuyển khoản sang ngân hàng — UC-ADM-05 */
    public void startProcessing() {
        if (this.status != WithdrawalStatus.APPROVED) throw new IllegalStateException("Chưa phê duyệt");
        this.status = WithdrawalStatus.PROCESSING;
    }

    /** Ngân hàng xác nhận đã chuyển — UC-ADM-05 */
    public void complete(Long walletTransactionId) {
        if (this.status != WithdrawalStatus.APPROVED && this.status != WithdrawalStatus.PROCESSING) throw new IllegalStateException("Lệnh rút chưa duyệt");
        this.status = WithdrawalStatus.COMPLETED;
        this.walletTransactionId = walletTransactionId;
    }

    /** Từ chối, trả tiền về số dư — UC-ADM-05 */
    public void reject(String reason, Long adminId, OffsetDateTime now) {
        if (this.status == WithdrawalStatus.COMPLETED || this.status == WithdrawalStatus.REJECTED) throw new IllegalStateException("Lệnh rút đã kết thúc");
        this.status = WithdrawalStatus.REJECTED;
        this.rejectionReason = reason;
        this.processedByAdminId = adminId;
        this.processedAt = now;
    }
}
