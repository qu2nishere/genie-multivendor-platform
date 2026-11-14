package com.genie.support.refund.domain;

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
 * RefundRequest — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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
