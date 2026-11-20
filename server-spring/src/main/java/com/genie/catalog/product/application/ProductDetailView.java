package com.genie.catalog.product.application;

import java.time.LocalDate;
import java.util.List;

/**
 * {@code GET /public/products/{id}} — chi tiết sản phẩm (UC-CAT-06; API_SPEC 5.6).
 * {@code variants[].stock} = tồn khả dụng ({@code stock_count − reserved_count}) đọc thẳng DB mỗi request.
 * {@code videoUrl}, {@code flashSale} = {@code null} tới khi có tính năng tương ứng.
 */
public record ProductDetailView(
        long id,
        String slug,
        String name,
        List<CategoryRef> categoryPath,
        BrandRef brand,
        List<String> images,
        String videoUrl,
        long price,
        Long originalPrice,
        Integer discountPercent,
        double ratingAvg,
        long ratingCount,
        long soldCount,
        boolean isPreOrder,
        Integer preOrderDays,
        Object flashSale,
        List<OptionGroup> optionGroups,
        List<Variant> variants,
        List<Spec> specs,
        String condition,
        String origin,
        String shipsFrom,
        String descriptionHtml,
        ShopInfo shop) {

    public record CategoryRef(long id, String name, String slug) {
    }

    public record BrandRef(Long id, String name, boolean isVerified) {
    }

    public record OptionValue(String value, String imageUrl) {
    }

    public record OptionGroup(String name, List<OptionValue> values) {
    }

    public record Variant(long id, List<String> options, long price, int stock) {
    }

    public record Spec(String name, String value) {
    }

    public record Vacation(LocalDate until, String message) {
    }

    public record ShopInfo(long id, String name, String logoUrl, boolean isMall, boolean isFavorite, double ratingAvg,
                           long followerCount, String provinceName, int joinedYear, boolean isOnline, Vacation vacation) {
    }
}
