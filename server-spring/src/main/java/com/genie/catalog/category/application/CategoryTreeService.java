// Cây danh mục công khai — API 5.1, 5.2 (UC-CAT-01; plan B2 mục 3.1, C03, D5)
package com.genie.catalog.category.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Chỉ danh mục đang hiển thị: ẩn một ngành thì cả nhánh con biến mất khỏi cây công khai (cờ của con không đổi — D5);
 * trang của danh mục ẩn hoặc nằm dưới cha ẩn → {@code 404 GNE-CAT-4041}. Đọc từ cache ({@link CategoryCatalog}).
 */
@Service
public class CategoryTreeService {

    private final CategoryCatalog catalog;

    public CategoryTreeService(CategoryCatalog catalog) {
        this.catalog = catalog;
    }

    public List<PublicCategoryNode> publicTree() {
        CategoryIndex idx = catalog.index();
        return idx.roots().stream().filter(CategoryRow::active).map(r -> node(idx, r)).toList();
    }

    public PublicCategoryHeader header(String slug) {
        CategoryIndex idx = catalog.index();
        CategoryRow row = idx.findBySlug(slug).filter(r -> idx.visible(r.id()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        List<PublicCategoryHeader.Crumb> crumbs = new java.util.ArrayList<>(idx.ancestors(row.id()).stream()
                .map(CategoryTreeService::crumb).toList());
        crumbs.add(crumb(row));
        List<PublicCategoryHeader.Crumb> children = idx.children(row.id()).stream().filter(CategoryRow::active)
                .map(CategoryTreeService::crumb).toList();
        Map<Long, CategoryCatalog.BranchCount> counts = catalog.publishedCounts();
        CategoryCatalog.BranchCount c = counts.get(row.id());
        return new PublicCategoryHeader(row.id(), row.name(), row.slug(), row.icon(), row.description(),
                row.parentId(), crumbs, children, c == null ? 0 : c.products());
    }

    private static PublicCategoryNode node(CategoryIndex idx, CategoryRow r) {
        return new PublicCategoryNode(r.id(), r.name(), r.slug(), r.icon(), idx.children(r.id()).stream()
                .filter(CategoryRow::active).map(c -> node(idx, c)).toList());
    }

    private static PublicCategoryHeader.Crumb crumb(CategoryRow r) {
        return new PublicCategoryHeader.Crumb(r.id(), r.name(), r.slug());
    }
}
