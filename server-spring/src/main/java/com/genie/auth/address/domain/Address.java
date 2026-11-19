package com.genie.auth.address.domain;

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
 * Address — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "addresses")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Address {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(name = "user_id", nullable = false)
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
    private String wardCode;                  // MÃ phường/xã/đặc khu (địa giới 2 cấp từ 01/07/2025) — dùng để GỌI API vận chuyển

    @Column(length = 100)
    private String district;                  // NULLABLE — TÊN quận/huyện, chỉ còn ở địa chỉ cũ trước sáp nhập (V16; plan AUTH D25)

    private Integer districtId;               // NULLABLE — MÃ quận/huyện, chỉ còn ở địa chỉ cũ trước sáp nhập (V16; plan AUTH D25)

    @Column(nullable = false, length = 100)
    private String province;                  // TÊN tỉnh/thành phố (34 đơn vị sau sáp nhập) — để hiển thị

    @Column(nullable = false)
    private Integer provinceId;               // MÃ tỉnh/thành — để gọi API

    @Column(length = 200)
    private String deliveryNote;              // NULLABLE — ghi chú cho người giao hàng ("Gần cổng ĐH NEU, gửi phòng bảo vệ") — UC-AUTH-09, plan AUTH D27 (V17)

    private Boolean isDefault;                // NULLABLE — true = địa chỉ mặc định khi thanh toán

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressLabel label = AddressLabel.HOME; // Loại địa chỉ: HOME (Nhà riêng) / OFFICE (Văn phòng) / SCHOOL (Trường học) / APARTMENT (Chung cư) / OTHER (Khác) — UC-AUTH-09 (TLCN v1.3; plan AUTH D24, V15)

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

    // ===== Bổ sung cho phân hệ AUTH (plan/2026-11-14/auth.md A03.04) =====

    /** Bỏ cờ mặc định — UC-AUTH-09 */
    public void unmarkDefault() {
        this.isDefault = false;
    }

    /** Sửa toàn bộ nội dung địa chỉ theo địa giới 2 cấp (tên hành chính do server tra từ mã) — UC-AUTH-09, plan AUTH D25 */
    public void update(String recipientName, String phoneNumber, String addressLine,
                       Integer provinceId, String province, String wardCode, String ward, AddressLabel label,
                       String deliveryNote) {
        if (Boolean.TRUE.equals(this.isDeleted)) throw new IllegalStateException("Địa chỉ đã xóa");
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.addressLine = addressLine;
        this.provinceId = provinceId;
        this.province = province;
        this.districtId = null;               // địa giới mới không còn cấp huyện
        this.district = null;
        this.wardCode = wardCode;
        this.ward = ward;
        this.label = label == null ? AddressLabel.HOME : label;
        this.deliveryNote = deliveryNote;
    }

    public boolean isDefaultAddress() {
        return Boolean.TRUE.equals(this.isDefault);
    }
}
