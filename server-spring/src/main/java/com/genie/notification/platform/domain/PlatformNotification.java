package com.genie.notification.platform.domain;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.Formula;

/**
 * PlatformNotification — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
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

    private OffsetDateTime dispatchedAt;       // NULLABLE — Thời điểm đã phát; NULL = đang hẹn giờ — UC-NOT-05 (V27, plan 2026-11-26 thông báo D2)
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Đếm lượt đọc broadcast — UC-NOT-05 */
    public void incrementRead() {
        this.readCount += 1;
    }

    /** Đã phát chưa — UC-NOT-05 (chỉ sửa được khi chưa phát, GNE-NOT-4091) */
    public boolean isDispatched() {
        return this.dispatchedAt != null;
    }

    /** Đang hẹn giờ (chưa phát) — {@code is_scheduled} ở API 11.17 — UC-NOT-05 */
    public boolean isScheduled() {
        return this.dispatchedAt == null;
    }

    /** Còn sửa được không (chỉ khi chưa phát — API 11.17, plan 2026-11-26 thông báo D15) — UC-NOT-05 */
    public boolean isEditable() {
        return this.dispatchedAt == null;
    }

    /** Đổi nội dung / đối tượng / lịch của bản chưa phát; đã phát → {@code IllegalStateException} (GNE-NOT-4091) — UC-NOT-05 */
    public void changeContent(String title, String content, PlatformNotificationTarget target, String recipientIdsJson,
                              OffsetDateTime sendAt) {
        if (!isEditable()) throw new IllegalStateException("Thông báo đã phát");
        this.title = title;
        this.content = content;
        this.target = target;
        this.recipientUserIdsJson = target == PlatformNotificationTarget.SPECIFIC_USERS ? recipientIdsJson : null;
        this.sendAt = sendAt;
    }

    /** Phát: chốt số người nhận tại thời điểm phát — gọi đúng một lần (phát ngay hoặc cron hẹn giờ) — UC-NOT-05 */
    public void dispatch(long reach, OffsetDateTime at) {
        if (this.dispatchedAt != null) throw new IllegalStateException("Thông báo đã phát");
        if (reach < 0) throw new IllegalArgumentException("reach < 0");
        Objects.requireNonNull(at, "at");
        this.reachCount = reach;
        this.dispatchedAt = at;
    }
}
