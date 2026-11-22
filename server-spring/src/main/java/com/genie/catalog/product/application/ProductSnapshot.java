package com.genie.catalog.product.application;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Phần TĨNH của trang chi tiết — cache Redis 5 phút (API_SPEC 0.12; plan 2026-11-17 D11).
 * Không chứa tồn kho: tồn đọc lại mỗi request ({@link ProductDetailService}).
 * Bản nội bộ — API trả {@link ProductDetailView} / {@link VariantChoices} dựng từ bản này.
 */
public record ProductSnapshot(
        long id,
        String slug,
        String name,
        Long categoryId,
        List<ProductDetailView.CategoryRef> categoryPath,
        ProductDetailView.BrandRef brand,
        List<String> images,
        long price,
        Long originalPrice,
        Integer discountPercent,
        double ratingAvg,
        long ratingCount,
        long soldCount,
        boolean isPreOrder,
        Integer preOrderDays,
        List<ProductDetailView.OptionGroup> optionGroups,
        List<SkuView> skus,
        List<ProductDetailView.Spec> specs,
        String condition,
        String origin,
        String shipsFrom,
        String descriptionHtml,
        Shop shop) {

    /** SKU còn bán (chưa xóa mềm), {@code options} theo thứ tự nhóm. */
    public record SkuView(long id, List<String> options, long price, String imageUrl) {
    }

    /** Thông tin shop + mốc tạm nghỉ thô (tính "đang nghỉ" lúc trả để không lệch theo cache). */
    public record Shop(long id, long vendorId, String name, String logoUrl, boolean isMall, boolean isFavorite,
                       double ratingAvg, long followerCount, String provinceName, int joinedYear,
                       OffsetDateTime vacationStartsAt, OffsetDateTime vacationEndsAt, String vacationMessage) {
    }
}
