package com.genie.auth.token.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Phiên đăng nhập — bảng refresh_tokens (ENTITY.md 1.7). Chỉ lưu băm của token, không lưu token gốc.
 */
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
    private String revokedReason;             // NULLABLE — Lý do thu hồi: "USER_LOGOUT", "PASSWORD_CHANGED", "ROTATED", "SUSPICIOUS_REUSE", "SUSPICIOUS"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;          // Thời điểm đăng nhập tạo ra phiên này
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
}
