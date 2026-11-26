package com.genie.order.cart.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Mới thêm trước (giỏ hiển thị shop có món mới nhất lên đầu). */
    @Query("SELECT i FROM CartItem i WHERE i.cartId = :cartId ORDER BY i.addedAt DESC, i.id DESC")
    List<CartItem> findByCart(@Param("cartId") long cartId);

    @Query("SELECT i FROM CartItem i WHERE i.cartId = :cartId ORDER BY i.addedAt DESC, i.id DESC")
    List<CartItem> findLatest(@Param("cartId") long cartId, Pageable page);

    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    List<CartItem> findByCartIdAndIdIn(Long cartId, Collection<Long> ids);

    long countByCartId(Long cartId);

    /**
     * Thêm dòng hoặc cộng dồn khi trùng biến thể ({@code uk_cart_items_cart_variant} — V25, plan C2 D15). Cộng dồn giữ
     * {@code price_snapshot} cũ (giá lúc THÊM lần đầu — dùng để báo đổi giá). Trả id dòng.
     */
    @Query(value = """
            INSERT INTO cart_items (cart_id, product_variant_id, quantity, price_snapshot, added_at)
            VALUES (:cartId, :variantId, :qty, :price, :now)
            ON CONFLICT (cart_id, product_variant_id) DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity
            RETURNING id""", nativeQuery = true)
    long upsertAdd(@Param("cartId") long cartId, @Param("variantId") long variantId, @Param("qty") int qty,
                   @Param("price") BigDecimal price, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("DELETE FROM CartItem i WHERE i.cartId = :cartId AND i.id IN :ids")
    int deleteOwned(@Param("cartId") long cartId, @Param("ids") Collection<Long> ids);
}
