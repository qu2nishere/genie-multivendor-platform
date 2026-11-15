package com.genie.auth.token.domain;

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
 * RefreshToken — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "user_id", nullable = false)
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
    private String revokedReason;             // NULLABLE — Lý do thu hồi: "USER_LOGOUT", "PASSWORD_CHANGED", "ROTATED", "SUSPICIOUS_REUSE", "SUSPICIOUS"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đăng nhập tạo ra phiên này

    // ---- Phiên đăng nhập ổn định qua các lần xoay vòng (plan AUTH D6, V14__auth_additions.sql) ----
    @Column(name = "family_id", nullable = false)
    private Long familyId;                    // Mã phiên: mọi token sinh ra do xoay vòng từ cùng một lần đăng nhập chung family_id (seq refresh_token_family_seq). Là `id` ở GET /me/sessions và claim `sid` của access token

    private OffsetDateTime lastUsedAt;         // NULLABLE — Lần làm mới gần nhất ("Hoạt động lần cuối" ở màn Thiết bị)
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

    // ===== Bổ sung cho phân hệ AUTH (plan/2026-11-14/auth.md A03.05) =====

    public static final int MAX_DEVICE_INFO = 255;

    /**
     * Phát token mới (chỉ lưu bản băm). {@code deviceInfo} cắt còn 255 ký tự (cột NOT NULL VARCHAR(255) — User-Agent
     * thường dài hơn); thiếu IP / User-Agent thì ghi giá trị giữ chỗ vì cột NOT NULL.
     */
    public static RefreshToken issue(Long userId, Long familyId, String tokenHash, String deviceInfo, String ipAddress,
                                     OffsetDateTime expiresAt) {
        String device = deviceInfo == null || deviceInfo.isBlank() ? "unknown" : deviceInfo;
        if (device.length() > MAX_DEVICE_INFO) device = device.substring(0, MAX_DEVICE_INFO);
        return RefreshToken.builder()
                .userId(userId)
                .familyId(familyId)
                .tokenHash(tokenHash)
                .deviceInfo(device)
                .ipAddress(ipAddress == null || ipAddress.isBlank() ? "0.0.0.0" : ipAddress)
                .expiresAt(expiresAt)
                .build();
    }

    /** Ghi nhận lần dùng gần nhất — UC-AUTH-11 */
    public void touch(OffsetDateTime now) {
        this.lastUsedAt = now;
    }

    public boolean isRevokedFor(String reason) {
        return this.revokedAt != null && reason.equals(this.revokedReason);
    }
}
