/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 06: Vận đơn 3PL, Cấu hình vận chuyển của shop, Trả hàng & Hoàn tiền (Nhóm 7–8)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.2) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 07. NHÓM VẬN CHUYỂN, 08. NHÓM HOÀN TIỀN / KHIẾU NẠI
 * Thành phần     : 6 @Entity, 0 @IdClass, 7 enum, 60 thuộc tính, 25 phương thức nghiệp vụ
 * Entity         : ShipmentTracking, ShipmentEvent, VendorShippingConfig, RefundRequest, RefundRequestItem, RefundEvidence
 * Enum           : CarrierType, ShipmentDirection, ShipmentTrackingStatus, RefundRequestType, RefundResolution, RefundStatus, MediaType
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d06_shipping_refund;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.*;
import java.util.*;

// ============================== ENUMS (7 — Phụ lục A) ==============================

public enum CarrierType { GHN, GHTK, VIETTEL_POST, JNT_EXPRESS, GENIE_EXPRESS }   // ← ShipmentTracking.carrierType (GENIE_EXPRESS chỉ là lựa chọn giao nội thành — không có app tài xế ở TLCN)

public enum ShipmentDirection { OUTBOUND, RETURN }   // ← ShipmentTracking.direction

public enum ShipmentTrackingStatus {                                                           // ← ShipmentTracking.status (9 trạng thái)
    AWAITING_PICKUP, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED, RETURNING, RETURNED, LOST
}

public enum RefundRequestType { RETURN_AND_REFUND, REFUND_ONLY }   // ← RefundRequest.requestType

public enum RefundResolution { APPROVE_WITH_RETURN, APPROVE_REFUND_ONLY }   // ← RefundRequest.resolutionMode

public enum RefundStatus {                                                                     // ← RefundRequest.status (9 trạng thái)
    REQUESTED, AWAITING_EVIDENCE, SHOP_REJECTED, DISPUTED, AWAITING_RETURN_SHIPMENT, RETURN_FAILED, APPROVED, REJECTED, REFUNDED
}

public enum MediaType { IMAGE, VIDEO }   // ← RefundEvidence.mediaType

// ============================== ENTITIES (6) ==============================

// 7.1. ShipmentTracking — Vận đơn  —  Bảng: shipment_trackings
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

// 7.2. ShipmentEvent — Sự kiện hành trình  —  Bảng: shipment_events
@Entity
@Table(name = "shipment_events")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class ShipmentEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
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

// 7.3. VendorShippingConfig — Cấu hình vận chuyển của shop  —  Bảng: vendor_shipping_config
@Entity
@Table(name = "vendor_shipping_config")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorShippingConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorId;                    // FK → users.id, UNIQUE — mỗi shop 1 cấu hình

    @Column(name = "ghn_enabled", nullable = false)
    private Boolean ghnEnabled;               // Bật/tắt Giao Hàng Nhanh (mặc định bật)

    @Column(name = "ghtk_enabled", nullable = false)
    private Boolean ghtkEnabled;              // Bật/tắt Giao Hàng Tiết Kiệm (mặc định bật)

    @Column(name = "vtp_enabled", nullable = false)
    private Boolean vtpEnabled;               // Bật/tắt Viettel Post (mặc định tắt)

    @Column(name = "jt_enabled", nullable = false)
    private Boolean jtEnabled;                // Bật/tắt J&T Express (mặc định tắt)

    @Column(name = "genie_express_enabled", nullable = false)
    private Boolean genieExpressEnabled;    // Bật/tắt giao nội thành Genie Express (FBL) — TLCN chỉ là lựa chọn, không có app tài xế (mặc định tắt)

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
        };
    }
}

// 8.1. RefundRequest — Yêu cầu hoàn tiền  —  Bảng: refund_requests
@Entity
@Table(name = "refund_requests")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — hoàn tiền cho đơn nào

    @Column(nullable = false)
    private Long requestedByUserId;           // FK → users.id — ai gửi yêu cầu (thường là khách)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;          // Tổng số tiền yêu cầu hoàn

    @Column(columnDefinition = "TEXT")
    private String reason;                    // NULLABLE — Lý do khách đưa ra: "Hàng lỗi", "Sai mẫu"...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundRequestType requestType;    // Phương án KHÁCH chọn: RETURN_AND_REFUND (gửi trả hàng rồi hoàn tiền) / REFUND_ONLY (hoàn tiền không cần trả hàng — thiếu hàng, hư hỏng một phần) — UC-SUP-01

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RefundResolution resolutionMode;  // NULLABLE — Phương án được DUYỆT (shop / sàn / tự động): APPROVE_WITH_RETURN (sinh vận đơn chiều về) / APPROVE_REFUND_ONLY (hoàn ngay) — UC-SUP-02

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status;              // REQUESTED / AWAITING_EVIDENCE / SHOP_REJECTED / DISPUTED / AWAITING_RETURN_SHIPMENT / RETURN_FAILED / APPROVED / REJECTED / REFUNDED (9 trạng thái)

    private Long resolvedByStaffId;           // NULLABLE — FK mềm → users.id, staff nào đã xử lý

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;            // NULLABLE — Ghi chú của staff về quyết định

    @Column(nullable = false)
    private OffsetDateTime requestedAt;        // Thời điểm khách gửi yêu cầu

    @Column(nullable = false)
    private OffsetDateTime shopResponseDueAt;  // Hạn shop phải phản hồi = requestedAt + 48h; đặt lại now + 48h khi khách nộp bằng chứng bổ sung

    private OffsetDateTime evidenceRequestedAt; // NULLABLE — Thời điểm shop yêu cầu bổ sung bằng chứng (tối đa 1 lần); khách có 48 giờ

    @Column(length = 500)
    private String evidenceRequestNote;       // NULLABLE — Shop ghi rõ cần bổ sung gì (vd: "Video mở hộp thấy rõ mã vận đơn")

    private OffsetDateTime resolvedAt;         // NULLABLE — Thời điểm staff ra quyết định

    private OffsetDateTime shopRejectedAt;     // NULLABLE — Thời điểm shop từ chối; mốc 48 giờ để khách khiếu nại lên sàn (Cron đóng yêu cầu nếu quá hạn)

    private OffsetDateTime returnDueAt;        // NULLABLE — Hạn khách bàn giao hàng hoàn cho ĐVVC = thời điểm duyệt + 7 ngày (chỉ khi APPROVE_WITH_RETURN)

    private OffsetDateTime returnShippedAt;    // NULLABLE — ĐVVC đã lấy hàng hoàn (vận đơn RETURN → PICKED_UP); từ mốc này Cron không còn đóng yêu cầu vì quá hạn

    private OffsetDateTime returnReceivedAt;   // NULLABLE — Hàng hoàn đã về kho shop (vận đơn RETURN → DELIVERED) → chuyển APPROVED để hoàn tiền

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Lần thay đổi gần nhất (@UpdateTimestamp) — truy vấn "yêu cầu không cập nhật quá 24 giờ" cho BI/SLA

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "refund_request_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua RefundRequestItem.refundRequestId
    private List<RefundRequestItem> items;    // Các sản phẩm cụ thể muốn trả

    @OneToMany
    @JoinColumn(name = "refund_request_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua RefundEvidence.refundRequestId
    private List<RefundEvidence> evidences;   // Ảnh/video bằng chứng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Shop đồng ý: hoàn tiền ngay (APPROVE_REFUND_ONLY) hoặc cho trả hàng (APPROVE_WITH_RETURN → chờ hàng hoàn) — UC-SUP-02 */
    public void approveByShop(RefundResolution mode, String note, OffsetDateTime now) {
        requireStatus(RefundStatus.REQUESTED);
        applyApproval(mode, now);
        this.resolutionNote = note;
    }

    /** Áp phương án duyệt: REFUND_ONLY chỉ được duyệt hoàn ngay (shop muốn lấy lại hàng thì phải từ chối / nhờ sàn); RETURN_AND_REFUND được chọn cả hai */
    private void applyApproval(RefundResolution mode, OffsetDateTime now) {
        if (this.requestType == RefundRequestType.REFUND_ONLY && mode == RefundResolution.APPROVE_WITH_RETURN)
            throw new IllegalArgumentException("Khách chọn hoàn tiền không trả hàng — không thể buộc trả hàng khi chấp thuận");
        this.resolutionMode = mode;
        this.resolvedAt = now;
        if (mode == RefundResolution.APPROVE_WITH_RETURN) {
            this.status = RefundStatus.AWAITING_RETURN_SHIPMENT;   // Service sinh vận đơn ShipmentTracking.forReturn(...)
            this.returnDueAt = now.plusDays(7);
        } else {
            this.status = RefundStatus.APPROVED;                   // hoàn ngay (UC-SUP-04)
        }
    }

    /** Shop từ chối — UC-SUP-02 */
    public void shopReject(String note, OffsetDateTime now) {
        requireStatus(RefundStatus.REQUESTED);
        this.status = RefundStatus.SHOP_REJECTED;
        this.resolutionNote = note;
        this.shopRejectedAt = now;   // khách có 48 giờ để khiếu nại lên sàn
    }

    /** Shop yêu cầu khách bổ sung bằng chứng (tạm dừng đồng hồ 48 giờ của shop) — UC-SUP-02 */
    public void requestMoreEvidence(String note, OffsetDateTime now) {
        requireStatus(RefundStatus.REQUESTED);
        if (this.evidenceRequestedAt != null) throw new IllegalStateException("Shop chỉ được yêu cầu bổ sung bằng chứng 1 lần");
        this.status = RefundStatus.AWAITING_EVIDENCE;
        this.evidenceRequestNote = note;
        this.evidenceRequestedAt = now;   // khách có 48 giờ để bổ sung
    }

    /** Khách đã tải bằng chứng bổ sung → quay về chờ shop — UC-SUP-01 */
    public void submitAdditionalEvidence(OffsetDateTime now) {
        requireStatus(RefundStatus.AWAITING_EVIDENCE);
        this.status = RefundStatus.REQUESTED;
        this.shopResponseDueAt = now.plusHours(48);   // shop có lại đủ 48 giờ
    }

    /** Cron: quá 48 giờ khách không bổ sung → đóng yêu cầu — UC-SUP-02 */
    public boolean closeIfEvidenceNotProvided(OffsetDateTime now) {
        if (this.status != RefundStatus.AWAITING_EVIDENCE || this.evidenceRequestedAt.plusHours(48).isAfter(now)) return false;
        this.status = RefundStatus.REJECTED;
        this.resolutionNote = "Đóng: khách không bổ sung bằng chứng trong 48 giờ";
        this.resolvedAt = now;
        return true;
    }

    /** Cron: quá shopResponseDueAt (48 giờ) shop không phản hồi → tự động chấp thuận theo đúng phương án khách đã chọn — UC-SUP-02 */
    public boolean autoApproveIfShopSilent(OffsetDateTime now) {
        if (this.status != RefundStatus.REQUESTED || this.shopResponseDueAt.isAfter(now)) return false;
        applyApproval(this.requestType == RefundRequestType.RETURN_AND_REFUND ? RefundResolution.APPROVE_WITH_RETURN : RefundResolution.APPROVE_REFUND_ONLY, now);
        this.resolutionNote = "Tự động chấp thuận: shop không phản hồi trong 48 giờ";
        return true;
    }

    /** ĐVVC đã lấy hàng hoàn từ khách (vận đơn RETURN → PICKED_UP) — UC-SUP-02 */
    public void markReturnShipped(OffsetDateTime now) {
        requireStatus(RefundStatus.AWAITING_RETURN_SHIPMENT);
        if (this.returnShippedAt == null) this.returnShippedAt = now;
    }

    /** Hàng hoàn đã về kho shop (vận đơn RETURN → DELIVERED) → sẵn sàng hoàn tiền — UC-SUP-02 */
    public void confirmReturnReceived(OffsetDateTime now) {
        requireStatus(RefundStatus.AWAITING_RETURN_SHIPMENT);
        if (this.returnShippedAt == null) throw new IllegalStateException("Chưa có ghi nhận ĐVVC lấy hàng hoàn");
        this.returnReceivedAt = now;
        this.status = RefundStatus.APPROVED;
    }

    /** Cron: quá returnDueAt (7 ngày) khách chưa giao hàng hoàn cho ĐVVC → RETURN_FAILED, đóng tranh chấp, giải ngân bình thường — UC-SUP-02 */
    public boolean failReturnIfOverdue(OffsetDateTime now) {
        if (this.status != RefundStatus.AWAITING_RETURN_SHIPMENT || this.returnShippedAt != null || this.returnDueAt.isAfter(now)) return false;
        this.status = RefundStatus.RETURN_FAILED;
        this.resolutionNote = "Đóng: khách không gửi hàng hoàn trong 7 ngày";
        return true;
    }

    /** Khách khiếu nại lên sàn (trong 48 giờ sau khi shop từ chối / không đồng ý yêu cầu bổ sung) hoặc shop chủ động nhờ sàn — UC-SUP-03 */
    public void escalateToDispute() {
        if (this.status != RefundStatus.REQUESTED && this.status != RefundStatus.SHOP_REJECTED && this.status != RefundStatus.AWAITING_EVIDENCE) throw new IllegalStateException("Không thể khiếu nại");
        this.status = RefundStatus.DISPUTED;
    }

    /** Cron: quá 48 giờ khách không khiếu nại → đóng yêu cầu (không hoàn tiền) — UC-SUP-02 */
    public boolean closeIfCustomerSilent(OffsetDateTime now) {
        if (this.status != RefundStatus.SHOP_REJECTED || this.shopRejectedAt.plusHours(48).isAfter(now)) return false;
        this.status = RefundStatus.REJECTED;
        this.resolutionNote = "Đóng: khách không khiếu nại trong 48 giờ sau khi shop từ chối";
        this.resolvedAt = now;
        return true;
    }

    /** Sàn phân xử chấp thuận hoàn tiền (chọn có / không thu hồi hàng) — UC-SUP-03 */
    public void approve(Long staffId, RefundResolution mode, String note, OffsetDateTime now) {
        requireStatus(RefundStatus.DISPUTED);
        applyApproval(mode, now);
        this.resolvedByStaffId = staffId; this.resolutionNote = note;
    }

    /** Sàn bác khiếu nại — UC-SUP-03 */
    public void reject(Long staffId, String note, OffsetDateTime now) {
        requireStatus(RefundStatus.DISPUTED);
        this.status = RefundStatus.REJECTED;
        this.resolvedByStaffId = staffId; this.resolutionNote = note; this.resolvedAt = now;
    }

    /** Đã hoàn đúng nguồn (chỉ một lần) — UC-SUP-04 */
    public void markRefunded() {
        requireStatus(RefundStatus.APPROVED);
        this.status = RefundStatus.REFUNDED;
    }

    /** Bảo vệ máy trạng thái */
    private void requireStatus(RefundStatus s) {
        if (this.status != s) throw new IllegalStateException("Yêu cầu hoàn không ở " + s);
    }
}

// 8.2. RefundRequestItem — Dòng sản phẩm muốn hoàn  —  Bảng: refund_request_items
@Entity
@Table(name = "refund_request_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundRequestItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id

    @Column(nullable = false)
    private Long orderItemId;                 // FK → order_items.id — hoàn dòng hàng nào của đơn

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng muốn trả (có thể ÍT HƠN số đã mua)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal itemRefundAmount;      // Số tiền hoàn cho dòng này (đã tính cả phân bổ giảm giá)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Số tiền hoàn của dòng (đã trừ chiết khấu phân bổ) — UC-SUP-01 */
    public BigDecimal amount() {
        return this.itemRefundAmount;
    }
}

// 8.3. RefundEvidence — Bằng chứng khiếu nại  —  Bảng: refund_evidences
@Entity
@Table(name = "refund_evidences")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefundEvidence {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id

    @Column(nullable = false, length = 500)
    private String mediaUrl;                  // Link ảnh/video bằng chứng khách upload

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;              // IMAGE hoặc VIDEO

    @Column(nullable = false)
    private Integer displayOrder;             // Thứ tự hiển thị

    @Column(nullable = false)
    private Integer submissionRound;          // 1 = nộp khi tạo yêu cầu; 2 = nộp bổ sung theo yêu cầu của shop

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm upload
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bằng chứng là video — UC-SUP-01 */
    public boolean isVideo() {
        return this.mediaType == MediaType.VIDEO;
    }

    /** Bằng chứng nộp bổ sung theo yêu cầu của shop — UC-SUP-01 */
    public boolean isAdditional() {
        return this.submissionRound != null && this.submissionRound > 1;
    }
}
