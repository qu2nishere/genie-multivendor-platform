// Sản phẩm theo danh mục cho màn Admin › Danh mục › Xem sản phẩm — API_SPEC 11.11 GET /admin/products (UC-CAT-11; plan C1 P05.03, D19)
package com.genie.catalog.moderation.application;

import com.genie.catalog.product.application.MediaUrls;
import com.genie.common.api.PageResponse;
import com.genie.common.error.InvalidRequestException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projection (không nạp entity — {@code Product.category} EAGER). {@code category_id} tính cả nhánh con. Tab: {@code ALL}
 * (trừ nháp và đã xóa), {@code PENDING} (chờ duyệt + AI nghi ngờ), {@code PUBLISHED}, {@code HIDDEN_OR_LOCKED}. Phân trang
 * theo số trang; {@code summary.counts} đếm cả 4 tab trong cùng nhánh danh mục.
 */
@Service
public class AdminCatalogProductService {

    public static final List<String> TABS = List.of("ALL", "PENDING", "PUBLISHED", "HIDDEN_OR_LOCKED");

    /** Một dòng bảng — {@code status} gộp FLAGGED vào PENDING_APPROVAL (web chỉ có 4 trạng thái). */
    public record Row(long id, String name, String imageUrl, String shopName, long price, long stock, String status,
                      boolean isLocked, String reason) {
    }

    public record Page(List<Row> items, PageResponse.Pagination pagination, Map<String, Long> counts) {
    }

    private static final String SUBTREE = """
            WITH RECURSIVE sub AS (
                SELECT id FROM categories WHERE id = :category
                UNION ALL
                SELECT c.id FROM categories c JOIN sub ON c.parent_id = sub.id
            )
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final MediaUrls media;

    public AdminCatalogProductService(NamedParameterJdbcTemplate jdbc, MediaUrls media) {
        this.jdbc = jdbc;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public Page list(Long categoryId, String tab, Integer page, Integer size) {
        if (categoryId == null) {
            throw InvalidRequestException.of("category_id", "REQUIRED", null, "Chọn danh mục.");
        }
        String t = tab == null || tab.isBlank() ? "ALL" : tab.trim().toUpperCase();
        if (!TABS.contains(t)) {
            throw InvalidRequestException.of("tab", "INVALID_VALUE", tab, "Tab: " + String.join(", ", TABS) + ".");
        }
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null ? 20 : Math.max(1, Math.min(size, 100));
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("category", categoryId)
                .addValue("limit", s).addValue("offset", (p - 1) * s);

        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(SUBTREE + """
                SELECT count(*) AS all_count,
                       count(*) FILTER (WHERE p.status IN ('PENDING_APPROVAL', 'FLAGGED')) AS pending,
                       count(*) FILTER (WHERE p.status = 'PUBLISHED') AS published,
                       count(*) FILTER (WHERE p.status = 'HIDDEN') AS hidden
                  FROM products p
                 WHERE p.category_id IN (SELECT id FROM sub) AND p.status NOT IN ('DRAFT', 'DELETED')""",
                params, rs -> {
                    counts.put("ALL", rs.getLong("all_count"));
                    counts.put("PENDING", rs.getLong("pending"));
                    counts.put("PUBLISHED", rs.getLong("published"));
                    counts.put("HIDDEN_OR_LOCKED", rs.getLong("hidden"));
                });
        TABS.forEach(x -> counts.putIfAbsent(x, 0L));

        // Khối văn bản """ cắt khoảng trắng đầu dòng → tự thêm dấu cách trước AND
        String filter = switch (t) {
            case "PENDING" -> " AND p.status IN ('PENDING_APPROVAL', 'FLAGGED')";
            case "PUBLISHED" -> " AND p.status = 'PUBLISHED'";
            case "HIDDEN_OR_LOCKED" -> " AND p.status = 'HIDDEN'";
            default -> "";
        };
        List<Row> rows = jdbc.query(SUBTREE + """
                SELECT p.id, p.name, p.status, p.is_locked, p.moderation_reason, p.base_price, vp.shop_name,
                       (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id AND i.variant_id IS NULL
                         ORDER BY i.is_primary DESC NULLS LAST, i.display_order, i.id LIMIT 1) AS image,
                       (SELECT COALESCE(sum(v.stock_count), 0) FROM product_variants v
                         WHERE v.product_id = p.id AND v.deleted_at IS NULL) AS stock
                  FROM products p LEFT JOIN vendor_profiles vp ON vp.user_id = p.vendor_id
                 WHERE p.category_id IN (SELECT id FROM sub) AND p.status NOT IN ('DRAFT', 'DELETED')"""
                + filter + " ORDER BY p.updated_at DESC, p.id DESC LIMIT :limit OFFSET :offset",
                params, (rs, i) -> {
                    String status = rs.getString("status");
                    BigDecimal price = rs.getBigDecimal("base_price");
                    String reason = rs.getString("moderation_reason");
                    boolean locked = rs.getBoolean("is_locked");
                    // lý do chỉ có nghĩa khi bị từ chối / bị sàn khóa
                    boolean showReason = "REJECTED".equals(status) || locked;
                    return new Row(rs.getLong("id"), rs.getString("name"), media.product(rs.getString("image")),
                            rs.getString("shop_name"), price == null ? 0 : price.longValue(), rs.getLong("stock"),
                            "FLAGGED".equals(status) ? "PENDING_APPROVAL" : status, locked,
                            showReason ? reason : null);
                });
        return new Page(rows, PageResponse.Pagination.of(p, s, counts.get(t)), counts);
    }
}
