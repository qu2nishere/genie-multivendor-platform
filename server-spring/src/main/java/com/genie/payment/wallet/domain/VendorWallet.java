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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * VendorWallet — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "vendor_wallets")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorWallet {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorId;                    // FK → users.id, UNIQUE — mỗi shop đúng 1 ví

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO; // Số dư KHẢ DỤNG — tiền shop có thể rút ngay

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingWithdrawal = BigDecimal.ZERO; // Số tiền đang bị GIỮ do có lệnh rút chờ xử lý

    @Column(nullable = false, length = 255)
    private String balanceHash;               // HMAC-SHA256(vendorId + balance + pendingWithdrawal + secret) — service verify trước mỗi UPDATE và ghi lại sau UPDATE, chống sửa trực tiếp CSDL

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — LÁ CHẮN CHỐNG RÚT TIỀN 2 LẦN

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo ví

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm biến động số dư gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Số dư khả dụng = số dư − đang chờ rút — UC-ADM-04 */
    public BigDecimal availableBalance() {
        return this.balance.subtract(this.pendingWithdrawal);
    }

    /** Cộng tiền (giải ngân Escrow, nạp ví) — UC-PAY-07 */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    /** Trừ tiền (rút tiền, thu hồi khi hoàn tiền sau giải ngân, điều chỉnh giảm) — UC-ADM-04 */
    public void debit(BigDecimal amount) {
        if (availableBalance().compareTo(amount) < 0) throw new IllegalStateException("GNE-WAL-4001");
        this.balance = this.balance.subtract(amount);
    }

    /** Giữ tiền khi tạo lệnh rút — UC-ADM-04 */
    public void holdForWithdrawal(BigDecimal amount) {
        if (availableBalance().compareTo(amount) < 0) throw new IllegalStateException("GNE-WAL-4001");
        this.pendingWithdrawal = this.pendingWithdrawal.add(amount);
    }

    /** Chốt lệnh rút (chi tiền hoặc trả lại số dư) — UC-ADM-05 */
    public void settleWithdrawal(BigDecimal amount, boolean approved) {
        this.pendingWithdrawal = this.pendingWithdrawal.subtract(amount);
        if (approved) this.balance = this.balance.subtract(amount);
    }

    /** Phát hiện sửa số dư trực tiếp CSDL (so HMAC hằng thời gian) — UC-PAY-07 */
    public boolean verifyHash(String expectedHmac) {
        return MessageDigest.isEqual(this.balanceHash.getBytes(StandardCharsets.UTF_8), expectedHmac.getBytes(StandardCharsets.UTF_8));
    }
}
