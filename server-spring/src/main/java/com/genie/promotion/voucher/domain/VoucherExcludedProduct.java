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
 * VoucherExcludedProduct — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: product — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "voucher_excluded_products")
@IdClass(VoucherExcludedProductId.class)      // Khoá chính GỘP (composite key) 2 cột
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VoucherExcludedProduct {

    @Id
    @Column(name = "voucher_id")   // D8: trùng tên logic với @JoinColumn chỉ-đọc
    private Long voucherId;                   // FK → vouchers.id — phần 1 của khoá chính

    @Id
    private Long productId;                   // FK → products.id — phần 2 của khoá chính

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm gắn sản phẩm BỊ LOẠI TRỪ khỏi voucher (@CreationTimestamp)

    // ===== QUAN HỆ =====
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Phạm vi loại trừ của voucher — UC-PRO-01 */
    public boolean matches(Long productId) {
        return this.productId.equals(productId);
    }
}
