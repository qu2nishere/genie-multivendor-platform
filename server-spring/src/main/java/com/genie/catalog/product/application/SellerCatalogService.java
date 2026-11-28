// Danh mục toàn sàn cho người bán + luật đăng bán theo ngành — API_SPEC 10.5 "listing-rules", 10.6 (UC-CAT-09, 15, 17; plan C1 P03)
package com.genie.catalog.product.application;

import com.genie.catalog.attribute.application.AttributeView;
import com.genie.catalog.category.application.CategoryCatalog;
import com.genie.catalog.category.application.CategoryFacade;
import com.genie.catalog.category.application.CategoryIndex;
import com.genie.catalog.category.application.CategoryRow;
import com.genie.catalog.category.application.CommissionRates;
import com.genie.catalog.category.application.CommissionService;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Chỉ đọc cây đã cache của B2 ({@link CategoryCatalog}). Danh mục ẩn (hoặc nằm dưới cha ẩn) không xuất hiện và không
 * đăng bán được. Hoa hồng trả <b>phần trăm</b> (API) — tỷ lệ đang áp dụng (ngành → cha gần nhất → mặc định).
 */
@Service
public class SellerCatalogService {

    /** Nút cây 2 cấp của màn "Danh mục toàn sàn" (API 10.6). {@code productCount} = sản phẩm đang bán của cả nhánh. */
    public record CatalogNode(long id, String name, BigDecimal commissionRate, long productCount,
                              List<CatalogNode> children) {
    }

    public record RuleAttribute(String code, String name, String dataType, List<String> values, String unit,
                                boolean isRequired, boolean isVariantAxis) {
    }

    /** {@code GET /seller/categories/{id}/listing-rules} — thuộc tính gồm cả kế thừa ngành cha. */
    public record ListingRules(long categoryId, BigDecimal commissionRate, boolean requiresDocuments,
                               List<RuleAttribute> attributes) {
    }

    private final CategoryCatalog catalog;
    private final CommissionService commissions;
    private final CategoryFacade categories;

    public SellerCatalogService(CategoryCatalog catalog, CommissionService commissions, CategoryFacade categories) {
        this.catalog = catalog;
        this.commissions = commissions;
        this.categories = categories;
    }

    public List<CatalogNode> tree() {
        CategoryIndex idx = catalog.index();
        Map<Long, CategoryCatalog.BranchCount> counts = catalog.publishedCounts();
        BigDecimal def = commissions.defaultRate();
        return idx.roots().stream().filter(CategoryRow::active)
                .map(root -> node(idx, counts, def, root,
                        idx.children(root.id()).stream().filter(CategoryRow::active)
                                .map(c -> node(idx, counts, def, c, List.of())).toList()))
                .toList();
    }

    public ListingRules rules(long categoryId) {
        CategoryIndex idx = catalog.index();
        if (!idx.visible(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        BigDecimal rate = CommissionRates.toPercent(
                commissions.effectiveRate(idx, categoryId, commissions.defaultRate()));
        List<RuleAttribute> attrs = categories.attributesFor(categoryId).stream()
                .map(SellerCatalogService::rule).toList();
        return new ListingRules(categoryId, rate, requiresDocuments(idx, categoryId), attrs);
    }

    /** Ngành hoặc một ngành tổ tiên là "kinh doanh có điều kiện" (UC-CAT-09 bước 8, 16; plan C1 D7). */
    static boolean requiresDocuments(CategoryIndex idx, long categoryId) {
        boolean self = idx.find(categoryId).map(CategoryRow::requiresBusinessLicense).orElse(false);
        return self || idx.ancestors(categoryId).stream().anyMatch(CategoryRow::requiresBusinessLicense);
    }

    private CatalogNode node(CategoryIndex idx, Map<Long, CategoryCatalog.BranchCount> counts, BigDecimal def,
                             CategoryRow row, List<CatalogNode> children) {
        BigDecimal rate = CommissionRates.toPercent(commissions.effectiveRate(idx, row.id(), def));
        CategoryCatalog.BranchCount c = counts.get(row.id());
        return new CatalogNode(row.id(), row.name(), rate, c == null ? 0 : c.products(), children);
    }

    private static RuleAttribute rule(AttributeView a) {
        return new RuleAttribute(a.code(), a.name(), a.dataType(), a.values(), a.unit(), a.isRequired(),
                a.isVariantAxis());
    }
}
