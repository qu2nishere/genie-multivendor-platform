package com.genie.catalog.search.domain;

/**
 * Tiêu chí sắp xếp {@code sort} của {@code GET /public/products} (API_SPEC 5.3; plan 2026-11-17 A3, D9).
 * "Phổ biến nhất" của UC-CAT-05 không có enum riêng: = {@link #RELEVANCE} khi có từ khóa, {@link #BEST_SELLING} khi không.
 */
public enum ProductSort {
    RELEVANCE, NEWEST, BEST_SELLING, PRICE_ASC, PRICE_DESC
}
