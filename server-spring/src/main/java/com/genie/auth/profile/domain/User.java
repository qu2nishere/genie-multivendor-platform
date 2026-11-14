package com.genie.auth.profile.domain;

import com.genie.common.domain.UserRole;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.genie.auth.address.domain.Address;
import com.genie.auth.token.domain.RefreshToken;

/**
 * Tài khoản đăng nhập — bảng users (ENTITY.md 1.1).
 * D2: không có quan hệ điều hướng sang VendorProfile / AdminProfile (module vendor / admin) — chỉ tra qua userId ở phía đó.
 */
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


    // ===== QUAN HỆ =====
    @OneToOne(mappedBy = "user")
    private CustomerProfile customerProfile;  // NULLABLE (phía nghịch của OneToOne) — Hồ sơ khách hàng (chỉ có khi role = CUSTOMER)

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
        this.oauthProviderId = null;
    }

}
