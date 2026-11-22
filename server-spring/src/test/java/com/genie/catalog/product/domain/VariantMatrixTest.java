package com.genie.catalog.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Ma trận phân loại SKU (plan 2026-11-17 C04.01, A7). */
class VariantMatrixTest {

    private static Map<String, String> attrs(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    static final List<VariantMatrix.Sku> SKUS = List.of(
            new VariantMatrix.Sku(101, attrs("Màu sắc", "Đen", "Phiên bản", "Tiêu chuẩn"), 389000, "den.webp"),
            new VariantMatrix.Sku(102, attrs("Màu sắc", "Đen", "Phiên bản", "Bản Pro"), 539000, null),
            new VariantMatrix.Sku(103, attrs("Màu sắc", "Trắng", "Phiên bản", "Tiêu chuẩn"), 389000, null));

    @Test
    void groupsAndValuesKeepFirstAppearanceOrder() {
        var m = VariantMatrix.build(SKUS, "Màu sắc");
        assertThat(m.groups()).extracting(VariantMatrix.OptionGroup::name).containsExactly("Màu sắc", "Phiên bản");
        assertThat(m.groups().get(0).values()).containsExactly(
                new VariantMatrix.OptionValue("Đen", "den.webp"), new VariantMatrix.OptionValue("Trắng", null));
        assertThat(m.groups().get(1).values()).extracting(VariantMatrix.OptionValue::value).containsExactly("Tiêu chuẩn", "Bản Pro");
        assertThat(m.groups().get(1).values()).allMatch(v -> v.imageUrl() == null);   // chỉ nhóm ảnh mới có ảnh
    }

    @Test
    void variantOptionsFollowGroupOrder() {
        var m = VariantMatrix.build(SKUS, "Màu sắc");
        assertThat(m.variants()).containsExactly(
                new VariantMatrix.Variant(101, List.of("Đen", "Tiêu chuẩn"), 389000),
                new VariantMatrix.Variant(102, List.of("Đen", "Bản Pro"), 539000),
                new VariantMatrix.Variant(103, List.of("Trắng", "Tiêu chuẩn"), 389000));
    }

    @Test
    void singleSkuWithoutAttributesHasNoGroups() {
        var m = VariantMatrix.build(List.of(new VariantMatrix.Sku(7, Map.of(), 1000, null)), null);
        assertThat(m.groups()).isEmpty();
        assertThat(m.variants()).containsExactly(new VariantMatrix.Variant(7, List.of(), 1000));
    }

    @Test
    void missingAttributeBecomesEmptyCell() {
        var m = VariantMatrix.build(List.of(
                new VariantMatrix.Sku(1, attrs("Màu", "Đỏ", "Size", "L"), 1, null),
                new VariantMatrix.Sku(2, attrs("Màu", "Xanh"), 1, null)), null);
        assertThat(m.variants().get(1).options()).containsExactly("Xanh", "");
    }

    @Test
    void outOfStockCombinationsAreNotSelectable() {
        var m = VariantMatrix.build(SKUS, "Màu sắc");
        Map<Long, Integer> stock = Map.of(101L, 5, 102L, 0, 103L, 2);
        // Đã chọn Đen → nhóm Phiên bản chỉ còn "Tiêu chuẩn" (Bản Pro hết hàng)
        assertThat(VariantMatrix.selectableValues(m, stock, Arrays.asList("Đen", null), 1)).containsExactly("Tiêu chuẩn");
        // Đã chọn Bản Pro → không màu nào còn hàng
        assertThat(VariantMatrix.selectableValues(m, stock, Arrays.asList(null, "Bản Pro"), 0)).isEmpty();
        // Chưa chọn gì → cả hai màu đều còn
        assertThat(VariantMatrix.selectableValues(m, stock, Arrays.asList(null, null), 0)).containsExactly("Đen", "Trắng");
    }
}
