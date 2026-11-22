package com.genie.catalog.search.application;

import java.util.List;

/**
 * Khối {@code summary.facets} của {@code GET /public/products?include_facets=true} (API_SPEC 5.3).
 * {@code attribute} = {@code null} khi chưa chọn đúng một danh mục hoặc danh mục không có thuộc tính lọc.
 */
public record SearchFacets(List<BrandCount> brands, List<LocationCount> locations, AttributeFacet attribute,
                           boolean hasShopVoucherSupported) {

    public record BrandCount(long id, String name, long count) {
    }

    public record LocationCount(int provinceId, String name, long count) {
    }

    public record AttributeFacet(String code, String name, List<ValueCount> values) {
    }

    public record ValueCount(String value, long count) {
    }

    /** Vỏ {@code summary} — {@code { "facets": … }}. */
    public record Summary(SearchFacets facets) {
    }
}
