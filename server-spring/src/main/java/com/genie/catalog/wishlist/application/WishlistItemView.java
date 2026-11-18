package com.genie.catalog.wishlist.application;

/**
 * Một dòng Tài khoản › Sản phẩm yêu thích (API_SPEC 3.10). Giá là số nguyên VND.
 *
 * @param productSlug      slug sinh từ tên ({@code Slugs.slugify}) để web dựng link {@code /p/{slug}-{id}} (handoff mục 3)
 * @param defaultVariantId SKU còn tồn rẻ nhất (hết hàng → SKU đầu tiên; không có SKU → {@code null}) — nút "Thêm vào giỏ"
 * @param isPurchasable    {@code PUBLISHED} + còn SKU có tồn + shop không nghỉ (D3)
 */
public record WishlistItemView(long productId, String productSlug, String name, String imageUrl, long price,
                               Long defaultVariantId, boolean isPurchasable) {
}
