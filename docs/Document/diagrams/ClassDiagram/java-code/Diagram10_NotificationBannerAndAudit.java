/*
 * ============================================================================
 * GENIE — SƠ ĐỒ LỚP 10: Thông báo cá nhân, Thông báo sàn, Thông báo quản trị, Banner & Nhật ký hệ thống (Nhóm 10, 12)
 * ============================================================================
 * Nguồn duy nhất : TLCN/diagrams/ClassDiagram/ENTITY.md (ENTITY.md TLCN v1.3) — sinh tự động, KHÔNG sửa tay;
 *                  sửa ENTITY.md rồi chạy lại công cụ sinh sơ đồ lớp (tools/classdiagram) để giữ đồng bộ.
 * Nhóm thực thể  : 10. NHÓM THÔNG BÁO & NỘI DUNG SÀN, 12. NHÓM NHẬT KÝ HỆ THỐNG
 * Thành phần     : 6 @Entity, 1 @IdClass, 7 enum, 68 thuộc tính, 12 phương thức nghiệp vụ
 * Entity         : Notification, PlatformNotification, UserPlatformNotificationRead, HomeBanner, AdminNotification, SystemLog
 * Enum           : NotificationType, NotificationChannel, NotificationDeliveryStatus, PlatformNotificationTarget, BannerPosition, AdminNotificationType, AuditAction
 * @IdClass       : UserPlatformNotificationReadId
 * Import vào EA  : Code > Import Source Directory / Import Source File (ngôn ngữ Java),
 *                  chọn cả thư mục java-code để EA nối được quan hệ giữa các sơ đồ.
 * Quy ước        : Getter/Setter do Lombok sinh — chỉ phương thức nghiệp vụ là ngăn Operations;
 *                  enum lấy nguyên văn từ Phụ lục A; @IdClass dựng từ các trường @Id của entity.
 */
package com.genie.platform.d10_notification_banner_audit;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;

// ============================== ENUMS (7 — Phụ lục A) ==============================

public enum NotificationType {                                                                 // ← Notification.type (mở rộng: PRICE_DROP_ALERT, B2B_RFQ_QUOTED)
    ORDER_STATUS_CHANGED, KYC_APPROVED, KYC_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, ESCROW_RELEASED,
    WALLET_CREDITED, SYSTEM_ALERT, REFUND_PROCESSED, NEW_PLATFORM_VOUCHER, NEW_VENDOR_VOUCHER, NEW_ORDER_FOR_VENDOR,
    FLASH_SALE_REMINDER, NEW_DEVICE_LOGIN, PRODUCT_APPROVED, PRODUCT_REJECTED   // cảnh báo đăng nhập thiết bị lạ (UC-AUTH-02), kết quả duyệt SP (UC-CAT-12/13)
}

public enum NotificationChannel { IN_APP, EMAIL }   // ← Notification.channel (mở rộng: PUSH, SMS, ZNS, WEBSOCKET)

public enum NotificationDeliveryStatus { PENDING, SENT, DELIVERED, FAILED }   // ← Notification.deliveryStatus

public enum PlatformNotificationTarget { ALL, CUSTOMER, VENDOR, SPECIFIC_USERS }   // ← PlatformNotification.target (mở rộng: SHIPPER)

public enum BannerPosition { HOME_TOP_CAROUSEL, HOME_TOP_SIDE, HOME_MIDDLE, CATEGORY_TOP, FLASH_SALE_TOP, POPUP }   // ← HomeBanner.position

public enum AdminNotificationType { SYSTEM_ALERT, NEW_ORDER, USER_APPROVAL, WITHDRAW_REQUEST, ESCROW_DISCREPANCY }   // ← AdminNotification.type (mở rộng: FRAUD_ALERT, KYC_BACKLOG, SLA_OVERDUE, COD_OVERDUE)

public enum AuditAction {                                                                      // ← SystemLog.actionType (mở rộng: MFA_ENABLED, MFA_DISABLED, MFA_CHALLENGE_FAILED, PAYOUT_REVIEWED)
    LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED, ACCOUNT_SUSPENDED, ACCOUNT_REACTIVATED,
    KYC_SUBMITTED, KYC_APPROVED, KYC_REJECTED, ORDER_STATUS_CHANGED, ESCROW_RELEASED,
    PAYOUT_APPROVED, PAYOUT_REJECTED, REFUND_PROCESSED, REVIEW_HIDDEN, COMMISSION_UPDATED,
    PERMISSION_GRANTED, PERMISSION_REVOKED, RBAC_ROLE_CHANGED, STAFF_INVITED, PII_ANONYMIZED,
    ACCESS_DENIED, PRODUCT_MODERATED,                 // lần truy cập bị chặn quyền (màn Nhật ký), kết quả duyệt sản phẩm
    PASSWORD_CHANGED, PASSWORD_RESET, EMAIL_CHANGED, OAUTH_LINKED, OAUTH_UNLINKED, SESSION_REVOKED,
    ACCOUNT_DELETION_REQUESTED, ACCOUNT_DELETION_CANCELLED,  // +8 giá trị AUTH (V14 — plan AUTH D20)
    BANNER_CHANGED,                                          // V15 — plan 2026-11-16 banner D1
    KYC_SUPPLEMENT_REQUESTED, KYC_REQUEUED, KYC_DOCUMENT_VIEWED, PAYOUT_ACCOUNT_CHANGED // V20 — plan 2026-11-16 Mở shop & KYC D3
}

// ============================== ENTITIES (6) ==============================

// 10.1. Notification — Thông báo cá nhân  —  Bảng: notifications
@Entity
@Table(name = "notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false)
    private Long userId;                      // FK → users.id — gửi cho ai

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;            // 15 loại (Phụ lục A): ORDER_STATUS_CHANGED, KYC_APPROVED, PAYMENT_SUCCESS (người mua), NEW_ORDER_FOR_VENDOR (shop)... — sự kiện con ghi trong metadataJson.event

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề thông báo

    @Column(columnDefinition = "TEXT")
    private String body;                      // NULLABLE — Nội dung chi tiết

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh minh hoạ (ảnh sản phẩm, banner)

    @Column(length = 255)
    private String referenceId;               // NULLABLE — ID đối tượng liên quan, để bấm vào là điều hướng tới

    @Column(length = 50)
    private String referenceType;             // NULLABLE — Loại đối tượng: "ORDER", "PRODUCT", "VOUCHER"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadataJson;              // NULLABLE — Dữ liệu bổ sung dạng JSON tuỳ loại thông báo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;      // Kênh phát thông báo MVP: IN_APP (lưu bảng + đẩy WebSocket) / EMAIL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus deliveryStatus; // PENDING, SENT, DELIVERED, FAILED (A.14)

    private Integer retryCount;               // NULLABLE — Số lần đã thử gửi lại khi thất bại

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Đã đọc hay chưa — dùng cho badge số đỏ

    private OffsetDateTime sentAt;             // NULLABLE — Thời điểm gửi đi thành công

    private OffsetDateTime readAt;             // NULLABLE — Thời điểm user mở đọc

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo thông báo
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đã gửi qua kênh — UC-NOT-01 */
    public void markSent(OffsetDateTime now) {
        this.deliveryStatus = NotificationDeliveryStatus.SENT;
        this.sentAt = now;
    }

    /** Gửi lỗi — tăng retryCount, @Scheduled thử lại tối đa 3 lần — UC-NOT-03 */
    public void markFailed() {
        this.retryCount += 1;
        this.deliveryStatus = NotificationDeliveryStatus.FAILED;
    }

    /** Người dùng đã đọc — UC-NOT-01 */
    public void markRead(OffsetDateTime now) {
        this.isRead = true;
        this.readAt = now;
    }
}

// 10.2. PlatformNotification — Thông báo phát toàn sàn  —  Bảng: platform_notifications
@Entity
@Table(name = "platform_notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class PlatformNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề thông báo

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;                   // Nội dung

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh banner kèm theo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlatformNotificationTarget target; // Đối tượng nhận: ALL, CUSTOMER, VENDOR, SPECIFIC_USERS

    @Column(nullable = false)
    private Long createdBy;                   // FK mềm → users.id — admin nào đã tạo

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm tạo

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm sửa gần nhất

    @Formula("(SELECT COUNT(*) FROM user_platform_notification_reads r WHERE r.platform_notification_id = id)")
    private Long readCount;                   // CHỈ ĐỌC, tính động (@Formula, thuộc tính dẫn xuất) — số người đã đọc; luôn ≥ 0

    @Builder.Default
    @Column(nullable = false)
    private Long reachCount = 0L;             // Số người ĐÃ NHẬN (tính lúc phát) — mẫu số để tính tỷ lệ đọc

    private OffsetDateTime sendAt;             // NULLABLE — Hẹn giờ phát; NULL = phát ngay (cron "Phát thông báo hẹn giờ") — UC-NOT-05 (TLCN v1.3)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String recipientUserIdsJson;      // NULLABLE — Mảng users.id khi target = SPECIFIC_USERS — UC-NOT-05 (TLCN v1.3)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đếm lượt đọc broadcast — UC-NOT-05 */
    public void incrementRead() {
        this.readCount += 1;
    }
}

// 10.3. UserPlatformNotificationRead — Đánh dấu đã đọc thông báo sàn  —  Bảng: user_platform_notification_reads
@Entity
@Table(name = "user_platform_notification_reads")
@IdClass(UserPlatformNotificationReadId.class)
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class UserPlatformNotificationRead {

    @Id
    private Long userId;                      // FK → users.id — phần 1 khoá chính gộp

    @Id
    private Long platformNotificationId;      // FK → platform_notifications.id — phần 2 khoá chính gộp

    @Column(nullable = false)
    private OffsetDateTime readAt;             // Thời điểm user đọc
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đánh dấu đã đọc thông báo toàn sàn — UC-NOT-05 */
    public static UserPlatformNotificationRead of(Long userId, Long notificationId, OffsetDateTime now) {
        UserPlatformNotificationRead r = new UserPlatformNotificationRead();
        r.userId = userId; r.platformNotificationId = notificationId; r.readAt = now;
        return r;
    }
}

// 10.4. HomeBanner — Banner trang chủ & chiến dịch tĩnh (CMS)  —  Bảng: home_banners
@Entity
@Table(name = "home_banners", indexes = {
    @Index(name = "idx_banner_position_time", columnList = "position, is_active, starts_at, ends_at")
})
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class HomeBanner {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(nullable = false, length = 200)
    private String title;                     // Tên nội bộ / alt text: "Siêu Sale 11.11 — Slider 1"

    @Column(nullable = false, length = 500)
    private String imageUrl;                  // Ảnh banner bản desktop (Silo/CDN)

    @Column(length = 500)
    private String mobileImageUrl;            // NULLABLE — Ảnh bản mobile; NULL = dùng imageUrl

    @Column(nullable = false, length = 500)
    private String targetUrl;                 // Đích khi bấm (đường dẫn trong miền sàn): danh mục, sản phẩm, cửa hàng, khu Flash Sale, Gợi ý hôm nay, trang chiến dịch

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BannerPosition position;          // HOME_TOP_CAROUSEL / HOME_TOP_SIDE / HOME_MIDDLE / CATEGORY_TOP / FLASH_SALE_TOP / POPUP

    private Long categoryId;                  // NULLABLE — FK → categories.id; bắt buộc khi position = CATEGORY_TOP

    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;            // Thứ tự trong cùng vị trí (số nhỏ hiển thị trước)

    @Column(nullable = false)
    private OffsetDateTime startsAt;           // Bắt đầu hiển thị

    @Column(nullable = false)
    private OffsetDateTime endsAt;             // Kết thúc hiển thị

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;          // Công tắc tắt khẩn cấp (sai giá, sai nội dung) mà không sửa lịch

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String targetAudienceJson;        // NULLABLE — Nhắm đối tượng: {"roles":["CUSTOMER"],"newUserOnly":true}; NULL = mọi người

    @Column(nullable = false)
    private Long createdByAdminId;            // ID admin tạo banner — dữ liệu kiểm toán, KHÔNG đặt FK (như VendorProfile.kycVerifiedBy)

    @Builder.Default
    @Column(nullable = false)
    private Long impressionCount = 0L;        // Lượt hiển thị (flush từ Redis banner:{id}:imp mỗi 5 phút)

    @Builder.Default
    @Column(nullable = false)
    private Long clickCount = 0L;             // Lượt bấm (flush từ Redis banner:{id}:clk)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đang hiển thị (bật và trong khung giờ) — UC-ADM-12 */
    public boolean isLive(OffsetDateTime now) {
        return Boolean.TRUE.equals(this.isActive) && !now.isBefore(this.startsAt) && now.isBefore(this.endsAt);
    }

    /** Tắt khẩn cấp — UC-ADM-12 */
    public void deactivate() {
        this.isActive = false;
    }

    /** Ghi bền bộ đếm từ Redis (GETDEL) — UC-ADM-12 */
    public void recordStats(long impressions, long clicks) {
        this.impressionCount += impressions;
        this.clickCount += clicks;
    }

    /** Tỷ lệ nhấp CTR — UC-ADM-12 */
    public double ctr() {
        return this.impressionCount == 0 ? 0.0 : (double) this.clickCount / this.impressionCount;
    }

    // ===== Bổ sung plan 2026-11-16 banner (B02.02; ENTITY 10.4, Phụ lục E) =====

    /** Tạo banner mới — UC-ADM-12. {@code imageKey} / {@code mobileImageKey} là khóa đối tượng trong {@code content-assets}. */
    public static HomeBanner create(String title, String imageKey, String mobileImageKey, String targetUrl,
                                    BannerPosition position, Long categoryId, int sortOrder, OffsetDateTime startsAt,
                                    OffsetDateTime endsAt, boolean active, long adminId) {
        HomeBanner b = new HomeBanner();
        b.impressionCount = 0L;
        b.clickCount = 0L;
        b.createdByAdminId = adminId;
        b.isActive = active;
        b.changeContent(title, targetUrl, position, categoryId, sortOrder);
        b.changeSchedule(startsAt, endsAt);
        b.changeImages(imageKey, mobileImageKey);
        return b;
    }

    /** Trạng thái hiển thị (D3): tắt → OFF; chưa tới giờ → SCHEDULED; hết giờ → ENDED; còn lại → LIVE — UC-ADM-12 */
    public BannerState state(OffsetDateTime now) {
        if (!Boolean.TRUE.equals(this.isActive)) return BannerState.OFF;
        if (now.isBefore(this.startsAt)) return BannerState.SCHEDULED;
        if (!now.isBefore(this.endsAt)) return BannerState.ENDED;
        return BannerState.LIVE;
    }

    /** Bật lại sau khi tắt khẩn cấp (service kiểm giới hạn 8 banner / vị trí trước) — UC-ADM-12 */
    public void activate() {
        this.isActive = true;
    }

    /** Đổi khung giờ hiển thị; {@code endsAt} phải sau {@code startsAt} — UC-ADM-12 */
    public void changeSchedule(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt))
            throw new IllegalArgumentException("Thời điểm kết thúc phải sau thời điểm bắt đầu");
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /** Đổi nội dung / vị trí; CATEGORY_TOP bắt buộc {@code categoryId}, vị trí khác bỏ {@code categoryId} — UC-ADM-12 */
    public void changeContent(String title, String targetUrl, BannerPosition position, Long categoryId, int sortOrder) {
        if (position == null) throw new IllegalArgumentException("Thiếu vị trí hiển thị");
        if (position == BannerPosition.CATEGORY_TOP && categoryId == null)
            throw new IllegalArgumentException("Banner đầu trang ngành hàng phải chọn danh mục");
        if (sortOrder < 0) throw new IllegalArgumentException("Thứ tự không được âm");
        this.title = title;
        this.targetUrl = targetUrl;
        this.position = position;
        this.categoryId = position == BannerPosition.CATEGORY_TOP ? categoryId : null;
        this.sortOrder = sortOrder;
    }

    /** Đổi ảnh (khóa đối tượng); ảnh desktop bắt buộc, mobile {@code null} = dùng ảnh desktop — UC-ADM-12 */
    public void changeImages(String imageKey, String mobileImageKey) {
        if (imageKey == null || imageKey.isBlank()) throw new IllegalArgumentException("Thiếu ảnh banner");
        this.imageUrl = imageKey;
        this.mobileImageUrl = mobileImageKey == null || mobileImageKey.isBlank() ? null : mobileImageKey;
    }

    /** Chỉ xóa được banner đã tắt hoặc đã hết giờ — UC-ADM-12 */
    public boolean isDeletable(OffsetDateTime now) {
        BannerState s = state(now);
        return s == BannerState.OFF || s == BannerState.ENDED;
    }
}

// 10.5. AdminNotification — Thông báo nội bộ cho quản trị viên  —  Bảng: admin_notifications
@Entity
@Table(name = "admin_notifications")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class AdminNotification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminNotificationType type;       // SYSTEM_ALERT / NEW_ORDER / USER_APPROVAL / WITHDRAW_REQUEST / ESCROW_DISCREPANCY (5 loại TLCN)

    @Column(nullable = false, length = 255)
    private String title;                     // Tiêu đề

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;                   // Nội dung chi tiết

    @Column(length = 500)
    private String referenceUrl;              // NULLABLE — Link tới trang xử lý trong admin panel

    @Column(length = 500)
    private String imageUrl;                  // NULLABLE — Ảnh minh hoạ

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;           // Đã xử lý/đã xem chưa

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm phát sinh

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Thời điểm cập nhật
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Nhân viên / Admin đã xem cảnh báo — UC-NOT-01 */
    public void markRead() {
        this.isRead = true;
    }
}

// 12.1. SystemLog — Nhật ký kiểm toán  —  Bảng: system_logs
@Entity
@Table(name = "system_logs")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class SystemLog {

    @Id
    @Column(length = 36)
    private String id;                        // Khoá chính kiểu UUID (36 ký tự), KHÔNG tự tăng

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm xảy ra hành động

    private Long userId;                      // NULLABLE — Ai thực hiện. NULL = hành động của hệ thống

    @Column(nullable = false, length = 255)
    private String userEmail;                 // Email người thực hiện — CHỤP LẠI, không JOIN

    @Column(nullable = false, length = 50)
    private String userRole;                  // Vai trò lúc thực hiện — chụp lại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction actionType;           // Loại hành động: LOGIN_SUCCESS, KYC_APPROVED, PAYOUT_APPROVED...

    @Column(length = 100)
    private String referenceId;               // NULLABLE — ID đối tượng bị tác động

    @Column(columnDefinition = "TEXT")
    private String details;                   // NULLABLE — Chi tiết dạng JSON: giá trị trước/sau khi sửa

    @Column(length = 45)
    private String ipAddress;                 // NULLABLE — Địa chỉ IP thực hiện hành động (NULL với tác vụ hệ thống / Cron)

    @Column(nullable = false, length = 20)
    private String status;                    // Kết quả: "SUCCESS" / "FAILED"

    @Column(length = 500)
    private String userAgent;                 // NULLABLE — Trình duyệt/thiết bị thực hiện

    // Sinh UUID và mốc thời gian trước khi lưu thông qua hook @PrePersist của JPA (createdAt còn có @CreationTimestamp — Quy ước 12)
    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
    }
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Tạo bản ghi kiểm toán bất biến — UC-ADM-03 */
    public static SystemLog of(Long userId, String userEmail, String userRole, AuditAction action, String referenceId, String details, String ip, boolean success) {
        SystemLog l = new SystemLog();
        l.userId = userId; l.userEmail = userEmail; l.userRole = userRole;   // NOT NULL — chụp lại lúc thực hiện ("system@genie.vn" / "SYSTEM" với job nền)
        l.actionType = action; l.referenceId = referenceId; l.details = details; l.ipAddress = ip;
        l.status = success ? "SUCCESS" : "FAILED";
        return l;   // id & createdAt gán ở prePersist() — không vi phạm NOT NULL dù factory không truyền thời gian
    }
}

// ============================== KHÓA CHÍNH GỘP @IdClass (1) ==============================

/** Khóa chính gộp (@IdClass) của UserPlatformNotificationRead — userId + platformNotificationId (ENTITY.md 10.3) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserPlatformNotificationReadId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;  // FK → users.id — phần 1 khoá chính gộp
    private Long platformNotificationId;  // FK → platform_notifications.id — phần 2 khoá chính gộp
}
