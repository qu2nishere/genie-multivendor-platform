package com.genie.catalog.product.domain;

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
 * ProductVariant — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "product_variants")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductVariant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "product_id", nullable = false)
    private Long productId;                   // FK → products.id — biến thể này thuộc sản phẩm nào

    @Column(unique = true, nullable = false, length = 100)
    private String sku;                       // Mã SKU duy nhất toàn hệ thống — mã kho của đơn vị bán

    @Column(length = 255)
    private String variantName;               // NULLABLE — Tên hiển thị của biến thể: "Đen - Size L"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attributesJson;            // NULLABLE — JSON thuộc tính: {"Màu":"Đen","Size":"L"}

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;                 // GIÁ BÁN THẬT của biến thể này

    @Column(nullable = false)
    private Integer stockCount;               // Tổng tồn kho vật lý đang có

    @Builder.Default
    @Column(nullable = false)
    private Integer reservedCount = 0;        // Số lượng đang bị GIỮ CHỖ cho các đơn chưa thanh toán xong

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — lá chắn chống bán vượt tồn kho (overselling)

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh riêng của biến thể này

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;          // NULLABLE — Xoá mềm (@SQLDelete + @SQLRestriction). NULL = chưa xoá

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;                  // Sản phẩm cha
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tồn khả dụng = tồn vật lý − đang giữ chỗ — UC-CAT-06 */
    public int availableStock() {
        return Math.max(0, this.stockCount - this.reservedCount);
    }

    /** Giữ chỗ mềm (service dùng UPDATE có điều kiện) — UC-ORD-07 */
    public void reserve(int qty) {
        if (availableStock() < qty) throw new IllegalStateException("GNE-INV-4090");   // hết tồn kho
        this.reservedCount += qty;
    }

    /** Nhả giữ chỗ (chỉ gọi sau khi StockReservation.release() thành công) — UC-ORD-08 */
    public void releaseReservation(int qty) {
        if (this.reservedCount < qty) throw new IllegalStateException("reservedCount không đủ để nhả — giữ chỗ đã được xử lý");
        this.reservedCount -= qty;
    }

    /** Trừ tồn cứng khi PAID — chỉ gọi sau StockReservation.confirm() (ACTIVE) để chống đua với Sweeper — UC-PAY-03 */
    public void commitSale(int qty) {
        if (this.reservedCount < qty || this.stockCount < qty) throw new IllegalStateException("GNE-INV-4093");
        this.reservedCount -= qty;
        this.stockCount -= qty;
    }

    /** Xóa mềm biến thể: chặn khi còn giữ chỗ của đơn đang xử lý; còn tồn thì chỉ Admin được ép xóa (force) — UC-CAT-11 */
    public void softDelete(OffsetDateTime now, boolean force) {
        if (this.reservedCount > 0) throw new IllegalStateException("Biến thể đang được giữ chỗ cho " + this.reservedCount + " sản phẩm trong đơn chưa thanh toán");
        if (this.stockCount > 0 && !force) throw new IllegalStateException("Còn " + this.stockCount + " tồn kho — đưa tồn về 0 hoặc Admin xóa cưỡng bức");
        this.deletedAt = now;
    }
}
