package com.genie.order.cart.application;

/**
 * Một dòng giỏ (API_SPEC 6.1 {@code items[]}). {@code unitPrice} = giá hiện tại; {@code priceAtAdd} = giá lúc thêm
 * ({@code cart_items.price_snapshot}) để web báo "giá đã đổi"; {@code stock} = tồn khả dụng
 * ({@code stock_count - reserved_count}). {@code categoryName} là ngành của sản phẩm (web dùng lọc voucher theo ngành).
 */
public record CartLineView(long id, long productId, String productSlug, long variantId, String productName,
                           String categoryName, String imageUrl, String variantName, long unitPrice,
                           Long originalPrice, Long priceAtAdd, int quantity, int stock, boolean isAvailable,
                           String unavailableReason) {
}
