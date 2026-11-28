// Ảnh theo giá trị phân loại của mọi nhóm ⇄ products.option_images_json (V26; plan C1, 2026-11-27)
package com.genie.catalog.product.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code {"Màu sắc": {"Đỏ": "vendor/7/product/9/x.webp"}, "Dung lượng": {"512 GB": "…"}}} — khóa ảnh trên Silo (bucket
 * product-media), URL dựng lúc đọc bằng {@code MediaUrls}. Sản phẩm cũ (cột NULL) đọc ảnh nhóm chính từ SKU như trước.
 */
public final class OptionImages {

    private OptionImages() {
    }

    /** JSON hỏng / NULL → rỗng (không ném lỗi — chỉ là ảnh hiển thị). */
    public static Map<String, Map<String, String>> parse(ObjectMapper json, String raw) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return out;
        try {
            JsonNode node = json.readTree(raw);
            if (!node.isObject()) return out;
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> g = it.next();
                if (!g.getValue().isObject()) continue;
                Map<String, String> values = new LinkedHashMap<>();
                for (Iterator<Map.Entry<String, JsonNode>> vt = g.getValue().fields(); vt.hasNext(); ) {
                    Map.Entry<String, JsonNode> v = vt.next();
                    if (v.getValue().isTextual() && !v.getValue().asText().isBlank()) {
                        values.put(v.getKey(), v.getValue().asText());
                    }
                }
                if (!values.isEmpty()) out.put(g.getKey(), values);
            }
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
        return out;
    }

    /** Khóa ảnh của một giá trị (null = không có). */
    public static String keyOf(Map<String, Map<String, String>> images, String group, String value) {
        Map<String, String> g = images.get(group);
        return g == null ? null : g.get(value);
    }
}
