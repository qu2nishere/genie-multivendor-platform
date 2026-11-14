package com.genie.order.order.domain;

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
 * OrderItem — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: productVariant — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "order_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "order_id", nullable = false)
    private Long orderId;                     // FK → orders.id — thuộc đơn hàng nào

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — biến thể đã mua

    @Column(length = 255)
    private String productNameSnapshot;       // NULLABLE — Tên sản phẩm CHỤP LẠI lúc mua (bất biến)

    @Column(length = 100)
    private String skuSnapshot;               // NULLABLE — Mã SKU chụp lại lúc mua

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;             // Đơn giá CHỐT tại thời điểm mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;             // Thành tiền dòng = unitPrice × quantity

    // ---- Phân bổ giảm giá xuống từng dòng ----
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal proratedDiscount = BigDecimal.ZERO; // Phần giảm giá chung được chia tỷ lệ cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformDiscount = BigDecimal.ZERO; // Phần giảm do SÀN tài trợ, phân bổ cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shopDiscount = BigDecimal.ZERO; // Phần giảm do SHOP tài trợ, phân bổ cho dòng này

    // ---- Kết toán tài chính cho dòng này ----
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;           // Doanh thu gộp của dòng, dùng làm căn cứ tính hoa hồng

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;        // Tỷ lệ hoa hồng CHỤP LẠI lúc bán. VD 0.0550 = 5.5%

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionAmount;      // Số tiền hoa hồng sàn thu = grossAmount × commissionRate

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal vendorReceivable;      // Số tiền shop thực nhận cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coinDiscount = BigDecimal.ZERO; // Phần giảm từ Điểm, phân bổ cho dòng này (sàn chịu — shop vẫn nhận đủ)

    // ===== QUAN HỆ =====
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chụp tỷ lệ & tính hoa hồng dòng hàng — UC-PAY-05 */
    public void applyCommission(BigDecimal rate) {
        this.commissionRate = rate;
        this.commissionAmount = this.grossAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        this.vendorReceivable = this.grossAmount.subtract(this.commissionAmount);
    }

    /** Gán phần chiết khấu phân bổ (phần dư dồn dòng cuối) — UC-PRO-03 */
    public void allocateDiscount(BigDecimal prorated) {
        this.proratedDiscount = prorated;
    }
}
