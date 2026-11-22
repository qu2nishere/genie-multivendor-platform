package com.genie.catalog.product.application;

import java.util.List;

/**
 * {@code GET /public/products/{id}/variants} — modal "Chọn phân loại" ở giỏ hàng (API_SPEC 5.10).
 * Nhẹ hơn chi tiết: không mô tả, không shop.
 */
public record VariantChoices(List<Group> optionGroups, List<Choice> variants, String imageUrl) {

    public record Group(String name, List<String> values) {
    }

    public record Choice(long id, List<String> options, long price, int stock, String imageUrl) {
    }
}
