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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * UserCoin — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "user_coins")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserCoin {

    @Id
    private Long userId;                      // KHOÁ CHÍNH đồng thời là FK → users.id (KHÔNG tự tăng) — ví Điểm của khách hàng (1 Điểm = 1₫ khi thanh toán)

    @Column(nullable = false)
    private Integer balance;                  // Số Điểm hiện có

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống race condition khi trừ Điểm đồng thời

    @Column(name = "balance_hash", nullable = false, length = 255)
    private String balanceHash;               // HMAC-SHA256(userId + balance + secret) — chống can thiệp trực tiếp CSDL

    @Column(nullable = false)
    private OffsetDateTime lastUpdated;        // Lần biến động gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng Điểm (service cập nhật lại balanceHash) — UC-PRO-08 */
    public void earn(int amount, OffsetDateTime now) {
        this.balance += amount;
        this.lastUpdated = now;
    }

    /** Trừ Điểm (thanh toán đơn / đổi voucher), FEFO theo lô ở service — UC-PRO-10 */
    public void spend(int amount, OffsetDateTime now) {
        if (amount > this.balance) throw new IllegalStateException("Không đủ Điểm");
        this.balance -= amount;
        this.lastUpdated = now;
    }

    /** Phát hiện sửa số dư trực tiếp CSDL — UC-PRO-09 */
    public boolean verifyHash(String expectedHmac) {
        return MessageDigest.isEqual(this.balanceHash.getBytes(StandardCharsets.UTF_8), expectedHmac.getBytes(StandardCharsets.UTF_8));
    }
}
