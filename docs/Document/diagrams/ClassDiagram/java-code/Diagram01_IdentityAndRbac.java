/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 01: Định danh, Hồ sơ người dùng, KYC & Phân quyền nhân viên (Nhóm 1)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.3) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 01. NHÓM NGƯỜI DÙNG & ĐỊNH DANH
 * Thành phần     : 8 @Entity, 0 @IdClass, 10 enum, 154 thuộc tính, 35 phương thức nghiệp vụ
 * Entity         : User, CustomerProfile, Address, VendorProfile, AdminProfile, KycResult, RefreshToken, StaffPermission
 * Enum           : UserRole, AccountStatus, Gender, BusinessType, KycStatus, KycVerificationMethod, KycOverallResult, StaffPermissionEnum, StaffTitle, AddressLabel
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d01_identity;

import jakarta.persistence.*;
import lombok.*;
import com.genie.platform.common.util.NameNormalizer;
import java.math.BigDecimal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.*;
import java.util.*;
import static com.genie.platform.d01_identity.StaffPermissionEnum.*;

// ============================== ENUMS (10 — Phụ lục A) ==============================

public enum UserRole { CUSTOMER, VENDOR, STAFF, ADMIN }   // ← User.role (mở rộng: VENDOR_STAFF, SHIPPER, B2B_BUYER)

public enum AccountStatus { UNVERIFIED, ACTIVE, LOCKED, SUSPENDED, ANONYMIZED, DELETED }   // ← User.status (ANONYMIZED: xóa tài khoản — UC-AUTH-10; LOCKED để dành — khóa tạm dùng lockedUntil)

public enum Gender { MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY }   // ← CustomerProfile.gender

public enum AddressLabel { HOME, OFFICE }   // ← Address.label (Nhà riêng / Văn phòng — TLCN v1.3)

public enum BusinessType { INDIVIDUAL, HOUSEHOLD, BUSINESS }   // ← VendorProfile.businessType

public enum KycStatus { NOT_SUBMITTED, PENDING, AI_PROCESSING, MANUAL_REVIEW, VERIFIED, REJECTED, SUPPLEMENT_REQUESTED }   // ← VendorProfile.kycStatus

public enum KycVerificationMethod { OCR_AUTO, MANUAL_STAFF, BIOMETRIC_FACIAL }   // ← VendorProfile.kycVerificationMethod

public enum KycOverallResult { PASS, PARTIAL, FAIL }   // ← KycResult.overallStatus

public enum StaffPermissionEnum {                                                              // ← StaffPermission.permission (13 quyền TLCN; mở rộng: SUPPORT_CUSTOMER, VIEW_SYSTEM_LOG, MANAGE_FEATURE_FLAGS, RUN_EXPERIMENTS, MANAGE_B2B_CREDIT, MANAGE_DISPATCH, MANAGE_SHIPPERS, REVIEW_FRAUD, HANDLE_DSAR, HANDLE_TAKEDOWN)
    VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW, HANDLE_DISPUTE, PROCESS_PAYOUT,
    UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER, MANAGE_PLATFORM_NOTIFICATION, VIEW_AUDIT_LOG, MANAGE_USER, MANAGE_RBAC
}

public enum StaffTitle {                                                                       // ← AdminProfile.staffTitle (3 chức danh, mỗi chức danh mang bộ quyền mặc định; mở rộng: KYC_OFFICER, MARKETING_OFFICER, DISPATCHER, RISK_ANALYST, COMPLIANCE_OFFICER, B2B_CREDIT_OFFICER, TECH_OPS)
    MODERATOR(VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW),
    CS_SPECIALIST(VIEW_DASHBOARD, HANDLE_DISPUTE, MODERATE_REVIEW),
    FINANCE_OFFICER(VIEW_DASHBOARD, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER);
    private final Set<StaffPermissionEnum> defaults;
    StaffTitle(StaffPermissionEnum... p) { this.defaults = Set.of(p); }
    public Set<StaffPermissionEnum> defaultPermissions() { return defaults; }   // MANAGE_RBAC không thuộc mẫu nào
}

// ============================== ENTITIES (8) ==============================

// 1.1. User — Tài khoản đăng nhập  —  Bảng: users
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_oauth", columnNames = {"oauth_provider", "oauth_provider_id"}))
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính, ID định danh duy nhất của tài khoản

    @Column(unique = true, nullable = false, length = 255)
    private String email;                     // Email đăng nhập — DUY NHẤT toàn hệ thống, dùng làm username

    @Column(length = 255)
    private String password;                  // NULLABLE — Mật khẩu đã băm (Argon2id chuẩn OWASP 2024); NULL khi tài khoản tạo bằng Google / Apple và chưa "Thiết lập mật khẩu" (UC-AUTH-03, UC-AUTH-06)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;                    // 4 vai trò MVP: CUSTOMER, VENDOR, STAFF, ADMIN (Phụ lục A — nhóm 1). Spring Security cấp authority dạng ROLE_<giá trị>; RoleHierarchy: ROLE_VENDOR > ROLE_CUSTOMER, ROLE_ADMIN > ROLE_STAFF — chủ shop dùng cùng tài khoản để mua hàng ở shop khác

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;             // Trạng thái tài khoản: UNVERIFIED, ACTIVE, LOCKED, SUSPENDED, DELETED

    private OffsetDateTime emailVerifiedAt;    // NULLABLE — Thời điểm user bấm link xác thực email. NULL = chưa xác thực

    @Builder.Default
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer failedLoginAttempts = 0;  // Đếm số lần nhập sai mật khẩu liên tiếp. Vượt ngưỡng → tự khoá

    private OffsetDateTime lockedUntil;        // NULLABLE — Khoá tạm thời (do sai pass) đến thời điểm này

    private OffsetDateTime suspendedUntil;     // NULLABLE — Bị admin đình chỉ đến thời điểm này (vi phạm chính sách)

    @Column(length = 500)
    private String suspensionReason;          // NULLABLE — Lý do bị đình chỉ, hiển thị cho user biết

    @Version
    @Column(nullable = false)
    private Long version;                     // Optimistic Lock — chống 2 request cùng sửa 1 tài khoản

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo tài khoản

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    // ---- Đăng nhập mạng xã hội (UC-AUTH-03) ----
    @Column(length = 30)
    private String oauthProvider;             // NULLABLE — Nhà cung cấp OAuth: "google", "apple"

    @Column(length = 255)
    private String oauthProviderId;           // NULLABLE — ID của user bên phía nhà cung cấp OAuth (claim `sub`, cố định kể cả khi đổi email bên Google); UNIQUE cùng oauthProvider — một tài khoản Google / Apple chỉ gắn với một user

    // ---- Xóa tài khoản (UC-AUTH-10) — bổ sung bằng V14__auth_additions.sql (plan AUTH D7) ----
    private OffsetDateTime deletionRequestedAt; // NULLABLE — Thời điểm khách yêu cầu xóa; NULL = không có yêu cầu; quá 30 ngày thì cron ẩn danh


    // ===== QUAN HỆ =====
    @OneToOne(mappedBy = "user")
    private CustomerProfile customerProfile;  // NULLABLE (phía nghịch của OneToOne) — Hồ sơ khách hàng (chỉ có khi role = CUSTOMER)

    @OneToOne(mappedBy = "user")
    private VendorProfile vendorProfile;      // NULLABLE (phía nghịch của OneToOne) — Hồ sơ người bán (chỉ có khi role = VENDOR)

    @OneToOne(mappedBy = "user")
    private AdminProfile adminProfile;        // NULLABLE (phía nghịch của OneToOne) — Hồ sơ admin/staff (chỉ có khi role = ADMIN hoặc STAFF)

    @OneToMany
    @JoinColumn(name = "user_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua Address.userId
    private List<Address> addresses;          // Danh sách địa chỉ giao hàng của user

    @OneToMany
    @JoinColumn(name = "user_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua RefreshToken.userId
    private List<RefreshToken> refreshTokens; // Danh sách phiên đăng nhập đang hoạt động
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kiểm tra tài khoản đang bị khóa tạm do sai mật khẩu — UC-AUTH-02 */
    public boolean isLocked(OffsetDateTime now) {
        return this.lockedUntil != null && this.lockedUntil.isAfter(now);
    }

    /** Tăng đếm sai mật khẩu, đủ 5 lần thì khóa 15 phút — UC-AUTH-02 */
    public void recordFailedLogin(OffsetDateTime now) {
        this.failedLoginAttempts = this.failedLoginAttempts + 1;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = now.plusMinutes(15);   // khóa 15 phút
            this.failedLoginAttempts = 0;
        }
    }

    /** Reset bộ đếm khi đăng nhập đúng — UC-AUTH-02 */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /** Đình chỉ tài khoản vi phạm (chỉ Admin) — UC-ADM-03 */
    public void suspend(String reason, OffsetDateTime until) {
        if (this.status == AccountStatus.DELETED) throw new IllegalStateException("Tài khoản đã xóa");
        this.status = AccountStatus.SUSPENDED;
        this.suspensionReason = reason;
        this.suspendedUntil = until;   // NULL = đình chỉ vô thời hạn
    }

    /** Cron mở lại tài khoản khi hết thời hạn đình chỉ — UC-ADM-03 */
    public boolean reactivateIfSuspensionExpired(OffsetDateTime now) {
        if (this.status != AccountStatus.SUSPENDED || this.suspendedUntil == null || this.suspendedUntil.isAfter(now)) return false;
        this.status = AccountStatus.ACTIVE;
        this.suspensionReason = null;
        this.suspendedUntil = null;
        return true;
    }

    /** Admin mở khóa thủ công — UC-ADM-03 */
    public void reactivate() {
        if (this.status != AccountStatus.SUSPENDED && this.status != AccountStatus.LOCKED) throw new IllegalStateException("Tài khoản không bị khóa");
        this.status = AccountStatus.ACTIVE;
        this.suspensionReason = null;
        this.suspendedUntil = null;
        this.lockedUntil = null;
    }

    /** Ẩn danh hóa theo yêu cầu xóa tài khoản (sau thời gian chờ 30 ngày) — UC-AUTH-10 */
    public void anonymize() {
        this.status = AccountStatus.ANONYMIZED;
        this.email = "deleted_user_" + this.id + "@anonymized.local";
        this.password = null;
        this.oauthProvider = null;                 // plan AUTH D19: xóa cả nhà cung cấp
        this.oauthProviderId = null;
        this.deletionRequestedAt = null;
    }

    // ---- Phương thức bổ sung khi hiện thực AUTH (plan A03.01) — chi tiết ENTITY.md 1.1 / Phụ lục E ----
    public boolean hasPassword() { return this.password != null; }
    public void changePassword(String passwordHash) { this.password = passwordHash; }
    public void verifyEmail(OffsetDateTime now) { if (this.emailVerifiedAt == null) this.emailVerifiedAt = now; }
    public void changeEmail(String newEmail, OffsetDateTime now) { this.email = newEmail.trim().toLowerCase(java.util.Locale.ROOT); this.emailVerifiedAt = now; }
    public void linkIdentity(String provider, String sub) {
        if (this.oauthProvider != null) throw new IllegalStateException("Tài khoản đã gắn nhà cung cấp khác");
        this.oauthProvider = provider; this.oauthProviderId = sub;
    }
    public void unlinkIdentity() {
        if (!hasPassword()) throw new IllegalStateException("Chưa có mật khẩu");   // service trả GNE-AUTH-4093
        this.oauthProvider = null; this.oauthProviderId = null;
    }
    public void lockFor(java.time.Duration duration, OffsetDateTime now) { this.lockedUntil = now.plus(duration); }
    public void clearLock() { this.lockedUntil = null; this.failedLoginAttempts = 0; }
    public void requestDeletion(OffsetDateTime now) { if (this.deletionRequestedAt == null) this.deletionRequestedAt = now; }
    public void cancelDeletion() { this.deletionRequestedAt = null; }
    public boolean isPendingDeletion() { return this.deletionRequestedAt != null; }

}

// 1.2. CustomerProfile — Hồ sơ khách hàng  —  Bảng: customer_profiles
@Entity
@Table(name = "customer_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CustomerProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id — UNIQUE nên quan hệ là 1-1

    @Column(nullable = false, length = 255)
    private String fullName;                  // Họ tên đầy đủ của khách hàng

    @Column(length = 20)
    private String phoneNumber;               // NULLABLE — Số điện thoại liên hệ

    @Column(length = 500)
    private String avatarUrl;                 // NULLABLE — Link ảnh đại diện

    @Enumerated(EnumType.STRING)
    private Gender gender;                    // NULLABLE — MALE / FEMALE / OTHER / PREFER_NOT_TO_SAY

    @Column(columnDefinition = "DATE")
    private LocalDate dateOfBirth;            // NULLABLE — Ngày sinh (chỉ ngày, không giờ)

    @Column(columnDefinition = "TEXT")
    private String bio;                       // NULLABLE — Giới thiệu bản thân

    @Column(length = 5)
    private String preferredLanguage;         // NULLABLE — Ngôn ngữ ưa thích: "vi", "en" (chuẩn ISO 639-1)

    private Long defaultAddressId;            // NULLABLE — FK mềm → addresses.id, địa chỉ giao hàng mặc định


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo hồ sơ

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    private OffsetDateTime anonymizedAt;       // NULLABLE — Thời điểm hồ sơ bị ẩn danh hoá (xoá tài khoản — UC-AUTH-10)


    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Tài khoản đăng nhập tương ứng
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đặt địa chỉ giao hàng mặc định — UC-AUTH-09 */
    public void setDefaultAddress(Long addressId) {
        this.defaultAddressId = addressId;
    }

    /** Xóa PII hồ sơ, giữ ID cho khóa ngoại — UC-AUTH-10 */
    public void anonymize(OffsetDateTime now) {
        this.fullName = "Người dùng đã xóa";
        this.phoneNumber = null;
        this.avatarUrl = null;
        this.bio = null;
        this.dateOfBirth = null;
        this.anonymizedAt = now;
    }

}

// 1.3. Address — Địa chỉ giao hàng  —  Bảng: addresses
@Entity
@Table(name = "addresses")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Address {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — địa chỉ này thuộc về ai

    @Column(nullable = false, length = 255)
    private String recipientName;             // Tên người NHẬN hàng (có thể khác tên chủ tài khoản)

    @Column(nullable = false, length = 20)
    private String phoneNumber;               // SĐT người nhận — shipper gọi số này

    @Column(nullable = false, length = 500)
    private String addressLine;               // Địa chỉ chi tiết: số nhà, tên đường, toà nhà

    @Column(nullable = false, length = 100)
    private String ward;                      // TÊN phường/xã — dùng để HIỂN THỊ cho người đọc

    @Column(nullable = false, length = 50)
    private String wardCode;                  // MÃ phường/xã theo chuẩn GHN — dùng để GỌI API tính phí ship

    @Column(nullable = false, length = 100)
    private String district;                  // TÊN quận/huyện — để hiển thị

    @Column(nullable = false)
    private Integer districtId;               // MÃ quận/huyện theo GHN — để gọi API

    @Column(nullable = false, length = 100)
    private String province;                  // TÊN tỉnh/thành phố — để hiển thị

    @Column(nullable = false)
    private Integer provinceId;               // MÃ tỉnh/thành theo GHN — để gọi API

    private Boolean isDefault;                // NULLABLE — true = địa chỉ mặc định khi thanh toán

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressLabel label = AddressLabel.HOME; // Loại địa chỉ: HOME (Nhà riêng) / OFFICE (Văn phòng) — UC-AUTH-09 (TLCN v1.3)

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;        // Xoá mềm — true = user đã xoá, nhưng bản ghi vẫn giữ lại

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo địa chỉ

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu địa chỉ mặc định (service bỏ cờ ở địa chỉ khác cùng user) — UC-AUTH-09 */
    public void markAsDefault() {
        this.isDefault = true;
    }

    /** Xóa mềm địa chỉ — UC-AUTH-09 */
    public void softDelete() {
        this.isDeleted = true;
        this.isDefault = false;
    }
}

// 1.4. VendorProfile — Hồ sơ người bán (Shop)  —  Bảng: vendor_profiles
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

    private OffsetDateTime shopNameChangedAt;  // NULLABLE — Lần đổi tên shop gần nhất; đổi tối đa 1 lần / 30 ngày (UC-VND-21, V17)

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
    private String contactPhone;              // NULLABLE — SĐT liên hệ của shop (form Mở shop — API 9.1, V17)

    @Column(length = 255)
    private String contactEmail;              // NULLABLE — Email liên hệ của shop; OTP mở shop gửi tới email này (V17)

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
    private String kycDocumentsMetaJson;      // NULLABLE — Tên tệp + dung lượng gốc từng giấy tờ, vd {"ID_FRONT":{"file_name":"cccd.jpg","size_bytes":2202009}}; màn KYC chỉ hiện thông tin này, không trả URL ảnh (V17)

    @Builder.Default
    @Column(nullable = false)
    private Boolean kycBiometricConsent = false; // Đồng ý xử lý dữ liệu sinh trắc (so khớp khuôn mặt); false → luôn chuyển nhân viên đối chiếu (UC-VND-02, V17)

    // ---- Kết quả thẩm định KYC ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;              // NOT_SUBMITTED / PENDING / AI_PROCESSING / MANUAL_REVIEW / VERIFIED / REJECTED / SUPPLEMENT_REQUESTED (7 trạng thái)

    @Column(columnDefinition = "TEXT")
    private String kycRejectionReason;        // NULLABLE — Lý do bị từ chối, để vendor biết đường sửa

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String kycSupplementDocumentsJson; // NULLABLE — Giấy tờ nhân viên yêu cầu bổ sung, vd ["ID_FRONT","BUSINESS_LICENSE"] (UC-VND-04, V17)

    private OffsetDateTime kycSupplementDeadlineAt; // NULLABLE — Hạn bổ sung (yêu cầu + 7 ngày); quá hạn → job chuyển REJECTED (V17)

    private OffsetDateTime kycManualReviewRequestedAt; // NULLABLE — Lúc người bán bấm "Yêu cầu nhân viên xem xét" (1 lần / hồ sơ; hồ sơ nộp lại mà AI kết luận FAIL thì vào MANUAL_REVIEW) (V17)

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
    private String pickupPhone;               // NULLABLE — SĐT người liên hệ tại kho lấy hàng (gửi GHN — UC-VND-05, V17)

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
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Tài khoản đăng nhập của chủ shop
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

    /** Lưu kho lấy hàng sau khi GHN đã cấp / cập nhật Shop ID — UC-VND-05 */
    public void updatePickup(String phone, Integer provinceId, String provinceName, Integer districtId, String districtName,
                             String wardCode, String wardName, String addressLine, Integer ghnShopId) {
        this.pickupPhone = phone;
        this.pickupProvinceId = provinceId;
        this.pickupProvince = provinceName;
        this.pickupDistrictId = districtId;
        this.pickupDistrict = districtName;
        this.pickupWardCode = wardCode;
        this.pickupWard = wardName;
        this.pickupAddressLine = addressLine;
        this.ghnShopId = ghnShopId;
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

// 1.5. AdminProfile — Hồ sơ Quản trị viên / Nhân viên  —  Bảng: admin_profiles
@Entity
@Table(name = "admin_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class AdminProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id, UNIQUE (quan hệ 1-1)

    @Column(nullable = false, length = 255)
    private String fullName;                  // Họ tên nhân viên/quản trị viên

    @Column(length = 500)
    private String avatarUrl;                 // NULLABLE — Ảnh đại diện

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActiveModerator = false; // true = đang trong danh sách nhận phân công kiểm duyệt

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo hồ sơ

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật gần nhất

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StaffTitle staffTitle;            // NULLABLE — Chức danh của STAFF (3 chức danh); NULL với ADMIN. Chỉ dùng nạp bộ quyền mặc định — quyền thật ở staff_permissions


    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;                        // Tài khoản đăng nhập
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Bật/tắt trạng thái đang trực kiểm duyệt — UC-ADM-03 */
    public void toggleModerator(boolean active) {
        this.isActiveModerator = active;
    }

    /** Đổi chức danh — chỉ trả về bộ quyền mặc định để Admin xác nhận, không tự cấp/thu hồi quyền — UC-ADM-01, UC-ADM-02 */
    public Set<StaffPermissionEnum> changeTitle(StaffTitle newTitle) {
        this.staffTitle = newTitle;
        return newTitle == null ? Set.of() : newTitle.defaultPermissions();
    }

}

// 1.6. KycResult — Kết quả OCR/AI thẩm định giấy tờ  —  Bảng: kyc_results
@Entity
@Table(name = "kyc_results")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class KycResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long vendorProfileId;             // FK → vendor_profiles.id, UNIQUE (mỗi shop 1 bản kết quả; TLCN chỉ KYC người bán)

    // ===== Dữ liệu bóc tách: QR mặt trước (nguồn chính) + MRZ mặt sau (đối chiếu) =====
    @Column(length = 20)
    private String extractedIdNumber;         // NULLABLE — Số CCCD 12 số (từ QR; MRZ chứa số này ở trường tùy chọn dòng 1)

    @Column(length = 255)
    private String extractedFullName;         // NULLABLE — Họ tên có dấu (từ QR; MRZ dòng 3 là bản không dấu để đối chiếu)

    @Column(length = 20)
    private String extractedDob;              // NULLABLE — Ngày sinh (từ QR, dạng ddMMyyyy; lưu String để giữ dữ liệu thô)

    @Column(length = 10)
    private String extractedGender;           // NULLABLE — Giới tính "Nam"/"Nữ" (từ QR; MRZ ghi M/F)

    @Column(length = 100)
    private String extractedNationality;      // NULLABLE — Quốc tịch (từ MRZ, mã VNM)

    @Column(columnDefinition = "TEXT")
    private String extractedAddress;          // NULLABLE — Nơi thường trú (từ QR)

    @Column(length = 20)
    private String extractedIssueDate;        // NULLABLE — Ngày cấp (từ QR)

    @Column(length = 20)
    private String extractedExpiryDate;       // NULLABLE — Ngày hết hạn (từ MRZ dòng 2, YYMMDD)

    @Column(length = 255)
    private String extractedIssuer;           // NULLABLE — Nơi cấp; QR/MRZ không chứa, chỉ có khi người duyệt nhập tay

    // ===== Tầng 0 — kiểm tra dữ liệu (vài chục ms, không cần GPU) =====
    private Boolean qrCodeDetected;           // NULLABLE — Giải mã được QR mặt trước (thẻ gắn chip / thẻ căn cước mới đều có)

    private Boolean mrzValid;                 // NULLABLE — Đọc được 3 dòng MRZ mặt sau và đúng toàn bộ check digit ICAO 9303 (trọng số 7-3-1)

    private Boolean mrzQrMatch;               // NULLABLE — QR khớp MRZ: số thẻ, ngày sinh, giới tính, họ tên (bỏ dấu, viết hoa)

    private Boolean cccdNumberValid;          // NULLABLE — Số CCCD đúng cấu trúc (mã tỉnh 3 số + thế kỷ/giới tính + 2 số năm sinh) và khớp ngày sinh, giới tính

    private Boolean expiryRuleValid;          // NULLABLE — Ngày hết hạn khớp quy tắc theo tuổi đổi thẻ (14 / 25 / 40 / 60 tuổi hoặc không thời hạn — Luật Căn cước 2023)

    private Boolean duplicateIdFound;         // NULLABLE — Số CCCD đã gắn với một shop khác đang hoạt động

    // ===== Tầng 1 — sinh trắc (~1 giây, CPU ONNX) =====
    private Boolean faceMatchResult;          // NULLABLE — Khuôn mặt selfie khớp ảnh chân dung cắt từ mặt trước thẻ

    @Column(precision = 5, scale = 4)
    private BigDecimal faceMatchScore;        // NULLABLE — Điểm tương đồng cosine 0.0000 → 1.0000 (YuNet + SFace)

    @Column(precision = 5, scale = 4)
    private BigDecimal livenessScore;         // NULLABLE — Điểm người thật 0.0000 → 1.0000 (thử thách nháy mắt/quay đầu phía client + anti-spoof thụ động)

    private Boolean faceDuplicateFound;       // NULLABLE — Khuôn mặt trùng hồ sơ KYC của CCCD khác (MVP: so cosine với embedding đã lưu trong AI Worker — không dùng Vector DB)

    // ===== Điểm rủi ro tổng hợp =====
    private Integer riskScore;                // NULLABLE — Tổng điểm rủi ro 0..∞; mỗi tín hiệu cộng điểm thay vì chặn cứng

    @Column(columnDefinition = "TEXT")
    private String riskSignalsJson;           // NULLABLE — Mảng JSON các tín hiệu đã cộng điểm, vd [{"code":"FACE_SCORE_BORDERLINE","points":25}]

    @Builder.Default
    @Column(nullable = false)
    private Boolean sampledForReview = false; // true = hồ sơ điểm thấp bị lấy mẫu ngẫu nhiên (~5%) để người duyệt hậu kiểm

    // ===== Metadata =====
    @Column(columnDefinition = "TEXT")
    private String errors;                    // NULLABLE — Mảng JSON chứa thông báo lỗi của từng bước xử lý

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycOverallResult overallStatus;   // Kết luận tổng: PASS / PARTIAL / FAIL (VARCHAR + CHECK constraint trên PostgreSQL 16)

    private OffsetDateTime processedAt;        // NULLABLE — Thời điểm AI xử lý xong

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo bản ghi

    // ===== QUAN HỆ =====
    @OneToOne(optional = false)
    @JoinColumn(name = "vendor_profile_id", insertable = false, updatable = false)
    private VendorProfile vendorProfile;      // Hồ sơ shop tương ứng

    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Kết quả cần nhân viên duyệt thủ công — UC-VND-03 */
    public boolean requiresManualReview() {
        return this.overallStatus == KycOverallResult.PARTIAL;
    }

    /** AI kết luận đạt — UC-VND-03 */
    public boolean isPassed() {
        return this.overallStatus == KycOverallResult.PASS;
    }

    /** Chốt điểm rủi ro và kết luận: ≥ rejectFrom → FAIL; ≥ autoApproveBelow hoặc bị lấy mẫu → PARTIAL; còn lại PASS (ngưỡng mặc định 30 / 70, cấu hình genie.kyc.risk.*) — UC-VND-03 */
    public KycOverallResult applyRiskScore(int score, String signalsJson, boolean sampled, int autoApproveBelow, int rejectFrom, OffsetDateTime now) {
        this.riskScore = score;
        this.riskSignalsJson = signalsJson;
        this.sampledForReview = sampled && score < autoApproveBelow;
        if (score >= rejectFrom) this.overallStatus = KycOverallResult.FAIL;
        else if (score >= autoApproveBelow || this.sampledForReview) this.overallStatus = KycOverallResult.PARTIAL;
        else this.overallStatus = KycOverallResult.PASS;
        this.processedAt = now;
        return this.overallStatus;
    }
}

// 1.7. RefreshToken — Phiên đăng nhập  —  Bảng: refresh_tokens
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — token này của ai

    @Column(unique = true, nullable = false, length = 255)
    private String tokenHash;                 // BĂM của refresh token (KHÔNG lưu token gốc!)

    @Column(nullable = false, length = 255)
    private String deviceInfo;                // Thông tin thiết bị (User-Agent) — để user nhận ra phiên nào là của mình

    @Column(nullable = false, length = 45)
    private String ipAddress;                 // IP lúc đăng nhập — độ dài 45 để chứa được cả IPv6

    @Column(nullable = false)
    private OffsetDateTime expiresAt;          // Thời điểm token hết hạn (chuẩn dự án: 7 ngày, xoay vòng mỗi lần làm mới)

    private OffsetDateTime revokedAt;          // NULLABLE — Thời điểm bị thu hồi. NULL = còn hiệu lực

    @Column(length = 100)
    private String revokedReason;             // NULLABLE — Lý do thu hồi: "USER_LOGOUT", "PASSWORD_CHANGED", "ROTATED", "REUSE_DETECTED", "SUSPENDED", "ACCOUNT_DELETED", "ADMIN_REVOKED"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đăng nhập tạo ra phiên này

    // ---- Bổ sung bằng V14 (plan AUTH D6) ----
    @Column(nullable = false)
    private Long familyId;                    // Mã phiên ổn định qua các lần xoay vòng (= id token gốc) — id ở GET /me/sessions, claim sid

    private OffsetDateTime lastUsedAt;         // NULLABLE — Lần làm mới gần nhất
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Token còn dùng được (chưa thu hồi, chưa hết hạn 7 ngày) — UC-AUTH-02 */
    public boolean isUsable(OffsetDateTime now) {
        return this.revokedAt == null && this.expiresAt.isAfter(now);
    }

    /** Thu hồi khi xoay vòng / đăng xuất / phát hiện dùng lại — UC-AUTH-11 */
    public void revoke(String reason, OffsetDateTime now) {
        this.revokedAt = now;
        this.revokedReason = reason;
    }

    /** Ghi lần làm mới gần nhất — UC-AUTH-02/11 (bổ sung AUTH) */
    public void touch(OffsetDateTime now) { this.lastUsedAt = now; }
}

// 1.8. StaffPermission — Phân quyền chi tiết cho nhân viên  —  Bảng: staff_permissions
@Entity
@Table(name = "staff_permissions")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class StaffPermission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long staffUserId;                 // FK → users.id — nhân viên được cấp quyền

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffPermissionEnum permission;   // 13 quyền TLCN: VIEW_DASHBOARD, REVIEW_KYC, MANAGE_CATALOG, MODERATE_REVIEW, HANDLE_DISPUTE, PROCESS_PAYOUT, UPDATE_COMMISSION, VIEW_FINANCE, MANAGE_VOUCHER, MANAGE_PLATFORM_NOTIFICATION, VIEW_AUDIT_LOG, MANAGE_USER, MANAGE_RBAC

    @Column(nullable = false)
    private Long grantedByAdminId;            // FK → users.id — Admin nào đã cấp quyền này

    @Column(nullable = false)
    private OffsetDateTime grantedAt;          // Thời điểm cấp quyền

    private OffsetDateTime revokedAt;          // NULLABLE — Thời điểm thu hồi. NULL = quyền vẫn còn hiệu lực
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Quyền còn hiệu lực — UC-ADM-02 */
    public boolean isActive() {
        return this.revokedAt == null;
    }

    /** Thu hồi quyền — UC-ADM-02 */
    public void revoke(OffsetDateTime now) {
        this.revokedAt = now;
    }
}
