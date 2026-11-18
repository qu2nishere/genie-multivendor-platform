package com.genie.catalog.product.application;

/**
 * {@code $ProductCard} — thẻ sản phẩm ở mọi lưới: tìm kiếm, danh mục, shop, tương tự, gợi ý (API_SPEC mục 1).
 * Tiền là số nguyên VND. {@code flashSale} = {@code null} tới khi có Flash Sale (UC-PRO-06).
 */
public record ProductCard(
        long id,
        String slug,
        String name,
        String imageUrl,
        long price,
        Long originalPrice,
        Integer discountPercent,
        double ratingAvg,
        long soldCount,
        ShopRef shop,
        boolean isFreeShipping,
        boolean isPreOrder,
        boolean isShopOnVacation,
        FlashSale flashSale) {

    /** {@code isMall} = nhãn "Chính hãng" trên thẻ (như {@code $ShopMini}). */
    public record ShopRef(long id, String name, boolean isMall) {
    }

    public record FlashSale(int soldCount, int totalCount) {
    }
}
