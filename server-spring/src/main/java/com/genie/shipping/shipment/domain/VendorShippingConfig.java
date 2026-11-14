package com.genie.shipping.shipment.domain;

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
 * VendorShippingConfig — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "vendor_shipping_config")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorShippingConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorId;                    // FK → users.id, UNIQUE — mỗi shop 1 cấu hình

    @Builder.Default   // sửa lệch ENTITY.md theo Quy ước 12 (implement-entity.md mục 6)
    @Column(name = "ghn_enabled", nullable = false)
    private Boolean ghnEnabled = true;               // Bật/tắt Giao Hàng Nhanh (mặc định bật)

    @Builder.Default
    @Column(name = "ghtk_enabled", nullable = false)
    private Boolean ghtkEnabled = true;              // Bật/tắt Giao Hàng Tiết Kiệm (mặc định bật)

    @Builder.Default
    @Column(name = "vtp_enabled", nullable = false)
    private Boolean vtpEnabled = false;               // Bật/tắt Viettel Post (mặc định tắt)

    @Builder.Default
    @Column(name = "jt_enabled", nullable = false)
    private Boolean jtEnabled = false;                // Bật/tắt J&T Express (mặc định tắt)

    @Builder.Default
    @Column(name = "genie_express_enabled", nullable = false)
    private Boolean genieExpressEnabled = false;    // Bật/tắt giao nội thành Genie Express (FBL) — TLCN chỉ là lựa chọn, không có app tài xế (mặc định tắt)

    @Column(precision = 15, scale = 2)
    private BigDecimal freeshipThreshold;     // NULLABLE — Ngưỡng đơn tối thiểu để shop MIỄN PHÍ ship
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đơn đạt ngưỡng shop miễn phí ship — UC-ORD-06 */
    public boolean isFreeshipFor(BigDecimal orderValue) {
        return this.freeshipThreshold != null && orderValue.compareTo(this.freeshipThreshold) >= 0;
    }

    /** Shop có bật hãng vận chuyển này không — UC-ORD-06 */
    public boolean supports(CarrierType carrier) {
        return switch (carrier) {
            case GHN -> Boolean.TRUE.equals(this.ghnEnabled);
            case GHTK -> Boolean.TRUE.equals(this.ghtkEnabled);
            case VIETTEL_POST -> Boolean.TRUE.equals(this.vtpEnabled);
            case GENIE_EXPRESS -> Boolean.TRUE.equals(this.genieExpressEnabled);
            case JNT_EXPRESS -> Boolean.TRUE.equals(this.jtEnabled);   // sửa lệch ENTITY.md: switch thiếu nhánh J&T (Java 21 bắt buộc đủ nhánh)
        };
    }
}
