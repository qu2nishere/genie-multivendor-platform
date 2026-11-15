package com.genie.auth.profile.domain;

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
 * CustomerProfile — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "customer_profiles")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class CustomerProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "user_id", unique = true, nullable = false)
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

    // ===== Bổ sung cho phân hệ AUTH (plan/2026-11-14/auth.md A03.03) =====

    /**
     * Tạo hồ sơ cho user vừa lưu (đã có id). Gán CẢ {@code userId} (cột được ghi) lẫn {@code user}
     * (quan hệ {@code optional = false}, chỉ đọc) — UC-AUTH-01, UC-AUTH-03.
     */
    public static CustomerProfile create(User user, String fullName, String phoneNumber, String preferredLanguage) {
        if (user.getId() == null) throw new IllegalStateException("User phải được lưu trước khi tạo hồ sơ");
        return CustomerProfile.builder()
                .userId(user.getId())
                .user(user)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .preferredLanguage(preferredLanguage)
                .build();
    }

    /** Cập nhật hồ sơ (null = giữ nguyên) — UC-AUTH-08 */
    public void updateProfile(String fullName, String phoneNumber, Gender gender, LocalDate dateOfBirth,
                              String bio, String preferredLanguage) {
        if (fullName != null) {
            if (fullName.isBlank()) throw new IllegalArgumentException("Họ tên không được rỗng");
            this.fullName = fullName.trim();
        }
        if (phoneNumber != null) this.phoneNumber = phoneNumber.isBlank() ? null : phoneNumber.trim();
        if (gender != null) this.gender = gender;
        if (dateOfBirth != null) this.dateOfBirth = dateOfBirth;
        if (bio != null) this.bio = bio.isBlank() ? null : bio;
        if (preferredLanguage != null) this.preferredLanguage = preferredLanguage;
    }

    /** Đổi ảnh đại diện (URL công khai sau khi chuyển khỏi tmp/) — UC-AUTH-08 */
    public void changeAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

}
