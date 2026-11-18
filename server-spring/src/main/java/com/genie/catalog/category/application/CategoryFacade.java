package com.genie.catalog.category.application;

import com.genie.catalog.category.domain.Category;
import com.genie.catalog.category.domain.CategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của danh mục cho module khác (TS 4.4) — vd. form mở shop kiểm ngành hàng chính (UC-VND-01; plan Mở shop
 * & KYC K03.02). Chỉ trả kiểu đơn giản, không trả entity.
 */
@Component
public class CategoryFacade {

    private final CategoryRepository categories;

    public CategoryFacade(CategoryRepository categories) {
        this.categories = categories;
    }

    /** Danh mục {@code categoryId} tồn tại và đang hiển thị. */
    @Transactional(readOnly = true)
    public boolean isActive(long categoryId) {
        return categories.findById(categoryId).map(Category::getIsActive).map(Boolean.TRUE::equals).orElse(false);
    }
}
