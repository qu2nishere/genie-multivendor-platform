package com.genie.order.cart.domain;

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
 * CartItem — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: variant — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "cart_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "cart_id", nullable = false)
    private Long cartId;                      // FK → carts.id — thuộc giỏ hàng nào

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — mua BIẾN THỂ nào (không phải sản phẩm chung)

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng khách muốn mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceSnapshot;         // Giá tại thời điểm THÊM VÀO GIỎ — để phát hiện biến động giá

    @Column(nullable = false)
    private OffsetDateTime addedAt;            // Thời điểm thêm vào giỏ

    // ===== QUAN HỆ =====
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tăng số lượng — UC-ORD-01 */
    public void increase(int qty) {
        this.quantity += qty;
    }

    /** Thành tiền dòng — UC-ORD-04 */
    public BigDecimal lineTotal() {
        return this.priceSnapshot.multiply(BigDecimal.valueOf(this.quantity));
    }
}
