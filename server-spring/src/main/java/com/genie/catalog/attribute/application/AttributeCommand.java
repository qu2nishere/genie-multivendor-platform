package com.genie.catalog.attribute.application;

import java.util.List;

/** Thân {@code POST} / {@code PUT} thuộc tính (API 11.11). Khi sửa: {@code code} lấy từ đường dẫn, {@code dataType} khác kiểu đang có → 422 (D9). */
public record AttributeCommand(String code, String name, String dataType, List<String> values, String unit,
                               Boolean isRequired, Boolean isFilterable, Boolean isVariantAxis) {
}
