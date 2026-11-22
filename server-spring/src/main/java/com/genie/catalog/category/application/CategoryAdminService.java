// Quản lý cây danh mục — API 11.11 (UC-CAT-14 bước 1–7; plan B2 mục 3.2, C04, D3–D7, D15–D17)
package com.genie.catalog.category.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.genie.catalog.category.domain.Category;
import com.genie.catalog.category.domain.CategoryRepository;
import com.genie.common.domain.Slugs;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.common.security.ClientInfo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thêm / sửa / ẩn / xóa danh mục. Quy tắc:
 * <ul>
 *   <li>slug: bỏ trống → sinh từ tên ({@link Slugs#slugify}); trùng slug hoặc trùng tên trong cùng cha →
 *       {@code 409 GNE-CAT-4091} kèm {@code details.existing_category {id, name}} (D3);</li>
 *   <li>tối đa 3 cấp, không chuyển một nhánh vào chính nó / con cháu → {@code 422} {@code field = parent_id} (D4);</li>
 *   <li>xóa chỉ khi không còn con, không còn sản phẩm chưa xóa, không còn voucher trỏ tới → khác thì
 *       {@code 409 GNE-CAT-4092} (D6);</li>
 *   <li>trường {@code commission_rate} cần thêm quyền {@code UPDATE_COMMISSION} khi giá trị thật sự đổi
 *       ({@code 403 GNE-SEC-4030}); đổi thì ghi {@code COMMISSION_UPDATED} (N3, N6).</li>
 * </ul>
 * Mọi thay đổi xóa cache cây / thuộc tính sau commit. Tạo / sửa / xóa ghi log ứng dụng (D15).
 */
@Service
public class CategoryAdminService {

    private static final Logger log = LoggerFactory.getLogger(CategoryAdminService.class);
    static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    static final Pattern ICON = Pattern.compile("^[a-z0-9-]{1,50}$");

    private final CategoryRepository categories;
    private final CategoryCatalog catalog;
    private final CommissionService commissions;

    public CategoryAdminService(CategoryRepository categories, CategoryCatalog catalog, CommissionService commissions) {
        this.categories = categories;
        this.catalog = catalog;
        this.commissions = commissions;
    }

    /** Cây đầy đủ (kể cả ẩn) + {@code summary}. */
    public record AdminTree(List<AdminCategoryView> roots, Map<String, Object> summary) {
    }

    // ===== Đọc =====

    @Transactional(readOnly = true)
    public AdminTree tree() {
        CategoryIndex idx = catalog.freshIndex();
        Map<Long, CategoryCatalog.BranchCount> counts = catalog.adminCounts();
        BigDecimal defaultRate = commissions.defaultRate();
        List<AdminCategoryView> roots = idx.roots().stream().map(r -> view(idx, r, counts, defaultRate)).toList();
        long rootCount = idx.roots().size();
        long products = idx.roots().stream().mapToLong(r -> count(counts, r.id()).products()).sum();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("root_count", rootCount);
        summary.put("child_count", idx.all().size() - rootCount);
        summary.put("product_count", products);
        summary.put("pending_proposals", catalog.pendingProposals());
        return new AdminTree(roots, summary);
    }

    @Transactional(readOnly = true)
    public AdminCategoryView get(long id) {
        CategoryIndex idx = catalog.freshIndex();
        CategoryRow row = idx.find(id).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return view(idx, row, catalog.adminCounts(), commissions.defaultRate());
    }

    // ===== Ghi =====

    @Transactional
    public AdminCategoryView create(JsonNode body, boolean canChangeCommission, long actorId, ClientInfo client) {
        CategoryIndex idx = catalog.freshIndex();
        List<Violation> errors = new ArrayList<>();
        String name = text(body, "name");
        if (name == null || name.isBlank() || name.trim().length() > 255) {
            errors.add(new Violation("name", "LENGTH", name, "Tên danh mục 1–255 ký tự."));
        }
        String cleanName = name == null ? "" : name.trim();
        String slug = slugOf(text(body, "slug"), cleanName, errors);
        Long parentId = longOrNull(body, "parent_id", errors);
        if (parentId != null) {
            if (idx.find(parentId).isEmpty()) {
                errors.add(new Violation("parent_id", "NOT_FOUND", parentId, "Danh mục cha không tồn tại."));
            } else if (idx.level(parentId) >= CategoryIndex.MAX_LEVEL) {
                errors.add(new Violation("parent_id", "TOO_DEEP", parentId, "Cây danh mục tối đa 3 cấp."));
            }
        }
        String icon = iconOf(text(body, "icon"), errors);
        String description = descriptionOf(text(body, "description"), errors);
        BigDecimal rate = rateOf(body, errors);
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);
        if (rate != null && !canChangeCommission) throw new BusinessException(ErrorCode.FORBIDDEN);
        requireUnique(idx, null, slug, cleanName, parentId);

        Category c = Category.builder().name(cleanName).slug(slug).parentId(parentId)
                .displayOrder(categories.nextDisplayOrder(parentId)).icon(icon).description(description)
                .isActive(bool(body, "is_active", true))
                .requiresBusinessLicense(bool(body, "requires_business_license", false)).build();
        if (rate != null) c.changeCommissionRate(rate);
        Category saved = saveUnique(c, slug);
        if (rate != null) commissions.record("CATEGORY", saved.getId(), null, rate, actorId, client);
        log.info("Danh mục #{} \"{}\" được tạo bởi #{}", saved.getId(), saved.getName(), actorId);
        catalog.evictAfterCommit();
        return get(saved.getId());
    }

    /** Sửa trường bất kỳ có trong body (vắng = giữ nguyên; {@code commission_rate: null} = bỏ tỷ lệ riêng). */
    @Transactional
    public AdminCategoryView patch(long id, JsonNode body, boolean canChangeCommission, long actorId,
                                   ClientInfo client) {
        Category c = categories.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        CategoryIndex idx = catalog.freshIndex();
        List<Violation> errors = new ArrayList<>();

        String name = c.getName();
        if (body.has("name")) {
            String v = text(body, "name");
            if (v == null || v.isBlank() || v.trim().length() > 255) {
                errors.add(new Violation("name", "LENGTH", v, "Tên danh mục 1–255 ký tự."));
            } else {
                name = v.trim();
            }
        }
        String slug = body.has("slug") ? slugOf(text(body, "slug"), name, errors) : c.getSlug();
        if (slug == null) slug = slugOf(null, name, errors);
        Long parentId = c.getParentId();
        if (body.has("parent_id")) {
            parentId = longOrNull(body, "parent_id", errors);
            checkMove(idx, id, parentId, errors);
        }
        String icon = body.has("icon") ? iconOf(text(body, "icon"), errors) : c.getIcon();
        String description = body.has("description") ? descriptionOf(text(body, "description"), errors)
                : c.getDescription();
        boolean rateGiven = body.has("commission_rate");
        BigDecimal rate = rateGiven ? rateOf(body, errors) : null;
        if (!errors.isEmpty()) throw new InvalidRequestException(errors);

        BigDecimal before = c.getCommissionRate();
        boolean rateChanges = rateGiven && !CommissionService.sameRate(before, rate);
        if (rateChanges && !canChangeCommission) throw new BusinessException(ErrorCode.FORBIDDEN);
        requireUnique(idx, id, slug, name, parentId);

        c.rename(name, slug);
        if (!java.util.Objects.equals(parentId, c.getParentId())) {
            c.moveTo(parentId, categories.nextDisplayOrder(parentId));
        }
        c.describe(icon, description, body.has("requires_business_license")
                ? bool(body, "requires_business_license", false) : Boolean.TRUE.equals(c.getRequiresBusinessLicense()));
        if (body.has("is_active")) {
            if (bool(body, "is_active", true)) c.show();
            else c.hide();
        }
        if (rateChanges) {
            if (rate == null) c.clearCommissionRate();
            else c.changeCommissionRate(rate);
            commissions.record("CATEGORY", id, before, rate, actorId, client);
        }
        saveUnique(c, slug);
        List<String> fields = new ArrayList<>();
        body.fieldNames().forEachRemaining(fields::add);
        log.info("Danh mục #{} được sửa bởi #{}: {}", id, actorId, fields);
        catalog.evictAfterCommit();
        return get(id);
    }

    @Transactional
    public void delete(long id, long actorId) {
        Category c = categories.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (categories.existsByParentId(id)) throw notDeletable("HAS_CHILDREN");
        if (catalog.liveProductsInBranch(id) > 0) throw notDeletable("HAS_PRODUCTS");
        try {
            categories.delete(c);
            categories.flush();   // FK voucher_categories (không cascade) báo lỗi ngay tại đây
        } catch (DataIntegrityViolationException e) {
            throw notDeletable("IN_USE");
        }
        log.info("Danh mục #{} \"{}\" bị xóa bởi #{}", id, c.getName(), actorId);
        catalog.evictAfterCommit();
    }

    // ===== Kiểm tra =====

    private void requireUnique(CategoryIndex idx, Long selfId, String slug, String name, Long parentId) {
        for (CategoryRow r : idx.all()) {
            if (selfId != null && r.id() == selfId) continue;
            boolean sameSlug = slug.equals(r.slug());
            boolean sameName = java.util.Objects.equals(parentId, r.parentId()) && name.equalsIgnoreCase(r.name());
            if (sameSlug || sameName) {
                throw new BusinessException(ErrorCode.CATEGORY_DUPLICATE, Map.<String, Object>of(
                        "field", sameSlug ? "slug" : "name",
                        "existing_category", Map.of("id", r.id(), "name", r.name())));
            }
        }
    }

    /** D4: cha tồn tại, không phải chính nó / con cháu, và cả nhánh sau khi chuyển vẫn ≤ 3 cấp. */
    static void checkMove(CategoryIndex idx, long id, Long newParentId, List<Violation> errors) {
        if (newParentId == null) {
            if (1 + idx.height(id) > CategoryIndex.MAX_LEVEL) {
                errors.add(new Violation("parent_id", "TOO_DEEP", null, "Cây danh mục tối đa 3 cấp."));
            }
            return;
        }
        if (idx.find(newParentId).isEmpty()) {
            errors.add(new Violation("parent_id", "NOT_FOUND", newParentId, "Danh mục cha không tồn tại."));
            return;
        }
        boolean intoSelf = newParentId == id || idx.descendants(id).stream().anyMatch(d -> d.id() == newParentId);
        if (intoSelf) {
            errors.add(new Violation("parent_id", "CYCLE", newParentId,
                    "Không thể chuyển danh mục vào chính nó hoặc danh mục con của nó."));
        } else if (idx.level(newParentId) + 1 + idx.height(id) > CategoryIndex.MAX_LEVEL) {
            errors.add(new Violation("parent_id", "TOO_DEEP", newParentId, "Cây danh mục tối đa 3 cấp."));
        }
    }

    private Category saveUnique(Category c, String slug) {
        try {
            return categories.saveAndFlush(c);
        } catch (DataIntegrityViolationException e) {   // hai Admin tạo trùng slug cùng lúc (uk_categories_slug — V22)
            throw new BusinessException(ErrorCode.CATEGORY_DUPLICATE, Map.<String, Object>of("field", "slug",
                    "slug", slug));
        }
    }

    private static BusinessException notDeletable(String reason) {
        return new BusinessException(ErrorCode.CATEGORY_NOT_DELETABLE, Map.<String, Object>of("reason", reason));
    }

    static String slugOf(String raw, String name, List<Violation> errors) {
        String slug = raw == null || raw.isBlank() ? Slugs.slugify(name) : raw.trim().toLowerCase(Locale.ROOT);
        if (slug.isEmpty() || slug.length() > 255 || !SLUG.matcher(slug).matches()) {
            errors.add(new Violation("slug", "INVALID_FORMAT", raw,
                    "Đường dẫn chỉ gồm chữ thường không dấu, số và dấu gạch ngang."));
        }
        return slug;
    }

    private static String iconOf(String raw, List<Violation> errors) {
        if (raw == null || raw.isBlank()) return null;
        String icon = raw.trim();
        if (!ICON.matcher(icon).matches()) {
            errors.add(new Violation("icon", "INVALID_FORMAT", raw, "Tên biểu tượng 1–50 ký tự a-z, 0-9, -."));
        }
        return icon;
    }

    private static String descriptionOf(String raw, List<Violation> errors) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.trim().length() > 500) errors.add(new Violation("description", "LENGTH", null, "Mô tả tối đa 500 ký tự."));
        return raw.trim();
    }

    /** {@code commission_rate} phần trăm → phân số; {@code null} hợp lệ (dùng mặc định). Ngoài 3,5–8 % → lỗi. */
    private static BigDecimal rateOf(JsonNode body, List<Violation> errors) {
        JsonNode n = body.get("commission_rate");
        if (n == null || n.isNull()) return null;
        if (!n.isNumber()) {
            errors.add(new Violation("commission_rate", "INVALID_FORMAT", n.asText(), "Hoa hồng phải là số."));
            return null;
        }
        BigDecimal fraction = CommissionRates.toFraction(n.decimalValue());
        if (!CommissionRates.inRange(fraction)) {
            errors.add(new Violation("commission_rate", "OUT_OF_RANGE", n.decimalValue(),
                    "Hoa hồng phải trong khung 3,5–8 %."));
        }
        return fraction;
    }

    private static String text(JsonNode body, String field) {
        JsonNode n = body.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static Long longOrNull(JsonNode body, String field, List<Violation> errors) {
        JsonNode n = body.get(field);
        if (n == null || n.isNull()) return null;
        if (!n.canConvertToLong()) {
            errors.add(new Violation(field, "INVALID_FORMAT", n.asText(), "Phải là số nguyên."));
            return null;
        }
        return n.asLong();
    }

    private static boolean bool(JsonNode body, String field, boolean fallback) {
        JsonNode n = body.get(field);
        return n == null || n.isNull() ? fallback : n.asBoolean(fallback);
    }

    // ===== Ánh xạ =====

    private AdminCategoryView view(CategoryIndex idx, CategoryRow r, Map<Long, CategoryCatalog.BranchCount> counts,
                                   BigDecimal defaultRate) {
        CategoryCatalog.BranchCount c = count(counts, r.id());
        return new AdminCategoryView(r.id(), r.name(), r.slug(), r.icon(), r.description(), r.parentId(),
                idx.level(r.id()), r.active(), r.requiresBusinessLicense(),
                CommissionRates.toPercent(r.commissionRate()),
                CommissionRates.toPercent(commissions.effectiveRate(idx, r.id(), defaultRate)),
                c.products(), c.shops(), idx.children(r.id()).stream().map(ch -> view(idx, ch, counts, defaultRate)).toList());
    }

    private static CategoryCatalog.BranchCount count(Map<Long, CategoryCatalog.BranchCount> counts, long id) {
        return counts.getOrDefault(id, new CategoryCatalog.BranchCount(id, 0, 0));
    }
}
