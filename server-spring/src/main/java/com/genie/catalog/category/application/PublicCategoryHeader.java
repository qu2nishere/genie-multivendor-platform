package com.genie.catalog.category.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Đầu trang danh mục — API 5.2. {@code breadcrumb} từ gốc tới chính nó; {@code children} là cấp kế tiếp đang hiển
 * thị; {@code productCount} = sản phẩm đang bán của cả nhánh; {@code parentId} để web dựng liên kết về danh mục cha.
 */
public record PublicCategoryHeader(long id, String name, String slug, String icon,
                                   @JsonInclude(JsonInclude.Include.ALWAYS) String description,
                                   @JsonInclude(JsonInclude.Include.ALWAYS) Long parentId,
                                   List<Crumb> breadcrumb, List<Crumb> children, long productCount) {

    public record Crumb(long id, String name, String slug) {
    }
}
