package com.genie.auth.address.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Địa chỉ giao hàng — bảng addresses (ENTITY.md 1.3). Xóa mềm qua isDeleted.
 */
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
