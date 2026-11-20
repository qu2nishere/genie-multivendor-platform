package com.genie.catalog.search.application;

import com.genie.catalog.search.domain.KeywordNormalizer;
import com.genie.catalog.search.domain.ProductSort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Dựng câu SQL động cho tìm kiếm & lọc (plan 2026-11-17 thuật toán A2–A4; API_SPEC 5.3).
 * <p>
 * Mọi giá trị người dùng đi qua <b>tham số đặt tên</b> — không nối chuỗi giá trị vào SQL (plan N3); biểu thức ORDER BY
 * lấy từ {@link ProductSort} (danh sách trắng). Đọc chéo bảng {@code users}, {@code vendor_profiles},
 * {@code vendor_shipping_config} bằng SQL thuần là read-model có chủ đích (quyết định D6).
 * <p>
 * Lớp thuần (không truy cập CSDL) để test đơn vị được chuỗi SQL + tham số.
 */
public final class ProductSearchSqlBuilder {

    /** Giá hiển thị = giá thấp nhất SKU còn bán, không có SKU thì base_price (D8). */
    static final String PRICE = "COALESCE(pv.min_price, p.base_price)";

    static final String FROM_BASE = """
             FROM products p
             JOIN users u ON u.id = p.vendor_id
             JOIN vendor_profiles vp ON vp.user_id = p.vendor_id
             LEFT JOIN vendor_shipping_config vsc ON vsc.vendor_id = p.vendor_id
            """;

    /** Giá thấp nhất SKU — chỉ JOIN khi lọc / sắp theo giá (tránh tính MIN cho mọi dòng — đo ở SearchPerformanceIT). */
    static final String PRICE_JOIN = """
             LEFT JOIN LATERAL (SELECT MIN(v.price) AS min_price FROM product_variants v
                                 WHERE v.product_id = p.id AND v.deleted_at IS NULL) pv ON TRUE
            """;

    static final String FULLTEXT = "p.search_vector @@ to_tsquery('simple', :tsq)";
    static final String FUZZY = ":kw <% immutable_unaccent(lower(p.name))";
    static final String RANK = "ts_rank_cd(p.search_vector, to_tsquery('simple', :tsq))";
    static final String SIMILARITY = "word_similarity(:kw, immutable_unaccent(lower(p.name)))";

    /** Cách khớp chữ. */
    public enum TextMode {
        /** không lọc theo chữ */
        NONE,
        /** tầng 1 — full-text có tiền tố */
        FULLTEXT,
        /** tầng 2 — chỉ những sản phẩm khớp trigram mà KHÔNG khớp tầng 1 */
        FUZZY_ONLY,
        /** tầng 1 hoặc tầng 2 */
        FULLTEXT_OR_FUZZY,
        /** đúng tập ID đã tìm được (tham số {@code :resultIds}) — facet ở chế độ bù gõ sai, tránh quét trigram lại */
        RESULT_IDS
    }

    /** Bộ lọc bị bỏ khi đếm facet của chính nó (disjunctive faceting). */
    public enum Exclude { NONE, BRAND, LOCATION, ATTRIBUTE }

    /** Mệnh đề WHERE + tham số; {@code from} có JOIN giá khi cần. */
    public record Where(String sql, MapSqlParameterSource params, String from) {
    }

    private ProductSearchSqlBuilder() {
    }

    /**
     * @param voucherVendorIds vendor_id (users.id) các shop đang có voucher — {@code null} = không hỗ trợ lọc (D10)
     */
    public static Where where(ProductSearchCriteria c, TextMode mode, Exclude exclude, Instant now,
                              List<Long> voucherVendorIds) {
        return where(c, mode, exclude, now, voucherVendorIds, List.of());
    }

    /** @param resultIds chỉ dùng với {@link TextMode#RESULT_IDS} */
    public static Where where(ProductSearchCriteria c, TextMode mode, Exclude exclude, Instant now,
                              List<Long> voucherVendorIds, List<Long> resultIds) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("now", Timestamp.from(now));
        List<String> and = new ArrayList<>();
        and.add("p.status = 'PUBLISHED'");
        and.add("u.status = 'ACTIVE'");
        and.add("vp.kyc_status = 'VERIFIED'");

        if (mode == TextMode.RESULT_IDS) {
            if (resultIds.isEmpty()) {
                and.add("FALSE");
            } else {
                and.add("p.id IN (:resultIds)");
                params.addValue("resultIds", resultIds);
            }
        } else if (c.hasKeyword() && mode != TextMode.NONE) {
            params.addValue("tsq", KeywordNormalizer.prefixTsQuery(c.keyword()));
            params.addValue("kw", c.keyword());
            switch (mode) {
                case FULLTEXT -> and.add(FULLTEXT);
                case FUZZY_ONLY -> and.add(FUZZY + " AND NOT (" + FULLTEXT + ")");
                case FULLTEXT_OR_FUZZY -> and.add("(" + FULLTEXT + " OR " + FUZZY + ")");
                default -> { }
            }
        }
        if (c.shopId() != null) {
            and.add("vp.id = :shopId");
            params.addValue("shopId", c.shopId());
        }
        if (c.categoryId() != null) {
            and.add("""
                    p.category_id IN (WITH RECURSIVE sub AS (
                          SELECT id FROM categories WHERE id = :categoryId
                          UNION ALL SELECT ch.id FROM categories ch JOIN sub ON ch.parent_id = sub.id)
                      SELECT id FROM sub)""");
            params.addValue("categoryId", c.categoryId());
        }
        if (!c.brandIds().isEmpty() && exclude != Exclude.BRAND) {
            and.add("p.brand_id IN (:brandIds)");
            params.addValue("brandIds", c.brandIds());
        }
        if (!c.provinceIds().isEmpty() && exclude != Exclude.LOCATION) {
            and.add("vp.pickup_province_id IN (:provinceIds)");
            params.addValue("provinceIds", c.provinceIds());
        }
        if (c.priceMin() != null) {
            and.add(PRICE + " >= :priceMin");
            params.addValue("priceMin", c.priceMin());
        }
        if (c.priceMax() != null) {
            and.add(PRICE + " <= :priceMax");
            params.addValue("priceMax", c.priceMax());
        }
        if (c.ratingMin() != null) {
            and.add("COALESCE(p.rating, 0) >= :ratingMin");
            params.addValue("ratingMin", c.ratingMin());
        }
        if (Boolean.TRUE.equals(c.isMall())) {
            and.add("vp.is_mall = TRUE");
        }
        if (Boolean.TRUE.equals(c.freeShipping())) {
            and.add("vsc.freeship_threshold IS NOT NULL");
        }
        if (Boolean.TRUE.equals(c.hasShopVoucher()) && voucherVendorIds != null) {
            if (voucherVendorIds.isEmpty()) {
                and.add("FALSE");
            } else {
                and.add("p.vendor_id IN (:voucherVendorIds)");
                params.addValue("voucherVendorIds", voucherVendorIds);
            }
        }
        if (c.discountMin() != null && c.discountMin() > 0) {
            and.add("p.base_price > " + PRICE + " AND (p.base_price - " + PRICE + ") * 100 >= :discountMin * p.base_price");
            params.addValue("discountMin", c.discountMin());
        }
        if (c.attributeFilterApplies() && exclude != Exclude.ATTRIBUTE) {
            int i = 0;
            for (Map.Entry<String, List<String>> e : c.attributes().entrySet()) {
                and.add("(p.specs_json ->> :ac" + i + ") IN (:av" + i + ")");
                params.addValue("ac" + i, e.getKey());
                params.addValue("av" + i, e.getValue());
                i++;
            }
        }
        boolean needsPrice = c.priceMin() != null || c.priceMax() != null
                || (c.discountMin() != null && c.discountMin() > 0)
                || c.effectiveSort() == ProductSort.PRICE_ASC || c.effectiveSort() == ProductSort.PRICE_DESC;
        String sql = " WHERE " + String.join("\n   AND ", and);
        return new Where(sql, params, needsPrice ? FROM_BASE + PRICE_JOIN : FROM_BASE);
    }

    /** ORDER BY theo tiêu chí — luôn kèm {@code p.id DESC} để phân trang ổn định. */
    public static String orderBy(ProductSort sort) {
        return switch (sort) {
            case RELEVANCE -> " ORDER BY " + RANK + " DESC, p.sold DESC NULLS LAST, p.id DESC";
            case NEWEST -> " ORDER BY p.created_at DESC, p.id DESC";
            case BEST_SELLING -> " ORDER BY p.sold DESC NULLS LAST, p.id DESC";
            case PRICE_ASC -> " ORDER BY " + PRICE + " ASC, p.id DESC";
            case PRICE_DESC -> " ORDER BY " + PRICE + " DESC, p.id DESC";
        };
    }

    public static String selectIds(Where w, ProductSort sort) {
        return "SELECT p.id" + w.from() + w.sql() + orderBy(sort) + " LIMIT :limit OFFSET :offset";
    }

    public static String count(Where w) {
        return "SELECT count(*)" + w.from() + w.sql();
    }

    /** Tầng 2: sắp theo độ giống tên rồi bán chạy. */
    public static String selectFuzzyIds(Where w) {
        return "SELECT p.id" + w.from() + w.sql() + " ORDER BY " + SIMILARITY + " DESC, p.sold DESC NULLS LAST, p.id DESC LIMIT :limit";
    }

    public static String brandFacet(Where w) {
        return "SELECT b.id, b.name, count(*) AS cnt" + w.from() + " JOIN brands b ON b.id = p.brand_id" + w.sql()
                + " GROUP BY b.id, b.name ORDER BY cnt DESC, b.name LIMIT 20";
    }

    public static String locationFacet(Where w) {
        return "SELECT vp.pickup_province_id AS province_id, max(vp.pickup_province) AS name, count(*) AS cnt" + w.from() + w.sql()
                + " AND vp.pickup_province_id IS NOT NULL GROUP BY vp.pickup_province_id ORDER BY cnt DESC LIMIT 20";
    }

    public static String attributeFacet(Where w) {
        return "SELECT p.specs_json ->> :facetCode AS value, count(*) AS cnt" + w.from() + w.sql()
                + " AND (p.specs_json ->> :facetCode) IS NOT NULL GROUP BY 1 ORDER BY cnt DESC, 1 LIMIT 20";
    }
}
