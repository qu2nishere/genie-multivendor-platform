package com.genie.promotion.voucher.domain;

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
 * UserVoucher — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: user — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "user_vouchers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "voucher_id"}))
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserVoucher {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — chủ ví

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;                   // FK → vouchers.id — mã đã lưu vào ví

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm user bấm "Lưu mã"

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false) @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private Voucher voucher;                  // Chi tiết voucher


    // ---- Trường tính toán, KHÔNG lưu DB ----
    @Transient
    private Boolean isUsed;                   // true = user đã dùng hết lượt cho phép của mã này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu voucher trong ví đã dùng — UC-PRO-03 */
    public void markUsed() {
        if (Boolean.TRUE.equals(this.isUsed)) throw new IllegalStateException("Voucher đã dùng");
        this.isUsed = true;
    }
}
