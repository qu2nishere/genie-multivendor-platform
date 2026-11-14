package com.genie.vendor.shop.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import com.genie.common.domain.NameNormalizer;

/**
 * VendorProfile — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 * D2: bỏ trường điều hướng sang module khác: user — dùng cột Long *Id tương ứng.
 */
@Entity
@Table(name = "vendor_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class VendorProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id — UNIQUE, 1 tài khoản chỉ mở được 1 shop

    @Column(nullable = false, length = 255)
    private String shopName;                  // Tên cửa hàng hiển thị cho khách

    private Long categoryId;                  // NULLABLE — FK → categories.id, ngành hàng chính của shop

    @Enumerated(EnumType.STRING)
    private BusinessType businessType;        // NULLABLE — INDIVIDUAL (cá nhân) / HOUSEHOLD (hộ KD) / BUSINESS (doanh nghiệp)

    @Column(length = 255)
    private String identityCode;              // NULLABLE — Số CCCD/CMND của chủ shop (dùng đối chiếu KYC)

    @Column(length = 255)
    private String taxCode;                   // NULLABLE — Mã số thuế (bắt buộc với hộ KD và doanh nghiệp)

    @Column(length = 255)
    private String representative;            // NULLABLE — Họ tên người đại diện pháp luật

    @Column(length = 255)
    private String coverUrl;                  // NULLABLE — Ảnh bìa trang shop

    @Column(columnDefinition = "TEXT")
    private String shopDescription;           // NULLABLE — Mô tả giới thiệu về shop

    @Column(length = 500)
    private String logoUrl;                   // NULLABLE — Logo/avatar của shop

    // ---- Hồ sơ KYC: ảnh giấy tờ đã upload ----
    @Column(length = 500)
    private String idFrontUrl;                // NULLABLE — Ảnh MẶT TRƯỚC CCCD

    @Column(length = 500)
    private String idBackUrl;                 // NULLABLE — Ảnh MẶT SAU CCCD

    @Column(length = 500)
    private String licenseUrl;                // NULLABLE — Ảnh giấy phép kinh doanh

    @Column(length = 500)
    private String selfieUrl;                 // NULLABLE — Ảnh chân dung tự chụp, dùng so khớp khuôn mặt với CCCD

    // ---- Kết quả thẩm định KYC ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;              // NOT_SUBMITTED / PENDING / AI_PROCESSING / MANUAL_REVIEW / VERIFIED / REJECTED / SUPPLEMENT_REQUESTED (7 trạng thái)

    @Column(columnDefinition = "TEXT")
    private String kycRejectionReason;        // NULLABLE — Lý do bị từ chối, để vendor biết đường sửa

    @Enumerated(EnumType.STRING)
    private KycVerificationMethod kycVerificationMethod; // NULLABLE — OCR_AUTO (AI duyệt) / MANUAL_STAFF (người duyệt) / BIOMETRIC_FACIAL (sinh trắc khuôn mặt)

    private Long kycVerifiedBy;               // NULLABLE — ID staff đã duyệt (KHÔNG đặt FK, xem giải thích bên dưới)

    @Column(precision = 5, scale = 4)
    private BigDecimal commissionRate;        // NULLABLE — Tỷ lệ hoa hồng THƯƠNG LƯỢNG RIÊNG của shop (VD: 0.0300 = 3%). NULL = không có thỏa thuận riêng → dùng Category.commissionRate → mặc định toàn sàn 5% (xem effectiveCommissionRate())

    private OffsetDateTime kycSubmittedAt;     // NULLABLE — Thời điểm vendor nộp hồ sơ

    private OffsetDateTime kycVerifiedAt;      // NULLABLE — Thời điểm hồ sơ được duyệt

    // ---- Địa chỉ lấy hàng (kho của shop) — NULLABLE cho tới khi hoàn tất UC-VND-05 (đăng ký GHN) ----
    private Integer pickupProvinceId;         // NULLABLE — Mã tỉnh kho hàng (GHN) — kho lấy hàng duy nhất của shop trong MVP (UC-VND-05), dùng tính phí ship GHN

    @Column(length = 100)
    private String pickupProvince;            // NULLABLE — Tên tỉnh/thành kho hàng (cache hiển thị, như Address.province)

    private Integer pickupDistrictId;         // NULLABLE — Mã quận/huyện kho hàng (GHN)

    @Column(length = 100)
    private String pickupDistrict;            // NULLABLE — Tên quận/huyện kho hàng (cache hiển thị)

    @Column(length = 50)
    private String pickupWardCode;            // NULLABLE — Mã phường/xã kho hàng (GHN)

    @Column(length = 100)
    private String pickupWard;                // NULLABLE — Tên phường/xã kho hàng (cache hiển thị)

    @Column(length = 500)
    private String pickupAddressLine;         // NULLABLE — Địa chỉ chi tiết kho hàng để shipper tới lấy

    private Integer ghnShopId;                // NULLABLE — ID shop do GHN cấp sau khi gọi API đăng ký (UC-VND-05); NULL khi mới mở shop (UC-VND-01)

    // ---- Tài khoản nhận tiền ----
    @Column(length = 100)
    private String payoutBankName;            // NULLABLE — Tên ngân hàng nhận tiền rút

    @Column(length = 50)
    private String payoutBankAccountNumber;   // NULLABLE — Số tài khoản ngân hàng

    @Column(length = 255)
    private String payoutBankAccountName;     // NULLABLE — Tên chủ tài khoản ngân hàng

    private OffsetDateTime payoutAccountVerifiedAt; // NULLABLE — Thời điểm xác minh tài khoản chính chủ (tên ngân hàng trả về khớp họ tên KYC). NULL = chưa được rút tiền; đổi tài khoản thì đặt lại NULL

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đăng ký shop

    // ---- Chỉ số hiển thị trang Shop & cấu hình vận hành (đối chiếu giao diện 2026-10-29) ----
    @Builder.Default
    @Column(nullable = false)
    private Boolean isMall = false;           // Huy hiệu "Chính hãng" / bộ lọc "Shop Chính Hãng": Staff gắn khi duyệt KYC doanh nghiệp chính hãng (UC-VND-04)

    @Builder.Default
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal ratingAvg = BigDecimal.ZERO; // Điểm đánh giá shop = trung bình sao các đánh giá đang hiển thị của mọi sản phẩm shop (UC-CAT-16)

    @Builder.Default
    @Column(nullable = false)
    private Integer ratingCount = 0;          // Số đánh giá dùng để tính ratingAvg

    @Builder.Default
    @Column(nullable = false)
    private Integer followerCount = 0;        // Số người theo dõi (đếm sẵn, đồng bộ với shop_follows — UC-SOC-06)

    @Builder.Default
    @Column(nullable = false)
    private Integer activeProductCount = 0;   // Số sản phẩm PUBLISHED (cập nhật khi đăng / ẩn / xóa sản phẩm)

    @Column(precision = 5, scale = 2)
    private BigDecimal chatResponseRate;      // NULLABLE — % hội thoại do khách mở được shop trả lời trong 24h, tính trên 30 ngày (job hằng ngày); NULL = chưa đủ dữ liệu

    private Integer avgResponseMinutes;       // NULLABLE — Thời gian phản hồi chat trung bình (phút) — hiển thị "phản hồi trong vài phút / vài giờ"

    @Builder.Default
    @Column(nullable = false)
    private Boolean preOrderEnabled = false;  // Cho phép đăng sản phẩm đặt trước (Pre-order) — UC-VND-21

    @Builder.Default
    @Column(nullable = false)
    private Boolean isFavorite = false;       // Huy hiệu "Shop Yêu Thích" do sàn gắn (đánh giá ≥ 4.8, phản hồi chat ≥ 90%) — UC-CAT-16 (TLCN v1.3)

    // ---- Chat tự động (UC-VND-06) ----
    @Builder.Default
    @Column(nullable = false)
    private Boolean autoReplyEnabled = false; // Bật/tắt tính năng trả lời tin nhắn tự động

    @Column(columnDefinition = "TEXT")
    private String autoReplyGreeting;         // NULLABLE — Câu chào tự động khi khách mở chat

    @Column(columnDefinition = "TEXT")
    private String autoReplyOffline;          // NULLABLE — Câu trả lời khi shop vắng mặt / ngoài giờ làm việc

    // ---- Chế độ tạm nghỉ (Vacation Mode — UC-VND-19) ----
    private OffsetDateTime vacationStartsAt;   // NULLABLE — Bắt đầu tạm nghỉ: sản phẩm vẫn hiển thị nhưng KHÔNG cho đặt mua; NULL = đang bán bình thường

    private OffsetDateTime vacationEndsAt;     // NULLABLE — Tự động mở bán lại (tối đa 30 ngày / lần)

    @Column(length = 500)
    private String vacationMessage;           // NULLABLE — Thông báo hiển thị trên trang shop và trả lời chat tự động khi tạm nghỉ

    // ===== QUAN HỆ =====
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nộp hồ sơ KYC — UC-VND-02 */
    public void submitKyc(OffsetDateTime now) {
        if (this.kycStatus == KycStatus.VERIFIED) throw new IllegalStateException("Đã xác minh");
        this.kycStatus = KycStatus.PENDING;
        this.kycSubmittedAt = now;
    }

    /** Đẩy hồ sơ vào hàng đợi AI Worker OCR/FaceMatch — UC-VND-03 */
    public void startAiProcessing() {
        if (this.kycStatus != KycStatus.PENDING) throw new IllegalStateException("Chỉ xử lý AI khi hồ sơ PENDING");
        this.kycStatus = KycStatus.AI_PROCESSING;
    }

    /** AI nghi ngờ / điểm tin cậy thấp → chuyển nhân viên duyệt tay — UC-VND-03 */
    public void routeToManualReview() {
        if (this.kycStatus != KycStatus.AI_PROCESSING) throw new IllegalStateException("Chỉ chuyển duyệt tay từ AI_PROCESSING");
        this.kycStatus = KycStatus.MANUAL_REVIEW;
    }

    /** Nhân viên yêu cầu bổ sung giấy tờ (không từ chối thẳng) — UC-VND-04 */
    public void requestSupplement(String note) {
        if (this.kycStatus != KycStatus.MANUAL_REVIEW) throw new IllegalStateException("Chỉ yêu cầu bổ sung khi đang duyệt tay");
        this.kycStatus = KycStatus.SUPPLEMENT_REQUESTED;
        this.kycRejectionReason = note;
    }

    /** Duyệt KYC — UC-VND-04 */
    public void approveKyc(Long staffId, KycVerificationMethod method, OffsetDateTime now) {
        this.kycStatus = KycStatus.VERIFIED;
        this.kycVerificationMethod = method;
        this.kycVerifiedBy = staffId;
        this.kycVerifiedAt = now;
        this.kycRejectionReason = null;
    }

    /** Từ chối KYC kèm lý do — UC-VND-04 */
    public void rejectKyc(String reason) {
        this.kycStatus = KycStatus.REJECTED;
        this.kycRejectionReason = reason;
    }

    /** Đã cấu hình kho lấy hàng & đăng ký GHN (điều kiện đăng bán / nhận đơn) — UC-VND-05 */
    public boolean isPickupConfigured() {
        return this.ghnShopId != null && this.pickupDistrictId != null && this.pickupWardCode != null && this.pickupAddressLine != null;
    }

    /** Tỷ lệ hoa hồng áp dụng: thỏa thuận riêng của shop → ngành hàng → mặc định 5% — UC-PAY-05 */
    public BigDecimal effectiveCommissionRate(BigDecimal categoryRate) {
        if (this.commissionRate != null) return this.commissionRate;          // thương lượng riêng ghi đè ngành hàng
        if (categoryRate != null) return categoryRate;
        return new BigDecimal("0.0500");                                      // mặc định toàn sàn
    }

    /** Xác minh tài khoản nhận tiền chính chủ: tên chủ TK do ngân hàng trả về (tra cứu tên qua đối tác chi hộ / NAPAS) phải khớp họ tên KYC sau khi bỏ dấu, viết hoa, gộp khoảng trắng — UC-VND-07 */
    public boolean verifyPayoutAccount(String bankReturnedHolderName, String kycFullName, OffsetDateTime now) {
        boolean matched = NameNormalizer.normalize(bankReturnedHolderName).equals(NameNormalizer.normalize(kycFullName));
        this.payoutAccountVerifiedAt = matched ? now : null;
        return matched;
    }

    /** Đã có tài khoản nhận tiền chính chủ — điều kiện tạo lệnh rút (GNE-WAL-4032) — UC-ADM-04 */
    public boolean isPayoutAccountVerified() {
        return this.kycStatus == KycStatus.VERIFIED && this.payoutAccountVerifiedAt != null;
    }

    /** Cập nhật điểm đánh giá shop khi có đánh giá mới / bị ẩn — UC-CAT-16 */
    public void applyRating(BigDecimal newAvg, int newCount) {
        this.ratingAvg = newAvg;
        this.ratingCount = newCount;
    }

    /** Huy hiệu "Shop Yêu Thích": điểm ≥ 4.5, đủ 20 đánh giá và phản hồi chat ≥ 80% — UC-CAT-16 */
    public boolean isPreferredShop() {
        return this.ratingCount >= 20 && this.ratingAvg.compareTo(new BigDecimal("4.5")) >= 0
                && this.chatResponseRate != null && this.chatResponseRate.compareTo(new BigDecimal("80")) >= 0;
    }


    /** Bật chế độ tạm nghỉ (Service kiểm tra không còn đơn CONFIRMED/PREPARING chưa bàn giao) — UC-VND-19 */
    public void startVacation(OffsetDateTime from, OffsetDateTime until, String message, OffsetDateTime now) {
        if (from.isBefore(now.minusMinutes(1)) || !until.isAfter(from)) throw new IllegalArgumentException("Khoảng thời gian không hợp lệ");
        if (until.isAfter(from.plusDays(30))) throw new IllegalArgumentException("Tạm nghỉ tối đa 30 ngày mỗi lần");
        this.vacationStartsAt = from;
        this.vacationEndsAt = until;
        this.vacationMessage = message;
    }

    /** Kết thúc tạm nghỉ (shop chủ động hoặc Cron khi tới vacationEndsAt) — UC-VND-19 */
    public void endVacation() {
        this.vacationStartsAt = null;
        this.vacationEndsAt = null;
        this.vacationMessage = null;
    }

    /** Đang tạm nghỉ: chặn đặt mua sản phẩm của shop, chat tự động trả vacationMessage — UC-VND-19 */
    public boolean isOnVacation(OffsetDateTime now) {
        return this.vacationStartsAt != null && !now.isBefore(this.vacationStartsAt) && (this.vacationEndsAt == null || now.isBefore(this.vacationEndsAt));
    }

}
