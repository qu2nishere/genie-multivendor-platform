/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 05: Thanh toán VNPay / VietQR, Ký quỹ Escrow, Ví shop, Rút tiền & Sổ cái (Nhóm 6)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.3) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 06. NHÓM THANH TOÁN & KÝ QUỸ
 * Thành phần     : 8 @Entity, 0 @IdClass, 8 enum, 88 thuộc tính, 32 phương thức nghiệp vụ
 * Entity         : PaymentTransaction, PaymentRefund, EscrowLedger, VendorWallet, WalletTransaction, WithdrawalRequest, FinancialLedger, CodRemittance
 * Enum           : PaymentRefundStatus, FundingSource, EscrowStatus, WalletTransactionType, WalletReferenceType, WithdrawalStatus, FinancialEventType, CodRemittanceStatus
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d05_payment_escrow;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import com.genie.platform.d04_order_inventory.PaymentMethod;
import com.genie.platform.d04_order_inventory.PaymentStatus;
import com.genie.platform.d06_shipping_refund.CarrierType;

// ============================== ENUMS (8 — Phụ lục A) ==============================

public enum PaymentRefundStatus { PENDING, SUCCESS, FAILED }   // ← PaymentRefund.status

public enum FundingSource { ONLINE_GATEWAY, COD_CASH }   // ← EscrowLedger.fundingSource (ONLINE_GATEWAY gồm VNPay và VietQR qua SePay; mở rộng: WALLET, SPLIT)

public enum EscrowStatus {                                                                     // ← EscrowLedger.status
    PENDING_FUNDING,  // Đã tạo record nhưng chưa nhận đủ tiền (COD: GHN chưa đối soát)
    HELD,             // Đã fund đầy đủ, đang giữ an toàn
    FROZEN,           // Admin chủ động đóng băng (nghi vấn gian lận, vi phạm sàn)
    RELEASED,         // Đã giải ngân cho vendor
    REFUNDED,         // Đã hoàn tiền cho khách
    DISPUTED          // Tự động khóa do phát sinh yêu cầu đổi trả / khiếu nại (RefundRequest)
}

public enum WalletTransactionType {                                                            // ← WalletTransaction.type (mở rộng: CREDIT_TOPUP, DEBIT_PURCHASE, DEBIT_AD_SPEND, DEBIT_SUBSCRIPTION_FEE, DEBIT_TAX_WITHHOLDING, DEBIT_COSMETIC_PURCHASE)
    CREDIT_ESCROW_RELEASE, DEBIT_REFUND, DEBIT_WITHDRAWAL, CREDIT_ADJUSTMENT, DEBIT_ADJUSTMENT
}

public enum WalletReferenceType { ORDER, WITHDRAWAL, REFUND, ADJUSTMENT }   // ← WalletTransaction.referenceType (mở rộng: SUBSCRIPTION, AD_CAMPAIGN, COSMETIC, TAX)

public enum WithdrawalStatus { PENDING, APPROVED, PROCESSING, COMPLETED, REJECTED }   // ← WithdrawalRequest.status

public enum CodRemittanceStatus { WAITING, DONE }   // ← CodRemittance.status (chờ bảng kê / đã đối soát — TLCN v1.3)

public enum FinancialEventType {                                                               // ← FinancialLedger.eventType (10 giá trị TLCN; mở rộng: B2B_WHOLESALE_DISBURSEMENT, TAX_WITHHOLDING, LOGISTICS_FEE, SUBSCRIPTION_FEE, AD_REVENUE, COSMETIC_REVENUE, BNPL_MERCHANT_FEE, COSMETIC_DEFERRED, SHIPPER_COD_REMIT, SHIPPER_DEPOSIT_DEDUCTION, AFFILIATE_COMMISSION)
    ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE,
    PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE
}

// ============================== ENTITIES (7) ==============================

// 6.1. PaymentTransaction — Giao dịch thanh toán  —  Bảng: payment_transactions
@Entity
@Table(name = "payment_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PaymentTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    private Long orderId;                     // NULLABLE — FK → orders.id (khi thanh toán cho 1 đơn lẻ)

    private Long checkoutSessionId;           // NULLABLE — FK → checkout_sessions.id (khi trả gộp nhiều shop)

    @Column(unique = true, nullable = false, length = 255)
    private String idempotencyKey;            // KHOÁ CHỐNG TRÙNG — chìa khoá ngăn trừ tiền 2 lần

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền giao dịch

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // Tổng đã hoàn (cho phép hoàn nhiều lần từng phần)

    @Column(length = 255)
    private String gatewayTransactionId;      // NULLABLE — VNPAY = vnp_TransactionNo; VIETQR = `id` giao dịch SePay (UNIQUE — chống trùng webhook); COD = NULL

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;    // NULLABLE — VNPAY / MOMO / ZALOPAY / VIETQR / INTERNAL_WALLET / COD

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;             // PENDING / REQUIRES_ACTION / SUCCESS / FAILED / EXPIRED / REFUNDED / PARTIALLY_REFUNDED / CANCELLED

    @Column(columnDefinition = "TEXT")
    private String webhookPayload;            // NULLABLE — JSON THÔ cổng thanh toán gửi về, giữ nguyên để đối soát

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống xử lý song song 1 giao dịch

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm khởi tạo giao dịch
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Ghi nhận IPN thành công (idempotent) — UC-PAY-03 */
    public void markSuccess(String gatewayTxId, String payload) {
        if (this.status == PaymentStatus.SUCCESS) return;   // IPN lặp → idempotent
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.REQUIRES_ACTION) throw new IllegalStateException("Giao dịch đã kết thúc: " + this.status);
        this.status = PaymentStatus.SUCCESS;
        this.gatewayTransactionId = gatewayTxId;
        this.webhookPayload = payload;
    }

    /** Ghi nhận thất bại (service gọi Order.markPaymentFailed) — UC-PAY-03 */
    public void markFailed(String payload) {
        if (this.status == PaymentStatus.SUCCESS) throw new IllegalStateException("Không ghi đè giao dịch đã thành công");
        this.status = PaymentStatus.FAILED;
        this.webhookPayload = payload;
    }

    /** Số tiền còn có thể hoàn — UC-SUP-04 */
    public BigDecimal refundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }

    /** Cập nhật sau khi hoàn qua cổng — UC-SUP-04 */
    public void recordRefund(BigDecimal amt) {
        if (this.status != PaymentStatus.SUCCESS && this.status != PaymentStatus.PARTIALLY_REFUNDED) throw new IllegalStateException("Chỉ hoàn giao dịch đã thành công");
        if (amt.compareTo(refundableAmount()) > 0) throw new IllegalArgumentException("Vượt số tiền có thể hoàn");
        this.refundedAmount = this.refundedAmount.add(amt);
        this.status = refundableAmount().signum() == 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }
}

// 6.2. PaymentRefund — Giao dịch hoàn tiền qua cổng  —  Bảng: payment_refunds
@Entity
@Table(name = "payment_refunds")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PaymentRefund {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long paymentTransactionId;        // FK → payment_transactions.id — hoàn cho giao dịch gốc nào

    @Column(nullable = false)
    private Long refundRequestId;             // FK → refund_requests.id — xuất phát từ yêu cầu hoàn nào

    @Column(nullable = false, length = 255)
    private String gatewayRefundId;           // Mã hoàn tiền bên cổng thanh toán cấp

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền hoàn của lần này

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentRefundStatus status;       // PENDING / SUCCESS / FAILED
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Cổng xác nhận hoàn tiền — UC-SUP-04 */
    public void markSuccess(String gatewayRefundId) {
        this.status = PaymentRefundStatus.SUCCESS;
        this.gatewayRefundId = gatewayRefundId;
    }

    /** Hoàn tiền thất bại — chuyển xử lý tay — UC-SUP-04 */
    public void markFailed() {
        this.status = PaymentRefundStatus.FAILED;
    }
}

// 6.3. EscrowLedger — Sổ ký quỹ (tiền tạm giữ)  —  Bảng: escrow_ledgers
@Entity
@Table(name = "escrow_ledgers")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class EscrowLedger {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long orderId;                     // FK → orders.id, UNIQUE — mỗi đơn đúng 1 bản ghi ký quỹ

    @Column(nullable = false)
    private Long vendorId;                    // FK → users.id — tiền này sẽ về shop nào

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;           // grossAmount = Order.subTotal - Order.discountAmount (KHÔNG gồm shippingFee)

    // Không có cột commissionRate — một đơn có thể gồm nhiều ngành hàng với tỷ lệ khác nhau; tỷ lệ chụp ở từng OrderItem.commissionRate

    @Column(name = "commission_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionFee;         // Tiền hoa hồng sàn thu = Σ OrderItem.commissionAmount của đơn (khớp tuyệt đối từng dòng)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal platformShippingFee;   // Phần phí ship sàn trả hộ

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal shopShippingSubsidy;   // Phần phí ship shop tự hỗ trợ khách

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;             // SỐ TIỀN THỰC shop được nhận sau tất cả khoản trừ

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO; // Tổng tiền hàng đã hoàn một phần cho khách (mặc định 0) — grossAmount gốc = grossAmount + refundedAmount

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FundingSource fundingSource;      // NULLABLE (NULL tới lần nạp đầu) — WALLET | ONLINE_GATEWAY | COD_CASH | SPLIT (SPLIT = có từ 2 nguồn khác nhau trở lên)

    @Builder.Default
    @Column(precision = 19, scale = 2, nullable = false)
    @ColumnDefault("0")
    private BigDecimal gatewayFundedAmount = BigDecimal.ZERO; // Phần trả online: VNPay hoặc chuyển khoản VietQR (xác nhận qua SePay)

    @Builder.Default
    @Column(precision = 19, scale = 2, nullable = false)
    @ColumnDefault("0")
    private BigDecimal codFundedAmount = BigDecimal.ZERO; // Tiền COD đã thu nộp (khi kế toán confirm SETTLED)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EscrowStatus status;              // PENDING_FUNDING | HELD | FROZEN | RELEASED | REFUNDED | DISPUTED

    private OffsetDateTime fundedAt;          // NULLABLE — Thời điểm fund đủ (chuyển PENDING_FUNDING → HELD)

    private OffsetDateTime codRemittedAt;     // NULLABLE — Thời điểm kế toán xác nhận đã nhận COD

    private OffsetDateTime releaseEligibleAt;  // NULLABLE — Thời điểm ĐỦ ĐIỀU KIỆN giải ngân (hết hạn khiếu nại)

    private OffsetDateTime releasedAt;         // NULLABLE — Thời điểm THỰC SỰ giải ngân

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo bút toán ký quỹ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nạp tiền vào ký quỹ — nguồn ONLINE_GATEWAY (VNPay / VietQR qua SePay) hoặc COD_CASH (GHN đã đối soát); chỉ HELD khi tổng đã nạp ≥ grossAmount — UC-PAY-05 */
    public void fund(BigDecimal amount, FundingSource source, OffsetDateTime now) {
        switch (source) {
            case ONLINE_GATEWAY -> this.gatewayFundedAmount = this.gatewayFundedAmount.add(amount);
            case COD_CASH -> this.codFundedAmount = this.codFundedAmount.add(amount);
        }
        this.fundingSource = source;   // MVP: mỗi đơn đúng 1 nguồn tiền (không chia nguồn)
        if (isFullyFunded() && this.status == EscrowStatus.PENDING_FUNDING) {
            this.status = EscrowStatus.HELD;   // chỉ HELD khi đủ grossAmount
            this.fundedAt = now;
        }
    }

    /** Tổng đã nạp từ mọi nguồn */
    private BigDecimal totalFunded() {
        return this.gatewayFundedAmount.add(this.codFundedAmount);
    }

    /** Đã nhận đủ tiền đơn — UC-PAY-05 */
    public boolean isFullyFunded() {
        return totalFunded().compareTo(this.grossAmount) >= 0;
    }

    /** Đặt mốc giải ngân 7 ngày sau DELIVERED — UC-ORD-12 */
    public void scheduleRelease(OffsetDateTime deliveredAt) {
        this.releaseEligibleAt = deliveredAt.plusDays(7);
    }

    /** Đủ điều kiện giải ngân (HELD, đủ tiền, qua 7 ngày) — UC-PAY-07 */
    public boolean isReleasable(OffsetDateTime now) {
        return this.status == EscrowStatus.HELD && isFullyFunded() && this.releaseEligibleAt != null && !this.releaseEligibleAt.isAfter(now);
    }

    /** Giải ngân cho shop — trả về số tiền netAmount hiện hành để Service cộng ví shop (VendorWallet + WalletTransaction) — UC-PAY-07 */
    public BigDecimal release(OffsetDateTime now) {
        if (!isReleasable(now)) throw new IllegalStateException("Chưa đủ điều kiện giải ngân");
        this.status = EscrowStatus.RELEASED;
        this.releasedAt = now;
        return this.netAmount;   // service ghi CREDIT_ESCROW_RELEASE đúng số này (đã trừ phần hoàn một phần nếu có)
    }

    /** Khóa tranh chấp khi khách gửi yêu cầu hoàn tiền (kể cả COD chưa đối soát) — UC-SUP-01 */
    public void dispute() {
        if (this.status != EscrowStatus.HELD && this.status != EscrowStatus.PENDING_FUNDING) throw new IllegalStateException("Không thể tranh chấp ở " + this.status);
        this.status = EscrowStatus.DISPUTED;
    }

    /** Sàn bác khiếu nại → quay về HELD (hoặc PENDING_FUNDING nếu COD chưa nộp) — UC-SUP-03 */
    public void releaseDisputeHold() {
        if (this.status != EscrowStatus.DISPUTED) return;
        this.status = isFullyFunded() ? EscrowStatus.HELD : EscrowStatus.PENDING_FUNDING;
    }

    /** Hoàn một phần: giảm grossAmount và commissionFee đúng bằng phần của các dòng bị trả (refundCommission = Σ OrderItem.commissionAmount × tỷ lệ số lượng trả), tính lại netAmount (khách + shop + sàn = tiền gốc); sau đó releaseDisputeHold() — UC-SUP-04 */
    public void applyPartialRefund(BigDecimal refundGross, BigDecimal refundCommission) {
        if (this.status != EscrowStatus.DISPUTED && this.status != EscrowStatus.HELD) throw new IllegalStateException("Không thể hoàn ở " + this.status);
        if (refundGross.signum() <= 0 || refundGross.compareTo(this.grossAmount) >= 0)
            throw new IllegalArgumentException("Hoàn toàn bộ dùng markRefunded(); hoàn một phần phải 0 < số tiền < grossAmount");
        if (refundCommission.signum() < 0 || refundCommission.compareTo(this.commissionFee) > 0)
            throw new IllegalArgumentException("Hoa hồng hoàn lại không hợp lệ");
        this.refundedAmount = this.refundedAmount.add(refundGross);
        this.grossAmount = this.grossAmount.subtract(refundGross);                 // doanh thu còn lại của shop
        this.commissionFee = this.commissionFee.subtract(refundCommission);        // bỏ đúng hoa hồng của các dòng bị trả (mỗi dòng giữ tỷ lệ ngành hàng riêng)
        this.netAmount = this.grossAmount.subtract(this.commissionFee).subtract(this.shopShippingSubsidy).max(BigDecimal.ZERO);
    }

    /** Hoàn tiền cho khách từ ký quỹ — UC-SUP-04 */
    public void markRefunded() {
        if (this.status != EscrowStatus.DISPUTED && this.status != EscrowStatus.HELD) throw new IllegalStateException("Không thể hoàn ở " + this.status);
        this.status = EscrowStatus.REFUNDED;
    }

    /** Admin phong tỏa tạm khi nghi ngờ gian lận (xử lý thủ công) — UC-SUP-03 */
    public void freeze() {
        if (this.status == EscrowStatus.RELEASED || this.status == EscrowStatus.REFUNDED) throw new IllegalStateException("Ký quỹ đã đóng");
        this.status = EscrowStatus.FROZEN;
    }
}

// 6.4. VendorWallet — Ví tiền của người bán  —  Bảng: vendor_wallets
@Entity
@Table(name = "vendor_wallets")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorWallet {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorId;                    // FK → users.id, UNIQUE — mỗi shop đúng 1 ví

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO; // Số dư KHẢ DỤNG — tiền shop có thể rút ngay

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingWithdrawal = BigDecimal.ZERO; // Số tiền đang bị GIỮ do có lệnh rút chờ xử lý

    @Column(nullable = false, length = 255)
    private String balanceHash;               // HMAC-SHA256(vendorId + balance + pendingWithdrawal + secret) — service verify trước mỗi UPDATE và ghi lại sau UPDATE, chống sửa trực tiếp CSDL

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — LÁ CHẮN CHỐNG RÚT TIỀN 2 LẦN

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo ví

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm biến động số dư gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Số dư khả dụng = số dư − đang chờ rút — UC-ADM-04 */
    public BigDecimal availableBalance() {
        return this.balance.subtract(this.pendingWithdrawal);
    }

    /** Cộng tiền (giải ngân Escrow, nạp ví) — UC-PAY-07 */
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    /** Trừ tiền (rút tiền, thu hồi khi hoàn tiền sau giải ngân, điều chỉnh giảm) — UC-ADM-04 */
    public void debit(BigDecimal amount) {
        if (availableBalance().compareTo(amount) < 0) throw new IllegalStateException("GNE-WAL-4001");
        this.balance = this.balance.subtract(amount);
    }

    /** Giữ tiền khi tạo lệnh rút — UC-ADM-04 */
    public void holdForWithdrawal(BigDecimal amount) {
        if (availableBalance().compareTo(amount) < 0) throw new IllegalStateException("GNE-WAL-4001");
        this.pendingWithdrawal = this.pendingWithdrawal.add(amount);
    }

    /** Chốt lệnh rút (chi tiền hoặc trả lại số dư) — UC-ADM-05 */
    public void settleWithdrawal(BigDecimal amount, boolean approved) {
        this.pendingWithdrawal = this.pendingWithdrawal.subtract(amount);
        if (approved) this.balance = this.balance.subtract(amount);
    }

    /** Phát hiện sửa số dư trực tiếp CSDL (so HMAC hằng thời gian) — UC-PAY-07 */
    public boolean verifyHash(String expectedHmac) {
        return MessageDigest.isEqual(this.balanceHash.getBytes(StandardCharsets.UTF_8), expectedHmac.getBytes(StandardCharsets.UTF_8));
    }
}

// 6.5. WalletTransaction — Lịch sử biến động ví  —  Bảng: wallet_transactions
@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class WalletTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long walletId;                    // FK → vendor_wallets.id — biến động của ví nào

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionType type;       // CREDIT_ESCROW_RELEASE / DEBIT_REFUND / DEBIT_WITHDRAWAL / CREDIT_ADJUSTMENT / DEBIT_ADJUSTMENT / CREDIT_TOPUP / DEBIT_PURCHASE (7 giá trị)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền biến động CÓ DẤU: DƯƠNG = CREDIT (cộng vào ví), ÂM = DEBIT (trừ khỏi ví) — quy ước chung của mọi sổ ví

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceBefore;         // SỐ DƯ TRƯỚC giao dịch — bất biến kiểm toán: balanceBefore + amount = balanceAfter

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;          // SỐ DƯ SAU giao dịch — dấu vết kiểm toán

    @Column(length = 255)
    private String referenceId;               // NULLABLE — ID của đối tượng liên quan (order_id, withdrawal_id...)

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WalletReferenceType referenceType; // NULLABLE — Loại đối tượng: ORDER / WITHDRAWAL / REFUND / ADJUSTMENT

    @Column(length = 500)
    private String note;                      // NULLABLE — Ghi chú, đặc biệt quan trọng với các khoản điều chỉnh tay

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo giao dịch trừ ví (append-only, gọi sau khi ví đã trừ; balanceBefore = số dư sau + số trừ) — UC-ADM-04 */
    public static WalletTransaction debit(VendorWallet wallet, WalletTransactionType type, BigDecimal amount, WalletReferenceType refType, String reference) {
        WalletTransaction t = new WalletTransaction();
        t.walletId = wallet.getId(); t.type = type; t.amount = amount.negate();
        t.balanceAfter = wallet.getBalance(); t.balanceBefore = t.balanceAfter.add(amount);
        t.referenceType = refType; t.referenceId = reference; t.createdAt = OffsetDateTime.now();
        return t;
    }

    /** Tạo giao dịch cộng ví (append-only, gọi sau khi ví đã cộng) — UC-PAY-07 */
    public static WalletTransaction credit(VendorWallet wallet, WalletTransactionType type, BigDecimal amount, WalletReferenceType refType, String reference) {
        WalletTransaction t = new WalletTransaction();
        t.walletId = wallet.getId(); t.type = type; t.amount = amount;
        t.balanceAfter = wallet.getBalance(); t.balanceBefore = t.balanceAfter.subtract(amount);
        t.referenceType = refType; t.referenceId = reference; t.createdAt = OffsetDateTime.now();
        return t;
    }

    /** Kiểm tra bất biến sổ ví (job đối soát đêm) — UC-PAY-07 */
    public boolean isConsistent() {
        return this.balanceBefore.add(this.amount).compareTo(this.balanceAfter) == 0;
    }
}

// 6.6. WithdrawalRequest — Yêu cầu rút tiền  —  Bảng: withdrawal_requests
@Entity
@Table(name = "withdrawal_requests")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class WithdrawalRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long walletId;                    // FK mềm theo ownerType → vendor_wallets.id / customer_wallets.id / shippers.id

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;                // Số tiền yêu cầu rút

    @Column(nullable = false, length = 255)
    private String bankAccountName;           // Tên chủ TK ngân hàng — CHỤP LẠI lúc gửi yêu cầu

    @Column(nullable = false, length = 50)
    private String bankAccountNumber;         // Số TK ngân hàng — chụp lại

    @Column(nullable = false, length = 100)
    private String bankName;                  // Tên ngân hàng — chụp lại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;          // PENDING / APPROVED / PROCESSING / COMPLETED / REJECTED

    @Column(length = 500)
    private String rejectionReason;           // NULLABLE — Lý do từ chối, để shop biết đường xử lý

    private Long processedByAdminId;          // NULLABLE — FK mềm → users.id, admin nào đã xử lý

    private Long walletTransactionId;         // NULLABLE — FK → wallet_transactions.id, bút toán tương ứng

    @Column(nullable = false)
    private OffsetDateTime requestedAt;        // Thời điểm shop gửi yêu cầu

    private OffsetDateTime processedAt;        // NULLABLE — Thời điểm admin xử lý xong

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nhân viên sàn phê duyệt lệnh rút (MVP duyệt một bước) — UC-ADM-05 */
    public void approve(Long adminId, OffsetDateTime now) {
        if (this.status != WithdrawalStatus.PENDING) throw new IllegalStateException("Lệnh rút không ở PENDING");
        this.status = WithdrawalStatus.APPROVED;
        this.processedByAdminId = adminId;
        this.processedAt = now;
    }

    /** Đã gửi lệnh chuyển khoản sang ngân hàng — UC-ADM-05 */
    public void startProcessing() {
        if (this.status != WithdrawalStatus.APPROVED) throw new IllegalStateException("Chưa phê duyệt");
        this.status = WithdrawalStatus.PROCESSING;
    }

    /** Ngân hàng xác nhận đã chuyển — UC-ADM-05 */
    public void complete(Long walletTransactionId) {
        if (this.status != WithdrawalStatus.APPROVED && this.status != WithdrawalStatus.PROCESSING) throw new IllegalStateException("Lệnh rút chưa duyệt");
        this.status = WithdrawalStatus.COMPLETED;
        this.walletTransactionId = walletTransactionId;
    }

    /** Từ chối, trả tiền về số dư — UC-ADM-05 */
    public void reject(String reason, Long adminId, OffsetDateTime now) {
        if (this.status == WithdrawalStatus.COMPLETED || this.status == WithdrawalStatus.REJECTED) throw new IllegalStateException("Lệnh rút đã kết thúc");
        this.status = WithdrawalStatus.REJECTED;
        this.rejectionReason = reason;
        this.processedByAdminId = adminId;
        this.processedAt = now;
    }
}

// 6.7. FinancialLedger — Sổ cái tài chính (chỉ ghi thêm)  —  Bảng: financial_ledger
@Entity
@Table(name = "financial_ledger")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class FinancialLedger {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;                         // Số THỨ TỰ bút toán — tăng dần tuyệt đối, không bao giờ tái sử dụng

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinancialEventType eventType;     // ESCROW_IN, COMMISSION, PLATFORM_VOUCHER, FREESHIP_COST, COIN_COST, ESCROW_RELEASE, PAYOUT_ACCRUE, PAYOUT_SETTLE, REFUND, GATEWAY_FEE

    @Column(nullable = false, length = 255)
    private String referenceId;               // ID đối tượng gốc: order_id / payout_id / refund_id

    private Long orderId;                     // NULLABLE — FK → orders.id khi bút toán gắn với một đơn (ESCROW_IN, COMMISSION, REFUND…) — lọc sổ cái theo đơn

    private Long vendorId;                    // NULLABLE — FK → users.id của shop khi bút toán gắn với một shop (giải ngân, phí) — đối soát theo shop

    @Column(nullable = false, length = 100)
    private String account;                   // TÊN TÀI KHOẢN kế toán bị tác động

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debit;                 // Số tiền ghi NỢ (bên trái)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal credit;                // Số tiền ghi CÓ (bên phải)

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;          // Số dư của tài khoản đó sau bút toán này

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm ghi sổ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bút toán (bảng chỉ INSERT, cấm UPDATE/DELETE) — UC-PAY-06 */
    public static FinancialLedger entry(FinancialEventType type, String account, BigDecimal debit, BigDecimal credit, String referenceId) {
        FinancialLedger l = new FinancialLedger();
        l.eventType = type; l.account = account; l.debit = debit; l.credit = credit;
        l.referenceId = referenceId; l.createdAt = OffsetDateTime.now();
        return l;
    }
}


@Entity
@Table(name = "cod_remittances")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CodRemittance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, unique = true, length = 30)
    private String remittanceCode;            // Mã phiên hiển thị: "COD-20260612"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarrierType carrierType;          // Hãng chuyển tiền thu hộ về sàn: GHN / GHTK / VIETTEL_POST / JNT_EXPRESS

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal receivedAmount;        // Số tiền thực nhận vào tài khoản sàn (SePay báo)

    @Column(nullable = false, unique = true, length = 100)
    private String bankReference;             // Mã giao dịch ngân hàng do SePay gửi — UNIQUE để webhook idempotent

    @Column(nullable = false)
    private OffsetDateTime receivedAt;         // Thời điểm tiền vào tài khoản sàn

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodRemittanceStatus status = CodRemittanceStatus.WAITING; // WAITING (chờ bảng kê) / DONE (đã đối soát)

    @Column(length = 255)
    private String statementFileName;         // NULLABLE — Tên tệp bảng kê CSV đã đối chiếu

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String linesJson;                 // NULLABLE — Kết quả đối chiếu từng dòng [{trackingCode, orderId, amount, fee, result}]

    private Long reconciledByStaffId;         // NULLABLE — FK mềm → users.id, nhân viên xác nhận (không đặt FK cứng, như kycVerifiedBy)

    private OffsetDateTime reconciledAt;       // NULLABLE — Thời điểm xác nhận đối soát

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm SePay báo tiền vào, tạo phiên
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Xác nhận đối soát khi bảng kê cân với số tiền nhận; service fund các EscrowLedger khớp — UC-LOG-03 */
    public void confirm(String fileName, String lines, Long staffId, OffsetDateTime now) {
        if (this.status != CodRemittanceStatus.WAITING) throw new IllegalStateException("Phiên đã đối soát");
        this.statementFileName = fileName;
        this.linesJson = lines;
        this.reconciledByStaffId = staffId;
        this.reconciledAt = now;
        this.status = CodRemittanceStatus.DONE;
    }
}
