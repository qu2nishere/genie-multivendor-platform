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
 * WalletTransaction — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class WalletTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long walletId;                    // FK → vendor_wallets.id — biến động của ví nào

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionType type;       // CREDIT_ESCROW_RELEASE / DEBIT_REFUND / DEBIT_WITHDRAWAL / CREDIT_ADJUSTMENT / DEBIT_ADJUSTMENT / CREDIT_TOPUP / DEBIT_PURCHASE (7 giá trị)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền biến động CÓ DẤU: DƯƠNG = CREDIT (cộng vào ví), ÂM = DEBIT (trừ khỏi ví) — quy ước chung của mọi sổ ví

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;         // SỐ DƯ TRƯỚC giao dịch — bất biến kiểm toán: balanceBefore + amount = balanceAfter

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;          // SỐ DƯ SAU giao dịch — dấu vết kiểm toán

    @Column(length = 255)
    private String referenceId;               // NULLABLE — ID của đối tượng liên quan (order_id, withdrawal_id...)

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WalletReferenceType referenceType; // NULLABLE — Loại đối tượng: ORDER / WITHDRAWAL / REFUND / ADJUSTMENT

    @Column(length = 500)
    private String note;                      // NULLABLE — Ghi chú, đặc biệt quan trọng với các khoản điều chỉnh tay

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo giao dịch trừ ví (append-only, gọi sau khi ví đã trừ; balanceBefore = số dư sau + số trừ) — UC-ADM-04 */
    public static WalletTransaction debit(VendorWallet wallet, WalletTransactionType type, BigDecimal amount, WalletReferenceType refType, String reference) {
        WalletTransaction t = new WalletTransaction();
        t.walletId = wallet.getId(); t.type = type; t.amount = amount.negate();
        t.balanceAfter = wallet.getBalance(); t.balanceBefore = t.balanceAfter.add(amount);
        t.referenceType = refType; t.referenceId = reference; t.createdAt = OffsetDateTime.now();
        return t;
    }

    /** Tạo giao dịch cộng ví (append-only, gọi sau khi ví đã cộng) — UC-PAY-07 */
    public static WalletTransaction credit(VendorWallet wallet, WalletTransactionType type, BigDecimal amount, WalletReferenceType refType, String reference) {
        WalletTransaction t = new WalletTransaction();
        t.walletId = wallet.getId(); t.type = type; t.amount = amount;
        t.balanceAfter = wallet.getBalance(); t.balanceBefore = t.balanceAfter.subtract(amount);
        t.referenceType = refType; t.referenceId = reference; t.createdAt = OffsetDateTime.now();
        return t;
    }

    /** Kiểm tra bất biến sổ ví (job đối soát đêm) — UC-PAY-07 */
    public boolean isConsistent() {
        return this.balanceBefore.add(this.amount).compareTo(this.balanceAfter) == 0;
    }
}
