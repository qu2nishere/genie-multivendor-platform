package com.genie.catalog.category.application;

import com.fasterxml.jackson.databind.JavaType;
import com.genie.common.cache.JsonCache;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Nguồn đọc danh mục cho cả module (plan B2 C03.01, N5): danh sách phẳng {@code categories} cache 1 giờ
 * ({@code cache:catalog:categories:all}), số sản phẩm theo nhánh. Lỗi Redis → đọc thẳng PostgreSQL ({@link JsonCache}).
 * Mọi thay đổi danh mục / thuộc tính gọi {@link #evictAfterCommit()} — rollback thì cache giữ nguyên.
 */
@Component
public class CategoryCatalog {

    static final Duration TREE_TTL = Duration.ofHours(1);
    static final Duration COUNT_TTL = Duration.ofMinutes(10);
    static final String ALL_KEY = "catalog:categories:all";
    static final String PUBLISHED_COUNT_KEY = "catalog:categories:published-counts";
    /** Tiền tố cache của cả nhóm danh mục + thuộc tính (xóa cùng lúc). */
    static final String CATEGORY_PREFIX = "catalog:categories:";
    public static final String ATTRIBUTE_PREFIX = "catalog:attrs:";

    /** Số sản phẩm / số shop của cả nhánh (ngành + con cháu). */
    public record BranchCount(long categoryId, long products, long shops) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final JsonCache cache;
    private final JavaType rowsType;
    private final JavaType countsType;

    public CategoryCatalog(NamedParameterJdbcTemplate jdbc, JsonCache cache) {
        this.jdbc = jdbc;
        this.cache = cache;
        this.rowsType = cache.listOf(CategoryRow.class);
        this.countsType = cache.listOf(BranchCount.class);
    }

    /** Cây danh mục (kể cả danh mục ẩn) từ cache. */
    public CategoryIndex index() {
        List<CategoryRow> rows = cache.getOrLoad(ALL_KEY, rowsType, TREE_TTL, this::loadRows);
        return new CategoryIndex(rows == null ? List.of() : rows);
    }

    /** Cây đọc thẳng CSDL (màn quản trị cần số mới nhất ngay sau khi sửa). */
    public CategoryIndex freshIndex() {
        return new CategoryIndex(loadRows());
    }

    /** Sản phẩm đang bán theo nhánh (trang công khai — cache 10 phút). */
    public Map<Long, BranchCount> publishedCounts() {
        List<BranchCount> rows = cache.getOrLoad(PUBLISHED_COUNT_KEY, countsType, COUNT_TTL,
                () -> branchCounts("p.status = 'PUBLISHED'"));
        return toMap(rows);
    }

    /** Sản phẩm chưa xóa theo nhánh + số shop khác nhau (màn quản trị — không cache). */
    public Map<Long, BranchCount> adminCounts() {
        return toMap(branchCounts("p.status <> 'DELETED'"));
    }

    /** Số sản phẩm chưa xóa của cả nhánh (thêm thuộc tính bắt buộc — D11; xóa danh mục — D6). */
    public long liveProductsInBranch(long categoryId) {
        Long n = jdbc.queryForObject("""
                WITH RECURSIVE sub AS (
                    SELECT id FROM categories WHERE id = :id
                    UNION ALL SELECT c.id FROM categories c JOIN sub ON c.parent_id = sub.id)
                SELECT count(*) FROM products p WHERE p.category_id IN (SELECT id FROM sub) AND p.status <> 'DELETED'
                """, new MapSqlParameterSource("id", categoryId), Long.class);
        return n == null ? 0 : n;
    }

    /** Đề xuất danh mục đang chờ duyệt (UC-CAT-17 — chỉ đếm cho thẻ "Chờ duyệt"). */
    public long pendingProposals() {
        Long n = jdbc.queryForObject("SELECT count(*) FROM category_proposals WHERE status = 'PENDING'",
                new MapSqlParameterSource(), Long.class);
        return n == null ? 0 : n;
    }

    /** Xóa cache danh mục + thuộc tính sau khi transaction hiện tại commit (không có transaction → xóa ngay). */
    public void evictAfterCommit() {
        Runnable evict = () -> {
            cache.evictPrefix(CATEGORY_PREFIX);
            cache.evictPrefix(ATTRIBUTE_PREFIX);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
    }

    private List<CategoryRow> loadRows() {
        return jdbc.query("""
                SELECT id, name, slug, parent_id, display_order, icon, description, commission_rate, is_active,
                       requires_business_license
                FROM categories
                """, (rs, i) -> new CategoryRow(rs.getLong("id"), rs.getString("name"), rs.getString("slug"),
                (Long) rs.getObject("parent_id", Long.class), (Integer) rs.getObject("display_order", Integer.class),
                rs.getString("icon"), rs.getString("description"), rs.getBigDecimal("commission_rate"),
                rs.getBoolean("is_active"), rs.getBoolean("requires_business_license")));
    }

    private List<BranchCount> branchCounts(String productFilter) {
        return jdbc.query("""
                WITH RECURSIVE tree AS (
                    SELECT id AS root_id, id FROM categories
                    UNION ALL SELECT t.root_id, c.id FROM categories c JOIN tree t ON c.parent_id = t.id)
                SELECT t.root_id, count(p.id) AS products, count(DISTINCT p.vendor_id) AS shops
                FROM tree t LEFT JOIN products p ON p.category_id = t.id AND %s
                GROUP BY t.root_id
                """.formatted(productFilter), new MapSqlParameterSource(),
                (rs, i) -> new BranchCount(rs.getLong("root_id"), rs.getLong("products"), rs.getLong("shops")));
    }

    private static Map<Long, BranchCount> toMap(List<BranchCount> rows) {
        Map<Long, BranchCount> m = new HashMap<>();
        if (rows != null) rows.forEach(r -> m.put(r.categoryId(), r));
        return m;
    }
}
