// Thuộc tính chuẩn của ngành — API 11.11 "Bộ thuộc tính" (UC-CAT-15; plan B2 C06, D8, D11)
package com.genie.catalog.attribute.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * {@code values}: danh sách giá trị khi {@code ENUM} / {@code MULTI_ENUM} (rỗng ở kiểu khác). {@code gracePeriodDays}
 * chỉ có trong phản hồi thêm / sửa khi thuộc tính vừa thành <b>bắt buộc</b> mà nhánh đã có sản phẩm (shop có 14 ngày bổ
 * sung — D11).
 */
public record AttributeView(String code, String name, String dataType, List<String> values,
                            @JsonInclude(JsonInclude.Include.ALWAYS) String unit,
                            boolean isRequired, boolean isFilterable, boolean isVariantAxis, boolean isActive,
                            int displayOrder,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Integer gracePeriodDays) {

    public AttributeView withGracePeriod(Integer days) {
        return new AttributeView(code, name, dataType, values, unit, isRequired, isFilterable, isVariantAxis, isActive,
                displayOrder, days);
    }
}
