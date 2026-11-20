package com.genie.catalog.product.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ma trận phân loại SKU của trang chi tiết (UC-CAT-06 bước 4–5; API_SPEC 5.6 {@code option_groups} + {@code variants};
 * plan 2026-11-17 thuật toán A7). Hàm thuần, không phụ thuộc CSDL.
 * <p><b>Thuật toán:</b>
 * <ol>
 *   <li>nhóm phân loại = các khóa của {@code attributes_json}, theo thứ tự xuất hiện đầu tiên (SKU sắp theo id);</li>
 *   <li>giá trị trong nhóm theo thứ tự xuất hiện; ảnh của giá trị chỉ có ở nhóm {@code variant_image_attribute}
 *       (vd "Màu sắc") — lấy ảnh của SKU đầu tiên mang giá trị đó;</li>
 *   <li>{@code variants[].options} = giá trị của SKU theo đúng thứ tự nhóm → client dựng bảng tra
 *       {@code options.join('|') → SKU} (O(1) khi chọn tổ hợp) và khóa giá trị không còn tổ hợp có hàng.</li>
 * </ol>
 * SKU thiếu một nhóm thì ô đó là {@code ""}.
 */
public final class VariantMatrix {

    /** Một SKU còn bán: thuộc tính theo thứ tự của JSON. */
    public record Sku(long id, Map<String, String> attributes, long price, String imageUrl) {
    }

    public record OptionValue(String value, String imageUrl) {
    }

    public record OptionGroup(String name, List<OptionValue> values) {
    }

    public record Variant(long id, List<String> options, long price) {
    }

    public record Result(List<OptionGroup> groups, List<Variant> variants) {
    }

    private VariantMatrix() {
    }

    public static Result build(List<Sku> skus, String imageAttribute) {
        Map<String, Map<String, String>> groups = new LinkedHashMap<>();   // nhóm → (giá trị → ảnh)
        for (Sku sku : skus) {
            for (Map.Entry<String, String> e : sku.attributes().entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null) {
                    continue;
                }
                Map<String, String> values = groups.computeIfAbsent(e.getKey(), k -> new LinkedHashMap<>());
                String image = e.getKey().equals(imageAttribute) ? sku.imageUrl() : null;
                values.merge(e.getValue(), image == null ? "" : image, (old, neu) -> old.isEmpty() ? neu : old);
            }
        }
        List<OptionGroup> outGroups = new ArrayList<>();
        groups.forEach((name, values) -> {
            List<OptionValue> vals = new ArrayList<>();
            values.forEach((v, img) -> vals.add(new OptionValue(v, img.isEmpty() ? null : img)));
            outGroups.add(new OptionGroup(name, vals));
        });
        List<String> order = new ArrayList<>(groups.keySet());
        List<Variant> variants = new ArrayList<>(skus.size());
        for (Sku sku : skus) {
            List<String> options = new ArrayList<>(order.size());
            for (String g : order) {
                options.add(Objects.requireNonNullElse(sku.attributes().get(g), ""));
            }
            variants.add(new Variant(sku.id(), options, sku.price()));
        }
        return new Result(outGroups, variants);
    }

    /**
     * Giá trị còn chọn được của nhóm {@code groupIndex} khi đã chọn {@code selected} (ô {@code null} = chưa chọn) —
     * tồn tại SKU khớp mọi lựa chọn khác + giá trị đó và còn hàng. Server không dùng; có để test đồng bộ với
     * hàm cùng tên phía web ({@code product.rules.ts}).
     */
    public static Set<String> selectableValues(Result matrix, Map<Long, Integer> stock, List<String> selected, int groupIndex) {
        Set<String> out = new LinkedHashSet<>();
        for (Variant v : matrix.variants()) {
            if (stock.getOrDefault(v.id(), 0) <= 0) {
                continue;
            }
            boolean ok = true;
            for (int g = 0; g < v.options().size(); g++) {
                if (g != groupIndex && selected.get(g) != null && !selected.get(g).equals(v.options().get(g))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                out.add(v.options().get(groupIndex));
            }
        }
        return out;
    }
}
