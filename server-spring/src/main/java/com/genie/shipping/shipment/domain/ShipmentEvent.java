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
 * ShipmentEvent — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "shipment_events")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShipmentEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;                  // FK → shipment_trackings.id — thuộc vận đơn nào

    @Column(nullable = false, length = 100)
    private String eventStatus;               // Mã trạng thái THÔ do hãng vận chuyển gửi về

    @Column(length = 255)
    private String location;                  // NULLABLE — Địa điểm xảy ra sự kiện: "Kho Sóc Sơn - Hà Nội"

    @Column(nullable = false)
    private OffsetDateTime occurredAt;         // Thời điểm sự kiện XẢY RA (theo hãng vận chuyển)

    @Column(length = 500)
    private String description;               // NULLABLE — Mô tả dễ đọc: "Đơn hàng đã rời kho phân loại"

    @Column(nullable = false)
    private OffsetDateTime receivedAt;         // Thời điểm hệ thống NHẬN ĐƯỢC webhook này
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chống ghi trùng webhook — UC-LOG-03 */
    public boolean isDuplicateOf(ShipmentEvent other) {
        return this.shipmentId.equals(other.getShipmentId()) && this.eventStatus.equals(other.getEventStatus()) && this.occurredAt.equals(other.getOccurredAt());
    }
}
