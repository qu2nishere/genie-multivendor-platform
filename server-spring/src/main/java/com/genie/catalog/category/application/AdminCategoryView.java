// Nút cây danh mục của màn quản trị — API 11.11 GET /admin/categories (UC-CAT-14; plan B2 D7, D17)
package com.genie.catalog.category.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code commissionRate}: hoa hồng <b>riêng</b> của ngành (phần trăm, {@code null} = dùng ngành cha / mặc định);
 * {@code effectiveCommissionRate}: tỷ lệ đang áp dụng thực tế (riêng → cha gần nhất → mặc định toàn sàn).
 * {@code productCount} / {@code shopCount}: sản phẩm chưa xóa / số shop khác nhau của <b>cả nhánh</b>.
 */
public record AdminCategoryView(long id, String name, String slug,
                                @JsonInclude(JsonInclude.Include.ALWAYS) String icon,
                                @JsonInclude(JsonInclude.Include.ALWAYS) String description,
                                @JsonInclude(JsonInclude.Include.ALWAYS) Long parentId,
                                int level, boolean isActive, boolean requiresBusinessLicense,
                                @JsonInclude(JsonInclude.Include.ALWAYS) BigDecimal commissionRate,
                                BigDecimal effectiveCommissionRate, long productCount, long shopCount,
                                List<AdminCategoryView> children) {
}
