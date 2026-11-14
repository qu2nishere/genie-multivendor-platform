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
 * ShipmentTracking — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "shipment_trackings")   // DDL: CREATE UNIQUE INDEX uq_shipment_outbound ON shipment_trackings(order_id) WHERE direction = 'OUTBOUND';
                                      //      CREATE UNIQUE INDEX uq_shipment_return   ON shipment_trackings(refund_request_id) WHERE direction = 'RETURN';
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShipmentTracking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — 1 vận đơn chiều đi (OUTBOUND) / đơn + 0..N vận đơn hoàn (RETURN, mỗi yêu cầu trả hàng 1 vận đơn)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ShipmentDirection direction;      // OUTBOUND (shop → khách) / RETURN (khách → shop, Reverse Logistics UC-SUP-02)

    private Long refundRequestId;             // NULLABLE — FK → refund_requests.id; BẮT BUỘC khi direction = RETURN (CHECK), NULL khi OUTBOUND

    @Column(length = 100)
    private String trackingCode;              // NULLABLE — Mã vận đơn khách dùng để tra cứu

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CarrierType carrierType;          // NULLABLE — Hãng vận chuyển được chọn khi điều phối (UC-LOG-15): GHN / GHTK / VIETTEL_POST / JNT_EXPRESS / GENIE_EXPRESS

    @Column(length = 100)
    private String carrierOrderId;            // NULLABLE — Mã đơn bên hệ thống hãng vận chuyển

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentTrackingStatus status;    // AWAITING_PICKUP → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED (hoặc FAILED/RETURNING/RETURNED/LOST)

    private OffsetDateTime estimatedDeliveryAt; // NULLABLE — Ngày giao DỰ KIẾN hãng vận chuyển cam kết

    private OffsetDateTime actualDeliveredAt;  // NULLABLE — Ngày giao THỰC TẾ

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "shipment_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua ShipmentEvent.shipmentId
    private List<ShipmentEvent> events;       // Nhật ký hành trình chi tiết
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cập nhật trạng thái từ webhook 3PL đã chuẩn hóa — UC-LOG-03 */
    public void applyCarrierStatus(ShipmentTrackingStatus newStatus, OffsetDateTime at) {
        this.status = newStatus;
        if (newStatus == ShipmentTrackingStatus.DELIVERED) this.actualDeliveredAt = at;
    }

    /** Trạng thái kết thúc — UC-ORD-09 */
    public boolean isFinal() {
        return this.status == ShipmentTrackingStatus.DELIVERED || this.status == ShipmentTrackingStatus.RETURNED || this.status == ShipmentTrackingStatus.LOST;
    }

    /** Tạo vận đơn chiều về cho yêu cầu trả hàng đã duyệt — UC-SUP-02 */
    public static ShipmentTracking forReturn(Long orderId, Long refundRequestId, CarrierType carrier) {
        ShipmentTracking t = new ShipmentTracking();
        t.orderId = orderId; t.refundRequestId = refundRequestId; t.carrierType = carrier;
        t.direction = ShipmentDirection.RETURN; t.status = ShipmentTrackingStatus.AWAITING_PICKUP;
        return t;
    }
}
