// Thứ tự nhóm phân loại của một biến thể (UC-CAT-06, UC-CAT-09; plan C1 — mục "Lệch so với plan" về thứ tự nhóm)
package com.genie.catalog.product.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cột {@code attributes_json} kiểu JSONB: PostgreSQL tự sắp lại khóa (khóa ngắn trước), nên "Phiên bản" / "Màu sắc"
 * người bán nhập theo thứ tự nào thì đọc ra vẫn có thể bị đảo. Cột {@code variant_name} lưu cùng tổ hợp theo đúng thứ
 * tự người bán nhập ({@code "512GB / Đỏ"} — {@code ProductWriteService}) → dùng nó để xếp lại khóa. Không khớp được
 * (dữ liệu cũ, tên khác định dạng, giá trị trùng nhau) thì giữ nguyên thứ tự JSONB. Hàm thuần.
 */
public final class VariantAttributes {

    /** Dấu nối giá trị trong {@code variant_name}. */
    public static final String SEPARATOR = " / ";

    private VariantAttributes() {
    }

    public static Map<String, String> ordered(Map<String, String> attributes, String variantName) {
        if (attributes == null || attributes.size() < 2 || variantName == null || variantName.isBlank()) {
            return attributes;
        }
        String[] parts = variantName.split(SEPARATOR, -1);
        if (parts.length != attributes.size()) {
            return attributes;
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (String part : parts) {
            String key = null;
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                if (!out.containsKey(e.getKey()) && part.equals(e.getValue())) {
                    key = e.getKey();
                    break;
                }
            }
            if (key == null) {
                return attributes;
            }
            out.put(key, part);
        }
        return out;
    }
}
