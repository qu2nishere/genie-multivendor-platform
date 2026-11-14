package com.genie.catalog.stats.domain;

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
 * ProductViewStat — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "product_view_stats", uniqueConstraints = {
    @UniqueConstraint(name = "uq_product_view_day_source", columnNames = {"product_id", "stat_date", "source"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ProductViewStat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productId;                   // FK → products.id

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — sao chép từ sản phẩm để tổng hợp theo shop không cần JOIN

    @Column(nullable = false)
    private LocalDate statDate;               // Ngày thống kê

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrafficSource source;             // SEARCH / HOME_FEED / PROMOTION / SHOP_PAGE / EXTERNAL_LINK

    @Builder.Default
    @Column(nullable = false)
    private Integer viewCount = 0;            // Lượt xem trang chi tiết

    @Builder.Default
    @Column(nullable = false)
    private Integer uniqueVisitorCount = 0;   // Khách khác nhau (HyperLogLog trên Redis, ghi số ước lượng)

    @Builder.Default
    @Column(nullable = false)
    private Integer addToCartCount = 0;       // Lượt thêm vào giỏ từ trang này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cộng dồn bộ đếm từ Redis (job 5 phút) — UC-CAT-06 */
    public void accumulate(int views, int visitors, int carts) {
        this.viewCount += views;
        this.uniqueVisitorCount = Math.max(this.uniqueVisitorCount, visitors);
        this.addToCartCount += carts;
    }
}
