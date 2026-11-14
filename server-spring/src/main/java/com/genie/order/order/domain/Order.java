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
import com.genie.common.domain.PaymentMethod;
import com.genie.common.domain.PaymentStatus;

/**
 * Order — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: vendorProfile — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "orders")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false, length = 32)
    private String orderCode;                 // Mã đơn công khai cho khách/CSKH tra cứu (vd: TR-20261015-000123) — Hash Index

    @Column(nullable = false)
    private Long customerId;                  // FK → users.id — người mua

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — shop bán (mỗi đơn CHỈ thuộc 1 shop)

    // Quan hệ ánh xạ tới VendorProfile qua users.id

    @Column(name = "checkout_session_id")   // D8: trùng tên logic với @JoinColumn chỉ-đọc
    private Long checkoutSessionId;           // NULLABLE — FK → checkout_sessions.id, phiên thanh toán gốc

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String shippingAddressSnapshotJson; // Ảnh chụp JSON địa chỉ giao hàng tại thời điểm đặt

    // ---- Các thành phần tiền ----
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subTotal;              // Tổng tiền hàng trước mọi khuyến mãi và phí ship

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;           // Phí vận chuyển gốc

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Tổng giảm giá từ voucher của SHOP

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformDiscountAmount = BigDecimal.ZERO; // Tổng giảm giá từ voucher của SÀN

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingDiscountAmount = BigDecimal.ZERO; // Tổng giảm từ mã miễn phí vận chuyển

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;            // SỐ TIỀN KHÁCH THỰC TRẢ lúc thanh toán (sau tất cả giảm giá) — BẤT BIẾN sau khi thanh toán; phần đã hoàn nằm ở refundedAmount, số còn giữ = netPaidAmount()

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // Tổng đã hoàn cho khách (mặc định 0) — UI hiển thị "Đã hoàn một phần" khi paymentStatus = PARTIALLY_REFUNDED

    // ---- Thanh toán, Nguồn vốn (Funding) & Trạng thái ----
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;     // NULLABLE — VNPAY / MOMO / ZALOPAY / VIETQR / INTERNAL_WALLET / COD

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentStatus paymentStatus;      // NULLABLE — Bản sao tóm tắt PaymentTransaction.status (denormalize để đọc nhanh)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FundingStatus fundingStatus;      // UNFUNDED | PENDING | FUNDED | FAILED — gán UNFUNDED khi tạo đơn

    private OffsetDateTime paymentConfirmedAt; // NULLABLE — Thời điểm KHÁCH đã trả tiền: online = IPN thành công; COD = lúc giao hàng, khách đưa tiền mặt cho tài xế

    private OffsetDateTime fundedAt;           // NULLABLE — Thời điểm tiền VÀO QUỸ ESCROW của sàn: online = paymentConfirmedAt; COD = Staff xác nhận GHN đã chuyển tiền thu hộ (UC-LOG-03) — hai mốc khác nhau với COD

    private OffsetDateTime deliveredAt;        // NULLABLE — Thời điểm giao hàng thành công (DELIVERED) để tính mốc 7 ngày

    private OffsetDateTime customerConfirmedAt;// NULLABLE — Thời điểm khách hàng bấm 'Đã nhận được hàng'

    private OffsetDateTime completedAt;        // NULLABLE — Thời điểm đơn chuyển COMPLETED (giải ngân Escrow)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;               // Trạng thái đơn con (Phụ lục A — OrderStatus, máy trạng thái ALLOWED bên dưới)
                                              // - Online Flow: PENDING_PAYMENT → PAID → CONFIRMED → PREPARING → READY_FOR_PICKUP → DELIVERING → DELIVERED → COMPLETED
                                              // - COD Flow:    PENDING_PAYMENT → CONFIRMED → PREPARING → READY_FOR_PICKUP → DELIVERING → DELIVERED → PAID → COMPLETED
                                              // - Ngoại lệ:     DELIVERY_FAILED, RETURNING, RETURNED_TO_VENDOR, CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED, RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, PARTIALLY_REFUNDED, REFUNDED

    @Column(length = 100)
    private String ghnOrderCode;              // NULLABLE — Mã vận đơn bên GHN, để tra cứu hành trình

    private OffsetDateTime shippingExpectedAt; // NULLABLE — Ngày dự kiến giao hàng

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống 2 luồng cùng đổi trạng thái đơn

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo đơn

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    @Transient
    private Boolean isReviewed;               // Trường tính toán — khách đã đánh giá đơn này chưa

    @Column(length = 500)
    private String buyerNote;                 // NULLABLE — "Lời nhắn cho Shop" khách nhập ở checkout (≤ 500 ký tự) — hiển thị ở UC-LOG-01

    @Builder.Default
    @Column(nullable = false)
    private Integer coinUsedCount = 0;          // Số Điểm khách đã dùng cho đơn này (UC-PRO-10)

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coinDiscountAmount = BigDecimal.ZERO; // Số tiền quy đổi tương ứng từ Điểm (1 Điểm = 1₫)

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "order_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua OrderItem.orderId
    private List<OrderItem> items;            // Các dòng sản phẩm trong đơn

    @OneToMany
    @JoinColumn(name = "order_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua OrderDiscount.orderId
    private List<OrderDiscount> discounts;    // Các khoản giảm giá đã áp

    // Bảng chuyển trạng thái hợp lệ — đồng bộ Document_Vi.md mục 3.4.2 (online & COD)
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.ofEntries(   // = Document_Vi.md 3.4.2
        Map.entry(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR, OrderStatus.AUTO_CANCELLED)),   // CANCELLED_BY_VENDOR: shop từ chối đơn COD chờ xác nhận / đơn online chưa trả tiền
        Map.entry(OrderStatus.PAID, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR, OrderStatus.COMPLETED, OrderStatus.RETURN_REQUESTED)),   // COMPLETED/RETURN_REQUESTED: chỉ COD đã giao
        Map.entry(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR)),
        Map.entry(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED, OrderStatus.CANCELLED_BY_VENDOR)),
        Map.entry(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.DELIVERING)),
        Map.entry(OrderStatus.DELIVERING, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED)),
        Map.entry(OrderStatus.DELIVERY_FAILED, EnumSet.of(OrderStatus.DELIVERING, OrderStatus.RETURNING)),
        Map.entry(OrderStatus.RETURNING, EnumSet.of(OrderStatus.RETURNED_TO_VENDOR)),
        Map.entry(OrderStatus.RETURNED_TO_VENDOR, EnumSet.of(OrderStatus.REFUNDED)),   // đơn đã trả online (VNPay / VietQR) bị GHN hoàn về shop → tự tạo RefundRequest REFUND_ONLY, hoàn theo UC-SUP-04
        Map.entry(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.PAID, OrderStatus.COMPLETED, OrderStatus.RETURN_REQUESTED)),
        Map.entry(OrderStatus.RETURN_REQUESTED, EnumSet.of(OrderStatus.RETURN_APPROVED, OrderStatus.RETURN_REJECTED)),
        Map.entry(OrderStatus.RETURN_APPROVED, EnumSet.of(OrderStatus.REFUNDED, OrderStatus.PARTIALLY_REFUNDED, OrderStatus.COMPLETED)),   // COMPLETED: khách không gửi hàng hoàn đúng hạn (RefundRequest.RETURN_FAILED)
        Map.entry(OrderStatus.PARTIALLY_REFUNDED, EnumSet.of(OrderStatus.RETURN_REQUESTED, OrderStatus.COMPLETED)),   // trả tiếp món khác trong hạn 7 ngày / hết hạn → giải ngân phần còn lại
        Map.entry(OrderStatus.RETURN_REJECTED, EnumSet.of(OrderStatus.COMPLETED)));
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bảo vệ máy trạng thái theo bảng ALLOWED (Document_Vi.md 3.4.2) */
    private void transitionTo(OrderStatus next) {
        // ALLOWED = hằng Map<OrderStatus, Set<OrderStatus>> khai báo ngay trên khối phương thức
        if (!ALLOWED.getOrDefault(this.status, Set.of()).contains(next))
            throw new IllegalStateException("Đơn " + this.orderCode + ": không thể chuyển " + this.status + " → " + next);
        this.status = next;
    }

    /** Đơn thanh toán khi nhận hàng */
    private boolean isCod() {
        return this.paymentMethod == PaymentMethod.COD;
    }

    /** Xác nhận đã thanh toán (online qua IPN / COD sau khi GHN đối soát tiền thu hộ) — đồng bộ paymentStatus & fundingStatus — UC-PAY-03 */
    public void markPaid(OffsetDateTime now) {
        if (isCod() && this.status != OrderStatus.DELIVERED) throw new IllegalStateException("COD chỉ PAID sau khi giao & kế toán đối soát");
        transitionTo(OrderStatus.PAID);   // online: PENDING_PAYMENT → PAID; COD: DELIVERED → PAID
        this.paymentStatus = PaymentStatus.SUCCESS;
        if (this.paymentConfirmedAt == null) this.paymentConfirmedAt = now;   // COD đã ghi lúc giao hàng (markDelivered)
        this.fundingStatus = FundingStatus.FUNDED;
        this.fundedAt = now;                                                  // tiền vào quỹ Escrow
    }

    /** Đồng bộ khi PaymentTransaction thất bại — UC-PAY-03 */
    public void markPaymentFailed() {
        if (this.status != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("Đơn không chờ thanh toán");
        this.paymentStatus = PaymentStatus.FAILED;   // trạng thái đơn giữ PENDING_PAYMENT, Sweeper hủy khi hết hạn
    }

    /** Shop xác nhận đơn (online đã PAID / COD chưa thu tiền) — UC-LOG-01 */
    public void confirmByVendor() {
        if (!isCod() && this.status != OrderStatus.PAID) throw new IllegalStateException("Đơn online phải PAID trước khi xác nhận");
        transitionTo(OrderStatus.CONFIRMED);   // online: PAID → CONFIRMED; COD: PENDING_PAYMENT → CONFIRMED
        if (isCod()) this.fundingStatus = FundingStatus.UNFUNDED;
    }

    /** Bắt đầu đóng gói — UC-LOG-01 */
    public void startPreparing() {
        transitionTo(OrderStatus.PREPARING);
    }

    /** In nhãn, chờ lấy hàng — UC-LOG-02 */
    public void readyForPickup() {
        transitionTo(OrderStatus.READY_FOR_PICKUP);
    }

    /** GHN đã lấy hàng (webhook picked) — UC-LOG-03 */
    public void startDelivering() {
        transitionTo(OrderStatus.DELIVERING);
    }

    /** Giao thành công (webhook delivered) — bắt đầu đếm 7 ngày (COD: chờ GHN đối soát) — UC-LOG-03 */
    public void markDelivered(OffsetDateTime now) {
        transitionTo(OrderStatus.DELIVERED);
        this.deliveredAt = now;
        if (isCod()) {
            this.paymentConfirmedAt = now;                  // khách đã trả tiền mặt cho tài xế
            this.fundingStatus = FundingStatus.PENDING;     // nhưng tiền chưa về quỹ Escrow (fundedAt đặt khi đối soát)
        }
    }

    /** Khách bấm "Đã nhận được hàng" (hoặc Cron tự xác nhận) — trước khi complete() — UC-ORD-11 */
    public void markCustomerConfirmed(OffsetDateTime now) {
        if (this.deliveredAt == null || (this.status != OrderStatus.DELIVERED && this.status != OrderStatus.PAID))
            throw new IllegalStateException("Chỉ xác nhận khi đơn đã giao");
        if (this.customerConfirmedAt == null) this.customerConfirmedAt = now;   // idempotent
    }

    /** GHN báo một lần giao thất bại — UC-LOG-03 */
    public void markDeliveryFailed() {
        transitionTo(OrderStatus.DELIVERY_FAILED);
    }

    /** GHN giao lại (tổng tối đa 3 lần giao theo chính sách GHN) — UC-LOG-03 */
    public void retryDelivery() {
        transitionTo(OrderStatus.DELIVERING);
    }

    /** GHN hoàn hàng về shop sau khi giao thất bại — UC-LOG-03 */
    public void startReturning() {
        transitionTo(OrderStatus.RETURNING);
    }

    /** Shop đã nhận lại hàng hoàn — UC-LOG-03 */
    public void markReturnedToVendor() {
        transitionTo(OrderStatus.RETURNED_TO_VENDOR);
    }

    /** Hủy đơn — chỉ 3 trạng thái hủy, chỉ trước khi bàn giao vận chuyển — UC-ORD-10 */
    public void cancel(OrderStatus cancelType) {
        if (cancelType != OrderStatus.CANCELLED && cancelType != OrderStatus.CANCELLED_BY_VENDOR && cancelType != OrderStatus.AUTO_CANCELLED)
            throw new IllegalArgumentException("Loại hủy không hợp lệ: " + cancelType);
        if (this.status == OrderStatus.PAID && this.deliveredAt != null) throw new IllegalStateException("GNE-ORD-4092");   // COD đã giao
        transitionTo(cancelType);
    }

    /** Khách mở yêu cầu trả hàng (đồng bộ RefundRequest.REQUESTED) — UC-SUP-01 */
    public void requestReturn() {
        if (this.deliveredAt == null) throw new IllegalStateException("Chỉ trả hàng sau khi đã giao");
        transitionTo(OrderStatus.RETURN_REQUESTED);   // online: DELIVERED; COD: DELIVERED hoặc PAID (đã đối soát); trả tiếp đợt sau: PARTIALLY_REFUNDED
    }

    /** Đồng bộ khi RefundRequest → APPROVED — UC-SUP-02 */
    public void approveReturn() {
        transitionTo(OrderStatus.RETURN_APPROVED);
    }

    /** Đồng bộ khi RefundRequest → REJECTED — UC-SUP-03 */
    public void rejectReturn() {
        transitionTo(OrderStatus.RETURN_REJECTED);
    }

    /** Đồng bộ khi RefundRequest → REFUNDED; cộng dồn refundedAmount: đủ grandTotal → REFUNDED, chưa đủ → PARTIALLY_REFUNDED (hoàn nhiều đợt được) — UC-SUP-04 */
    public void markRefunded(BigDecimal amount) {
        if (amount.signum() <= 0 || this.refundedAmount.add(amount).compareTo(this.grandTotal) > 0)
            throw new IllegalArgumentException("Số hoàn phải > 0 và tổng hoàn không vượt grandTotal");
        this.refundedAmount = this.refundedAmount.add(amount);
        boolean full = this.refundedAmount.compareTo(this.grandTotal) >= 0;
        transitionTo(full ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED);   // từ RETURN_APPROVED, hoặc RETURNED_TO_VENDOR (hoàn toàn bộ)
        this.paymentStatus = full ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;   // UI: "Đã hoàn một phần {refundedAmount}"
    }

    /** Số tiền khách đã trả còn nằm lại ở đơn (sau các lần hoàn) — UC-SUP-04 */
    public BigDecimal netPaidAmount() {
        return this.grandTotal.subtract(this.refundedAmount);
    }

    /** Hoàn tất (khách xác nhận hoặc tự động sau 7 ngày) — UC-ORD-12 */
    public void complete(OffsetDateTime now) {
        if (this.status == OrderStatus.DELIVERED && isCod()) throw new IllegalStateException("COD phải đối soát (PAID) trước khi hoàn tất");
        if (this.status == OrderStatus.PAID && this.deliveredAt == null) throw new IllegalStateException("Đơn chưa giao");
        transitionTo(OrderStatus.COMPLETED);   // online: DELIVERED; COD: PAID (sau giao); hoặc RETURN_REJECTED / PARTIALLY_REFUNDED / RETURN_APPROVED (trả hàng thất bại)
        this.completedAt = now;
    }

    /** Tính lại tổng thanh toán (không âm) — CHỈ trước khi thanh toán (giỏ/checkout); sau thanh toán grandTotal bất biến — UC-PRO-03 */
    public BigDecimal recalculateGrandTotal() {
        if (this.status != OrderStatus.PENDING_PAYMENT || this.paymentConfirmedAt != null)
            throw new IllegalStateException("grandTotal đã chốt khi thanh toán — hoàn tiền ghi vào refundedAmount");
        this.grandTotal = this.subTotal.add(this.shippingFee)
            .subtract(this.discountAmount).subtract(this.platformDiscountAmount)
            .subtract(this.shippingDiscountAmount).max(BigDecimal.ZERO);
        return this.grandTotal;
    }
}
