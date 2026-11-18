package com.genie.catalog.search.application;

import com.genie.catalog.search.domain.KeywordNormalizer;
import com.genie.catalog.search.domain.ProductSort;
import java.util.List;
import java.util.Map;

/**
 * Tham số tìm kiếm & lọc đã kiểm hợp lệ (API_SPEC 5.3; UC-CAT-02, UC-CAT-05, UC-CAT-16).
 *
 * @param keyword       từ khóa đã chuẩn hóa + bỏ dấu ({@link KeywordNormalizer#normalizeUnaccented}); {@code ""} = không tìm theo chữ
 * @param attributes    {@code attr.{code}} → các giá trị (OR trong cùng code, AND giữa các code)
 * @param page          bắt đầu từ 1
 */
public record ProductSearchCriteria(
        String keyword,
        Long shopId,
        Long categoryId,
        List<Long> brandIds,
        List<Integer> provinceIds,
        Long priceMin,
        Long priceMax,
        Integer ratingMin,
        Boolean isMall,
        Boolean freeShipping,
        Boolean hasShopVoucher,
        Map<String, List<String>> attributes,
        Integer discountMin,
        ProductSort sort,
        boolean includeFacets,
        int page,
        int size) {

    public ProductSearchCriteria {
        keyword = keyword == null ? "" : keyword;
        brandIds = brandIds == null ? List.of() : List.copyOf(brandIds);
        provinceIds = provinceIds == null ? List.of() : List.copyOf(provinceIds);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public boolean hasKeyword() {
        return !keyword.isEmpty();
    }

    /** Sắp xếp thực dùng: mặc định RELEVANCE khi có từ khóa, NEWEST khi không; RELEVANCE mà không có từ khóa → BEST_SELLING. */
    public ProductSort effectiveSort() {
        if (sort == null) {
            return hasKeyword() ? ProductSort.RELEVANCE : ProductSort.NEWEST;
        }
        if (sort == ProductSort.RELEVANCE && !hasKeyword()) {
            return ProductSort.BEST_SELLING;
        }
        return sort;
    }

    /** Lọc thuộc tính chỉ có nghĩa khi đã chọn đúng một danh mục (API_SPEC 5.3). */
    public boolean attributeFilterApplies() {
        return categoryId != null && !attributes.isEmpty();
    }

    public static ProductSearchCriteria simple(String keyword, int page, int size) {
        return new ProductSearchCriteria(keyword, null, null, null, null, null, null, null, null, null, null, null, null,
                null, false, page, size);
    }
}
