package com.genie.social.follow.domain;

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
 * ShopFollow — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "shop_follows", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"customerId", "vendorId"})
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShopFollow {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — người theo dõi (UNIQUE ghép với vendorId)

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop được theo dõi

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bấm theo dõi
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Theo dõi shop (UNIQUE customer+vendor) — UC-SOC-06 */
    public static ShopFollow of(Long customerId, Long vendorId, OffsetDateTime now) {
        ShopFollow f = new ShopFollow();
        f.customerId = customerId; f.vendorId = vendorId; f.createdAt = now;
        return f;
    }
}
