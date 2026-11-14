package com.genie.payment.escrow.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.ColumnDefault;

/**
 * EscrowLedger — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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
