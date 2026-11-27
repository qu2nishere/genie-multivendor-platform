// Xếp lại thứ tự nhóm phân loại theo variant_name (plan C1 — thứ tự nhóm bị JSONB đảo)
package com.genie.catalog.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariantAttributesTest {

    /** Thứ tự JSONB trả về: khóa ngắn trước ("Màu sắc" trước "Phiên bản"). */
    private static Map<String, String> jsonbOrder(String color, String version) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Màu sắc", color);
        m.put("Phiên bản", version);
        return m;
    }

    @Test
    void reordersGroupsByVariantName() {
        Map<String, String> out = VariantAttributes.ordered(jsonbOrder("Đỏ", "512GB"), "512GB / Đỏ");
        assertThat(List.copyOf(out.keySet())).containsExactly("Phiên bản", "Màu sắc");
        assertThat(out).containsEntry("Phiên bản", "512GB").containsEntry("Màu sắc", "Đỏ");
    }

    @Test
    void keepsOriginalOrderWhenNameDoesNotMatch() {
        Map<String, String> in = jsonbOrder("Đỏ", "512GB");
        assertThat(VariantAttributes.ordered(in, null)).isSameAs(in);
        assertThat(VariantAttributes.ordered(in, "Đỏ 512GB")).isSameAs(in);
        assertThat(VariantAttributes.ordered(in, "1TB / Đỏ")).isSameAs(in);
    }

    @Test
    void sameValueInBothGroupsStillMapsEachGroupOnce() {
        Map<String, String> in = new LinkedHashMap<>();
        in.put("Size", "M");
        in.put("Kiểu", "M");
        Map<String, String> out = VariantAttributes.ordered(in, "M / M");
        assertThat(out).hasSize(2).containsEntry("Size", "M").containsEntry("Kiểu", "M");
    }

    @Test
    void singleGroupUntouched() {
        Map<String, String> in = Map.of("Màu sắc", "Đen");
        assertThat(VariantAttributes.ordered(in, "Đen")).isSameAs(in);
    }
}
