package com.genie.promotion.flashsale.domain;

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
 * FlashSaleItem — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "flash_sale_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FlashSaleItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;                  // FK → flash_sale_campaigns.id

    @Column(nullable = false)
    private Long productId;                   // FK → products.id — sản phẩm được giảm giá

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal flashPrice;            // GIÁ SỐC trong thời gian diễn ra chiến dịch

    @Column(nullable = false)
    private Integer flashStock;               // Số lượng GIỚI HẠN dành riêng cho flash sale

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer reservedCount = 0;        // Số suất đã có ĐƠN đang chờ thanh toán (tăng khi tạo Order + StockReservation; giảm khi PAID hoặc giữ chỗ hết hạn)

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer soldCount = 0;            // Số đã bán trong flash sale — dùng vẽ thanh tiến trình

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlashSaleItemStatus status = FlashSaleItemStatus.PENDING; // Kết quả sàn duyệt đăng ký: PENDING / APPROVED / REJECTED — UC-PRO-05 (TLCN v1.3)

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống race condition khi trừ tồn kho Flash Sale đồng thời
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đăng ký: 0 < flashStock ≤ tồn khả dụng của sản phẩm (Σ stockCount − reservedCount các SKU); kiểm tra lại khi chiến dịch mở bán — UC-PRO-05 */
    public void validateAgainst(int productAvailableStock) {
        if (this.flashStock <= 0 || this.flashStock > productAvailableStock)
            throw new IllegalArgumentException("Số suất Flash Sale (" + this.flashStock + ") phải > 0 và ≤ tồn khả dụng (" + productAvailableStock + ")");
    }

    /** Suất còn lại — UC-PRO-06 */
    public int remaining() {
        return this.flashStock - this.soldCount - this.reservedCount;
    }

    /** Giữ suất khi tạo Order + StockReservation trong cùng transaction — UC-PRO-06 */
    public void reserve(int qty) {
        if (remaining() < qty) throw new IllegalStateException("GNE-INV-4091");   // hết suất Flash Sale
        this.reservedCount += qty;
    }

    /** Giữ chỗ hết hạn / đơn bị hủy trước khi trả tiền → trả suất — UC-ORD-08 */
    public void releaseReservation(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ để nhả");
        this.reservedCount -= qty;
    }

    /** Ghi bền khi đơn Flash Sale thanh toán thành công — UC-PRO-06 */
    public void recordSale(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ");
        this.reservedCount -= qty;
        this.soldCount += qty;
    }
}
