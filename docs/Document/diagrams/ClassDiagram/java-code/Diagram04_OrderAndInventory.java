/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 04: Phiên thanh toán, Đơn hàng con theo shop & Giữ chỗ tồn kho (Nhóm 5)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.2) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 05. NHÓM ĐƠN HÀNG
 * Thành phần     : 5 @Entity, 0 @IdClass, 6 enum, 77 thuộc tính, 32 phương thức nghiệp vụ
 * Entity         : CheckoutSession, Order, OrderItem, OrderDiscount, StockReservation
 * Enum           : SessionStatus, OrderStatus, FundingStatus, StockReservationStatus, PaymentMethod, PaymentStatus
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d04_order_inventory;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;
import com.genie.platform.d01_identity.VendorProfile;
import com.genie.platform.d02_catalog_cart.ProductVariant;
import com.genie.platform.d03_voucher.FundingParty;
import com.genie.platform.d03_voucher.VoucherType;

// ============================== ENUMS (6 — Phụ lục A) ==============================

public enum SessionStatus { INITIATED, PAYMENT_PENDING, COMPLETED, FAILED, EXPIRED }   // ← CheckoutSession.status

public enum OrderStatus {                                                                      // ← Order.status (19 trạng thái — giữ đủ vì GHN cũng có giao thất bại / hoàn hàng)
    PENDING_PAYMENT,   // Online: chờ thanh toán | COD: chờ shop xác nhận (đã có đơn)
    PAID,              // Online: đã TT | COD: sàn đã nhận tiền thu hộ từ GHN (fund escrow HELD)
    CONFIRMED, PREPARING, READY_FOR_PICKUP,
    DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNING, RETURNED_TO_VENDOR,
    COMPLETED, RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED,
    PARTIALLY_REFUNDED, // đã hoàn một phần, phần còn lại vẫn giao dịch bình thường
    REFUNDED, CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED
}

public enum FundingStatus { UNFUNDED, PENDING, FUNDED, FAILED }   // ← Order.fundingStatus

public enum StockReservationStatus { ACTIVE, CONFIRMED, EXPIRED, RELEASED }   // ← StockReservation.status

public enum PaymentMethod { VNPAY, VIETQR, COD }   // ← PaymentTransaction.paymentMethod, Order.paymentMethod (VIETQR = chuyển khoản, xác nhận bằng webhook SePay; mở rộng: MOMO, ZALOPAY, INTERNAL_WALLET, BNPL)

public enum PaymentStatus {                                                                    // ← PaymentTransaction.status, Order.paymentStatus
    PENDING, REQUIRES_ACTION, SUCCESS, FAILED, EXPIRED, REFUNDED, PARTIALLY_REFUNDED, CANCELLED
}

// ============================== ENTITIES (5) ==============================

// 5.1. CheckoutSession — Phiên thanh toán  —  Bảng: checkout_sessions
@Entity
@Table(name = "checkout_sessions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CheckoutSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — khách đang thanh toán

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;           // TỔNG tiền của cả phiên (gộp nhiều shop)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;             // INITIATED / PAYMENT_PENDING / COMPLETED / FAILED / EXPIRED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String cartSnapshotJson;          // NULLABLE — Ảnh chụp JSON toàn bộ giỏ hàng lúc bắt đầu thanh toán

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Hạn chót thanh toán (thường 15 phút) — hết hạn sẽ nhả tồn kho

    private OffsetDateTime completedAt;        // NULLABLE — Thời điểm thanh toán thành công

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bắt đầu phiên

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "checkout_session_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Order.checkoutSessionId
    private List<Order> orders;               // Các đơn hàng được tách ra theo từng shop
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chuyển sang chờ thanh toán — UC-ORD-07 */
    public void markPaymentPending() {
        if (this.status != SessionStatus.INITIATED) throw new IllegalStateException("Sai trạng thái phiên");
        this.status = SessionStatus.PAYMENT_PENDING;
    }

    /** Thanh toán thành công toàn phiên — UC-PAY-03 */
    public void complete(OffsetDateTime now) {
        if (this.status != SessionStatus.INITIATED && this.status != SessionStatus.PAYMENT_PENDING) throw new IllegalStateException("Phiên không còn chờ thanh toán");
        this.status = SessionStatus.COMPLETED;
        this.completedAt = now;
    }

    /** Cổng báo thất bại — UC-PAY-03 */
    public void fail() {
        if (this.status == SessionStatus.PAYMENT_PENDING) this.status = SessionStatus.FAILED;
    }

    /** Hết hạn 15 phút — Sweeper nhả tồn kho — UC-ORD-08 */
    public void expire() {
        if (this.status == SessionStatus.INITIATED || this.status == SessionStatus.PAYMENT_PENDING) this.status = SessionStatus.EXPIRED;
    }
}

// 5.2. Order — Đơn hàng (theo từng shop)  —  Bảng: orders
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private VendorProfile vendorProfile;      // Profile shop bán hàng

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

// 5.3. OrderItem — Dòng sản phẩm trong đơn  —  Bảng: order_items
@Entity
@Table(name = "order_items")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — thuộc đơn hàng nào

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — biến thể đã mua

    @Column(length = 255)
    private String productNameSnapshot;       // NULLABLE — Tên sản phẩm CHỤP LẠI lúc mua (bất biến)

    @Column(length = 100)
    private String skuSnapshot;               // NULLABLE — Mã SKU chụp lại lúc mua

    @Column(nullable = false)
    private Integer quantity;                 // Số lượng mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;             // Đơn giá CHỐT tại thời điểm mua

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;             // Thành tiền dòng = unitPrice × quantity

    // ---- Phân bổ giảm giá xuống từng dòng ----
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal proratedDiscount = BigDecimal.ZERO; // Phần giảm giá chung được chia tỷ lệ cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformDiscount = BigDecimal.ZERO; // Phần giảm do SÀN tài trợ, phân bổ cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shopDiscount = BigDecimal.ZERO; // Phần giảm do SHOP tài trợ, phân bổ cho dòng này

    // ---- Kết toán tài chính cho dòng này ----
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;           // Doanh thu gộp của dòng, dùng làm căn cứ tính hoa hồng

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;        // Tỷ lệ hoa hồng CHỤP LẠI lúc bán. VD 0.0550 = 5.5%

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionAmount;      // Số tiền hoa hồng sàn thu = grossAmount × commissionRate

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal vendorReceivable;      // Số tiền shop thực nhận cho dòng này

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal coinDiscount = BigDecimal.ZERO; // Phần giảm từ Điểm, phân bổ cho dòng này (sàn chịu — shop vẫn nhận đủ)

    // ===== QUAN HỆ =====
    @ManyToOne(optional = false)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    private ProductVariant productVariant;    // Biến thể sản phẩm
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Chụp tỷ lệ & tính hoa hồng dòng hàng — UC-PAY-05 */
    public void applyCommission(BigDecimal rate) {
        this.commissionRate = rate;
        this.commissionAmount = this.grossAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        this.vendorReceivable = this.grossAmount.subtract(this.commissionAmount);
    }

    /** Gán phần chiết khấu phân bổ (phần dư dồn dòng cuối) — UC-PRO-03 */
    public void allocateDiscount(BigDecimal prorated) {
        this.proratedDiscount = prorated;
    }
}

// 5.4. OrderDiscount — Chi tiết các mã đã áp cho đơn  —  Bảng: order_discounts
@Entity
@Table(name = "order_discounts")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class OrderDiscount {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id

    private Long voucherId;                   // NULLABLE — FK → vouchers.id. NULL khi là giảm giá từ Điểm

    @Column(length = 50)
    private String voucherCodeSnapshot;       // NULLABLE — Mã voucher CHỤP LẠI, phòng khi voucher bị xoá

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;        // Số tiền mã này đã giảm

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VoucherType voucherType;          // NULLABLE — PLATFORM / SHOP / FREESHIP / PAYMENT. NULL = dòng giảm giá từ Điểm (xem Order.coinDiscountAmount)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FundingParty fundedBy;            // NULLABLE — AI TRẢ TIỀN: PLATFORM hay VENDOR (enum, cùng enum với Voucher.sponsorType)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Voucher do sàn tài trợ (không trừ tiền shop) — UC-PRO-03 */
    public boolean isPlatformFunded() {
        return this.fundedBy == FundingParty.PLATFORM;
    }
}

// 5.5. StockReservation — Giữ chỗ tồn kho  —  Bảng: stock_reservations
@Entity
@Table(name = "stock_reservations")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class StockReservation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long productVariantId;            // FK → product_variants.id — giữ chỗ cho biến thể nào

    @Column(nullable = false)
    private Long orderId;                     // FK → orders.id — giữ cho đơn hàng nào

    @Column(nullable = false)
    private Integer reservedQuantity;         // Số lượng đang giữ

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockReservationStatus status;    // ACTIVE (đang giữ) / CONFIRMED (đã chốt) / EXPIRED / RELEASED

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống Sweeper và Webhook cùng đổi trạng thái 1 lượt giữ chỗ

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Hạn giữ chỗ — quá hạn sẽ tự nhả

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm bắt đầu giữ

    private OffsetDateTime releasedAt;         // NULLABLE — Thời điểm thực sự nhả chỗ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đã quá hạn giữ chỗ — UC-ORD-08 */
    public boolean isExpired(OffsetDateTime now) {
        return this.status == StockReservationStatus.ACTIVE && this.expiresAt.isBefore(now);
    }

    /** Chốt khi thanh toán thành công — UC-PAY-03 */
    public void confirm() {
        if (this.status != StockReservationStatus.ACTIVE) throw new IllegalStateException("GNE-INV-4093: giữ chỗ đã hết hạn");
        this.status = StockReservationStatus.CONFIRMED;   // @Version: IPN và Sweeper chỉ một bên thắng
    }

    /** Nhả giữ chỗ (idempotent) — UC-ORD-08 */
    public boolean release(OffsetDateTime now, boolean expired) {
        if (this.status != StockReservationStatus.ACTIVE) return false;   // đã CONFIRMED/RELEASED → không nhả lần 2
        this.status = expired ? StockReservationStatus.EXPIRED : StockReservationStatus.RELEASED;
        this.releasedAt = now;
        return true;   // true → service mới được giảm reservedCount
    }
}
