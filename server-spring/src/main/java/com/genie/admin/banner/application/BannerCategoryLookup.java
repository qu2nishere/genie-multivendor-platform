package com.genie.admin.banner.application;

import java.util.Collection;
import java.util.Map;

/**
 * Cổng đảo ngược tra danh mục cho banner {@code CATEGORY_TOP} (plan banner B03.02): module {@code catalog} cài đặt khi có
 * nghiệp vụ danh mục — {@code admin} không phụ thuộc {@code catalog}. Chưa có bean → chỉ kiểm {@code category_id} khác
 * {@code null}, API trả {@code category.name = null}.
 */
public interface BannerCategoryLookup {

    boolean exists(long categoryId);

    /** Tên hiển thị theo id (thiếu id nào thì bỏ qua). */
    Map<Long, String> namesOf(Collection<Long> categoryIds);
}
