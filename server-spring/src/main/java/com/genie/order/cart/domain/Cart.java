package com.genie.order.cart.domain;

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
 * Cart — sinh từ ENTITY.md (nguồn sự thật), xem plan/2026-11-14/server-spring/implement-entity.md.
 */
@Entity
@Table(name = "carts")
@Getter @Setter @Builder @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor
public class Cart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                          // Khoá chính

    @Column(unique = true, nullable = false)
    private Long userId;                      // FK → users.id, UNIQUE — mỗi user có đúng 1 giỏ hàng

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;          // Lần cuối giỏ hàng thay đổi — dùng cho email nhắc "bỏ quên giỏ hàng"

    // ===== QUAN HỆ =====
    @OneToMany
    @JoinColumn(name = "cart_id", insertable = false, updatable = false)   // một chiều, chỉ đọc — FK ghi qua CartItem.cartId
    private List<CartItem> items;             // Các dòng sản phẩm trong giỏ
    // ===== Phương thức nghiệp vụ (Domain Methods — ngăn Operations của Class Diagram) =====
    // Getter/Setter do Lombok sinh, KHÔNG vẽ trên diagram; chỉ vẽ các phương thức dưới đây.

    /** Thêm sản phẩm (gộp nếu trùng SKU) — UC-ORD-01 */
    public void addItem(CartItem item) {
        this.items.stream().filter(i -> i.getProductVariantId().equals(item.getProductVariantId())).findFirst()
            .ifPresentOrElse(i -> i.increase(item.getQuantity()), () -> this.items.add(item));
    }

    /** Xóa dòng hàng — UC-ORD-01 */
    public void removeItem(Long productVariantId) {
        this.items.removeIf(i -> i.getProductVariantId().equals(productVariantId));
    }

    /** Tạm tính theo giá snapshot — UC-ORD-04 */
    public BigDecimal subtotal() {
        return this.items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Gộp giỏ tạm của khách vãng lai (lưu trên trình duyệt) vào giỏ tài khoản khi đăng nhập / đăng ký; service đã lọc SKU ngừng bán và giới hạn số lượng theo tồn kho trước khi gọi — UC-ORD-01 */
    public void mergeGuestItems(List<CartItem> guestItems) {
        guestItems.forEach(this::addItem);
    }
}
