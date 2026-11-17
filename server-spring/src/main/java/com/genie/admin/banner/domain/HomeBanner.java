package com.genie.admin.banner.domain;

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
 * HomeBanner — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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
    private String imageUrl;                  // Ảnh banner bản desktop (MinIO/CDN)

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
