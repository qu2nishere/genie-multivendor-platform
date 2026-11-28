package com.genie.catalog.category.application;

import com.genie.catalog.attribute.application.AttributeView;
import com.genie.catalog.attribute.application.ListingAttributes;
import com.genie.catalog.category.domain.Category;
import com.genie.catalog.category.domain.CategoryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của danh mục cho module khác (TS 4.4) — vd. form mở shop kiểm ngành hàng chính (UC-VND-01; plan Mở shop
 * & KYC K03.02), plan đơn hàng chụp hoa hồng vào {@code order_items} (plan B2 C05.03). Chỉ trả kiểu đơn giản, không trả
 * entity.
 */
@Component
public class CategoryFacade {

    private final CategoryRepository categories;
    private final CategoryCatalog catalog;
    private final CommissionService commissions;
    private final ListingAttributes listingAttributes;

    public CategoryFacade(CategoryRepository categories, CategoryCatalog catalog, CommissionService commissions,
                          ListingAttributes listingAttributes) {
        this.categories = categories;
        this.catalog = catalog;
        this.commissions = commissions;
        this.listingAttributes = listingAttributes;
    }

    /** Danh mục {@code categoryId} tồn tại và đang hiển thị. */
    @Transactional(readOnly = true)
    public boolean isActive(long categoryId) {
        return categories.findById(categoryId).map(Category::getIsActive).map(Boolean.TRUE::equals).orElse(false);
    }

    /**
     * Hoa hồng áp cho sản phẩm thuộc ngành {@code categoryId} (phân số, vd {@code 0.0500}): riêng của ngành → ngành cha
     * gần nhất có cấu hình → mặc định toàn sàn. Hoa hồng riêng của shop ({@code VendorProfile}) do module order / vendor
     * ghép trước bước này — UC-CAT-14 bước 8.
     */
    public BigDecimal effectiveCommissionRate(long categoryId) {
        return commissions.effectiveRate(catalog.index(), categoryId, commissions.defaultRate());
    }

    /**
     * Thuộc tính đang dùng của ngành, gồm cả kế thừa từ ngành cha, xếp từ gốc xuống — form đăng bán & kiểm thuộc tính bắt
     * buộc khi gửi duyệt (UC-CAT-09; plan C1 D8, plan B2 C06.02). Ngành không tồn tại → danh sách rỗng.
     */
    public List<AttributeView> attributesFor(long categoryId) {
        return listingAttributes.effectiveFor(categoryId);
    }
}
