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

    private OffsetDateTime shopNameChangedAt;  // NULLABLE — Lần đổi tên shop gần nhất; đổi tối đa 1 lần / 30 ngày (UC-VND-21, V20)

    private Long categoryId;                  // NULLABLE — FK → categories.id, ngành hàng chính của shop

    @Enumerated(EnumType.STRING)
    private BusinessType businessType;        // NULLABLE — INDIVIDUAL (cá nhân) / HOUSEHOLD (hộ KD) / BUSINESS (doanh nghiệp)

    @Column(length = 255)
    private String identityCode;              // NULLABLE — Số CCCD/CMND của chủ shop (dùng đối chiếu KYC)

    @Column(length = 255)
    private String taxCode;                   // NULLABLE — Mã số thuế (bắt buộc với hộ KD và doanh nghiệp)

    @Column(length = 255)
    private String representative;            // NULLABLE — Họ tên người đại diện pháp luật

    @Column(length = 20)
    private String contactPhone;              // NULLABLE — SĐT liên hệ của shop (form Mở shop — API 9.1, V20)

    @Column(length = 255)
    private String contactEmail;              // NULLABLE — Email liên hệ của shop; OTP mở shop gửi tới email này (V20)

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String kycDocumentsMetaJson;      // NULLABLE — Tên tệp + dung lượng gốc từng giấy tờ, vd {"ID_FRONT":{"file_name":"cccd.jpg","size_bytes":2202009}}; màn KYC chỉ hiện thông tin này, không trả URL ảnh (V20)

    @Builder.Default
    @Column(nullable = false)
    private Boolean kycBiometricConsent = false; // Đồng ý xử lý dữ liệu sinh trắc (so khớp khuôn mặt); false → luôn chuyển nhân viên đối chiếu (UC-VND-02, V20)

    // ---- Kết quả thẩm định KYC ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;              // NOT_SUBMITTED / PENDING / AI_PROCESSING / MANUAL_REVIEW / VERIFIED / REJECTED / SUPPLEMENT_REQUESTED (7 trạng thái)

    @Column(columnDefinition = "TEXT")
    private String kycRejectionReason;        // NULLABLE — Lý do bị từ chối, để vendor biết đường sửa

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String kycSupplementDocumentsJson; // NULLABLE — Giấy tờ nhân viên yêu cầu bổ sung, vd ["ID_FRONT","BUSINESS_LICENSE"] (UC-VND-04, V20)

    private OffsetDateTime kycSupplementDeadlineAt; // NULLABLE — Hạn bổ sung (yêu cầu + 7 ngày); quá hạn → job chuyển REJECTED (V20)

    private OffsetDateTime kycManualReviewRequestedAt; // NULLABLE — Lúc người bán bấm "Yêu cầu nhân viên xem xét" (1 lần / hồ sơ; hồ sơ nộp lại mà AI kết luận FAIL thì vào MANUAL_REVIEW) (V20)

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

    @Column(length = 20)
    private String pickupPhone;               // NULLABLE — SĐT người liên hệ tại kho lấy hàng (gửi GHN — UC-VND-05, V20)

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

    /** Factory mở shop: KYC chưa nộp, chưa có hoa hồng riêng / kho / GHN — UC-VND-01 bước 4 */
    public static VendorProfile register(Long userId, String shopName, Long categoryId, BusinessType businessType,
                                         String representative, String contactPhone, String contactEmail) {
        return VendorProfile.builder()
                .userId(userId).shopName(shopName).categoryId(categoryId).businessType(businessType)
                .representative(representative).contactPhone(contactPhone).contactEmail(contactEmail)
                .kycStatus(KycStatus.NOT_SUBMITTED)
                .build();
    }

    /** Hồ sơ còn sửa / nộp được: chưa nộp, bị từ chối hoặc được yêu cầu bổ sung — UC-VND-02 */
    public boolean isKycEditable() {
        return this.kycStatus == KycStatus.NOT_SUBMITTED || this.kycStatus == KycStatus.REJECTED
                || this.kycStatus == KycStatus.SUPPLEMENT_REQUESTED;
    }

    /** Lưu nháp dữ liệu kê khai KYC (trường null = giữ nguyên), không đổi trạng thái — UC-VND-02 */
    public void saveKycDraft(String identityCode, String taxCode, String representative, String shopName,
                             BusinessType businessType, Boolean biometricConsent) {
        if (!isKycEditable()) throw new IllegalStateException("Hồ sơ KYC đang được xử lý hoặc đã duyệt");
        if (identityCode != null) this.identityCode = identityCode;
        if (taxCode != null) this.taxCode = taxCode.isBlank() ? null : taxCode;
        if (representative != null) this.representative = representative;
        if (shopName != null) this.shopName = shopName;
        if (businessType != null) this.businessType = businessType;
        if (biometricConsent != null) this.kycBiometricConsent = biometricConsent;
    }

    /** Gắn giấy tờ đã mã hóa (khóa đối tượng .enc; null = giữ bản cũ) + metadata hiển thị — UC-VND-02 */
    public void attachKycDocuments(String idFrontKey, String idBackKey, String licenseKey, String selfieKey,
                                   String documentsMetaJson) {
        if (!isKycEditable()) throw new IllegalStateException("Hồ sơ KYC đang được xử lý hoặc đã duyệt");
        if (idFrontKey != null) this.idFrontUrl = idFrontKey;
        if (idBackKey != null) this.idBackUrl = idBackKey;
        if (licenseKey != null) this.licenseUrl = licenseKey;
        if (selfieKey != null) this.selfieUrl = selfieKey;
        this.kycDocumentsMetaJson = documentsMetaJson;
    }

    /** Nộp / nộp lại hồ sơ KYC (chỉ khi chưa nộp, bị từ chối, cần bổ sung) — UC-VND-02 */
    public void submitKyc(OffsetDateTime now) {
        if (!isKycEditable()) throw new IllegalStateException("Hồ sơ KYC đang được xử lý hoặc đã duyệt");
        this.kycStatus = KycStatus.PENDING;
        this.kycSubmittedAt = now;
        this.kycRejectionReason = null;
        this.kycSupplementDocumentsJson = null;
        this.kycSupplementDeadlineAt = null;
    }

    /** Đẩy hồ sơ vào hàng đợi AI Worker OCR/FaceMatch — UC-VND-03 */
    public void startAiProcessing() {
        if (this.kycStatus != KycStatus.PENDING) throw new IllegalStateException("Chỉ xử lý AI khi hồ sơ PENDING");
        this.kycStatus = KycStatus.AI_PROCESSING;
    }

    /** AI nghi ngờ / điểm tin cậy thấp / AI lỗi hoặc quá hạn → chuyển nhân viên duyệt tay — UC-VND-03 */
    public void routeToManualReview() {
        if (this.kycStatus != KycStatus.AI_PROCESSING) throw new IllegalStateException("Chỉ chuyển duyệt tay từ AI_PROCESSING");
        this.kycStatus = KycStatus.MANUAL_REVIEW;
    }

    /** Hồ sơ bị từ chối → người bán yêu cầu nhân viên xem xét lại (1 lần / hồ sơ) — UC-VND-03 ràng buộc */
    public void requestManualReview(OffsetDateTime now) {
        if (this.kycStatus != KycStatus.REJECTED) throw new IllegalStateException("Chỉ yêu cầu xem xét khi hồ sơ bị từ chối");
        if (isManualReviewRequested()) throw new IllegalStateException("Đã yêu cầu nhân viên xem xét");
        this.kycManualReviewRequestedAt = now;
        this.kycStatus = KycStatus.MANUAL_REVIEW;
    }

    /** Đã từng yêu cầu nhân viên xem xét — UC-VND-03 */
    public boolean isManualReviewRequested() {
        return this.kycManualReviewRequestedAt != null;
    }

    /** Nhân viên yêu cầu bổ sung giấy tờ (không từ chối thẳng), hạn 7 ngày — UC-VND-04 */
    public void requestSupplement(String documentsJson, String note, OffsetDateTime deadline) {
        if (this.kycStatus != KycStatus.MANUAL_REVIEW) throw new IllegalStateException("Chỉ yêu cầu bổ sung khi đang duyệt tay");
        this.kycStatus = KycStatus.SUPPLEMENT_REQUESTED;
        this.kycRejectionReason = note;
        this.kycSupplementDocumentsJson = documentsJson;
        this.kycSupplementDeadlineAt = deadline;
    }

    /** Quá hạn bổ sung hồ sơ → từ chối (job hằng ngày) — UC-VND-04 */
    public void expireSupplement(String reason) {
        if (this.kycStatus != KycStatus.SUPPLEMENT_REQUESTED) throw new IllegalStateException("Hồ sơ không ở trạng thái chờ bổ sung");
        this.kycStatus = KycStatus.REJECTED;
        this.kycRejectionReason = reason;
        this.kycSupplementDeadlineAt = null;
    }

    /** Đưa hồ sơ đã quyết định trở lại hàng chờ duyệt tay (không chạy lại AI) — UC-VND-04 */
    public void requeueForReview() {
        if (this.kycStatus != KycStatus.VERIFIED && this.kycStatus != KycStatus.REJECTED
                && this.kycStatus != KycStatus.SUPPLEMENT_REQUESTED) {
            throw new IllegalStateException("Hồ sơ đang ở hàng chờ hoặc chưa nộp");
        }
        this.kycStatus = KycStatus.MANUAL_REVIEW;
        this.kycRejectionReason = null;
        this.kycSupplementDocumentsJson = null;
        this.kycSupplementDeadlineAt = null;
        this.kycVerificationMethod = null;
        this.kycVerifiedBy = null;
        this.kycVerifiedAt = null;
    }

    /** Duyệt KYC: AI tự duyệt (từ AI_PROCESSING) hoặc nhân viên duyệt (từ MANUAL_REVIEW) — UC-VND-03, UC-VND-04 */
    public void approveKyc(Long staffId, KycVerificationMethod method, OffsetDateTime now) {
        if (this.kycStatus != KycStatus.AI_PROCESSING && this.kycStatus != KycStatus.MANUAL_REVIEW) {
            throw new IllegalStateException("Hồ sơ không ở trạng thái chờ duyệt");
        }
        this.kycStatus = KycStatus.VERIFIED;
        this.kycVerificationMethod = method;
        this.kycVerifiedBy = staffId;
        this.kycVerifiedAt = now;
        this.kycRejectionReason = null;
        this.kycSupplementDocumentsJson = null;
        this.kycSupplementDeadlineAt = null;
    }

    /** Từ chối KYC kèm lý do (AI kết luận FAIL hoặc nhân viên từ chối) — UC-VND-03, UC-VND-04 */
    public void rejectKyc(String reason) {
        if (this.kycStatus != KycStatus.AI_PROCESSING && this.kycStatus != KycStatus.MANUAL_REVIEW) {
            throw new IllegalStateException("Hồ sơ không ở trạng thái chờ duyệt");
        }
        this.kycStatus = KycStatus.REJECTED;
        this.kycRejectionReason = reason;
    }

    /** Gắn / gỡ huy hiệu "Chính hãng" khi duyệt KYC doanh nghiệp — UC-VND-04 bước 8 */
    public void markAsMall(boolean mall) {
        this.isMall = mall;
    }

    /** Hoa hồng thương lượng riêng (tỷ lệ 0..1, vd 0.0300 = 3%); null = theo ngành hàng — UC-VND-04 */
    public void setNegotiatedCommission(BigDecimal rate) {
        if (rate != null && (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException("Tỷ lệ hoa hồng phải trong khoảng 0–100%");
        }
        this.commissionRate = rate;
    }

    /** Thời điểm được đổi tên lần tiếp theo; null = đổi được ngay — UC-VND-21 */
    public OffsetDateTime nextRenameAllowedAt(OffsetDateTime now) {
        if (this.shopNameChangedAt == null) return null;
        OffsetDateTime next = this.shopNameChangedAt.plusDays(30);
        return next.isAfter(now) ? next : null;
    }

    /** Đổi tên shop, tối đa 1 lần / 30 ngày (Service kiểm trùng tên / từ cấm trước) — UC-VND-21 */
    public void renameShop(String newName, OffsetDateTime now) {
        if (newName.equals(this.shopName)) return;
        if (nextRenameAllowedAt(now) != null) throw new IllegalStateException("Chỉ đổi tên shop 1 lần mỗi 30 ngày");
        this.shopName = newName;
        this.shopNameChangedAt = now;
    }

    /** Cập nhật mô tả, logo, ảnh bìa (khóa đối tượng trong avatar-assets) — UC-VND-21 */
    public void updateBranding(String description, String logoKey, String coverKey) {
        this.shopDescription = description;
        this.logoUrl = logoKey;
        this.coverUrl = coverKey;
    }

    /** Cấu hình tin nhắn trả lời tự động: bật mà cả hai nội dung đều rỗng → lỗi (GNE-VND-4222) — UC-VND-06 */
    public void configureAutoReply(boolean enabled, String greeting, String awayMessage) {
        boolean empty = (greeting == null || greeting.isBlank()) && (awayMessage == null || awayMessage.isBlank());
        if (enabled && empty) throw new IllegalArgumentException("Bật tự trả lời cần ít nhất một nội dung");
        this.autoReplyEnabled = enabled;
        this.autoReplyGreeting = greeting;
        this.autoReplyOffline = awayMessage;
    }

    /**
     * Lưu kho lấy hàng sau khi GHN đã cấp / cập nhật Shop ID — UC-VND-05. Địa giới 2 cấp từ 01/07/2025 (Tỉnh → Phường;
     * plan AUTH D25, plan Mở shop & KYC D16): không còn cấp quận nên {@code pickupDistrict*} đặt {@code null}.
     */
    public void updatePickup(String phone, Integer provinceId, String provinceName, String wardCode, String wardName,
                             String addressLine, Integer ghnShopId) {
        this.pickupPhone = phone;
        this.pickupProvinceId = provinceId;
        this.pickupProvince = provinceName;
        this.pickupDistrictId = null;
        this.pickupDistrict = null;
        this.pickupWardCode = wardCode;
        this.pickupWard = wardName;
        this.pickupAddressLine = addressLine;
        this.ghnShopId = ghnShopId;
    }

    /** Đã cấu hình kho lấy hàng & đăng ký GHN (điều kiện đăng bán / nhận đơn) — UC-VND-05 (2 cấp: tỉnh + phường) */
    public boolean isPickupConfigured() {
        return this.ghnShopId != null && this.pickupProvinceId != null && this.pickupWardCode != null
                && this.pickupAddressLine != null;
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

    /** Lưu tài khoản nhận tiền chỉ khi tên ngân hàng trả về khớp họ tên KYC; không khớp → giữ nguyên tài khoản cũ, trả false — UC-VND-07 */
    public boolean setPayoutAccount(String bankName, String accountNumber, String holderName, String bankReturnedHolderName,
                                    String kycFullName, OffsetDateTime now) {
        if (!NameNormalizer.normalize(bankReturnedHolderName).equals(NameNormalizer.normalize(kycFullName))) return false;
        this.payoutBankName = bankName;
        this.payoutBankAccountNumber = accountNumber;
        this.payoutBankAccountName = holderName;
        this.payoutAccountVerifiedAt = now;
        return true;
    }

    /** Xóa tài khoản nhận tiền (Service kiểm không còn lệnh rút đang chờ) — UC-VND-07 */
    public void clearPayoutAccount() {
        this.payoutBankName = null;
        this.payoutBankAccountNumber = null;
        this.payoutBankAccountName = null;
        this.payoutAccountVerifiedAt = null;
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
