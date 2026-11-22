package com.genie.catalog.product.application;

import com.fasterxml.jackson.databind.JavaType;
import com.genie.catalog.search.domain.KeywordNormalizer;
import com.genie.common.cache.JsonCache;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sản phẩm tương tự — {@code GET /public/products/{id}/similar} (UC-CAT-07; API_SPEC 5.9; plan 2026-11-17 thuật toán A8).
 * Gợi ý <b>dựa trên nội dung</b> (content-based), không dùng AI / vector (UC-CAT-07: để DATN).
 * <ol>
 *   <li><b>Sinh ứng viên:</b> cùng danh mục lá, đang bán, còn hàng, shop không tạm nghỉ, giá trong khoảng ±50%,
 *       loại chính nó — tối đa {@value #CANDIDATES} ứng viên bán chạy nhất;</li>
 *   <li><b>Chấm điểm:</b> {@code score = w1·similarity(tên) + w2·ln(1+sold)/ln(1+max_sold) + w3·rating/5}
 *       (trigram trên tên bỏ dấu; số bán chuẩn hóa log để vài sản phẩm "hot" không lấn át; mặc định 0,5 / 0,3 / 0,2);</li>
 *   <li><b>Bù khi thiếu:</b> mở rộng sang danh mục cha + các danh mục anh em (trừ danh mục lá, bỏ điều kiện giá);
 *       vẫn thiếu thì trả ít hơn.</li>
 * </ol>
 * Chạy được cả khi sản phẩm gốc đã ẩn — trang "Sản phẩm không còn bán" vẫn hiện khối tương tự (UC-CAT-18). Cache 30 phút.
 */
@Service
public class SimilarProductService {

    static final int CANDIDATES = 200;
    static final Duration TTL = Duration.ofMinutes(30);

    private final NamedParameterJdbcTemplate jdbc;
    private final ProductCardAssembler cards;
    private final JsonCache cache;
    private final Clock clock;
    private final JavaType cardsType;
    private final double wName;
    private final double wSold;
    private final double wRating;

    public SimilarProductService(NamedParameterJdbcTemplate jdbc, ProductCardAssembler cards, JsonCache cache, Clock clock,
                                 @Value("${genie.catalog.similar.weight-name:0.5}") double wName,
                                 @Value("${genie.catalog.similar.weight-sold:0.3}") double wSold,
                                 @Value("${genie.catalog.similar.weight-rating:0.2}") double wRating) {
        this.jdbc = jdbc;
        this.cards = cards;
        this.cache = cache;
        this.clock = clock;
        this.cardsType = cache.listOf(ProductCard.class);
        this.wName = wName;
        this.wSold = wSold;
        this.wRating = wRating;
    }

    public List<ProductCard> similar(long productId, int limit) {
        return cache.getOrLoad("catalog:similar:" + productId + ":" + limit, cardsType, TTL, () -> compute(productId, limit));
    }

    @Transactional(readOnly = true)
    List<ProductCard> compute(long productId, int limit) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.name, p.category_id, c.parent_id,
                       COALESCE((SELECT MIN(v.price) FROM product_variants v WHERE v.product_id = p.id AND v.deleted_at IS NULL),
                                p.base_price) AS price
                  FROM products p LEFT JOIN categories c ON c.id = p.category_id
                 WHERE p.id = :id""", new MapSqlParameterSource("id", productId));
        if (rows.isEmpty() || rows.get(0).get("category_id") == null) {
            return List.of();
        }
        Map<String, Object> base = rows.get(0);
        long categoryId = ((Number) base.get("category_id")).longValue();
        BigDecimal price = (BigDecimal) base.get("price");
        String name = KeywordNormalizer.normalizeUnaccented((String) base.get("name"));

        List<Long> ids = new ArrayList<>(rank(productId, name, List.of(categoryId), price, List.of(), limit));
        if (ids.size() < limit && base.get("parent_id") != null) {
            long parentId = ((Number) base.get("parent_id")).longValue();
            // Danh mục cha + các danh mục anh em, TRỪ danh mục lá (sản phẩm lá ngoài khoảng giá vẫn bị loại)
            List<Long> siblings = jdbc.queryForList(
                    "SELECT id FROM categories WHERE (id = :p OR parent_id = :p) AND id <> :leaf",
                    new MapSqlParameterSource("p", parentId).addValue("leaf", categoryId), Long.class);
            if (!siblings.isEmpty()) {
                ids.addAll(rank(productId, name, siblings, null, ids, limit - ids.size()));
            }
        }
        return cards.load(ids);
    }

    /** Một lượt sinh ứng viên + chấm điểm. {@code price == null} → không lọc khoảng giá. */
    private List<Long> rank(long productId, String name, List<Long> categoryIds, BigDecimal price, List<Long> exclude, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("id", productId).addValue("name", name).addValue("cats", categoryIds)
                .addValue("now", Timestamp.from(clock.instant())).addValue("limit", limit).addValue("candidates", CANDIDATES)
                .addValue("wName", wName).addValue("wSold", wSold).addValue("wRating", wRating)
                .addValue("exclude", exclude.isEmpty() ? List.of(-1L) : exclude);
        String priceBand = "";
        if (price != null) {
            priceBand = " AND COALESCE(pv.min_price, p.base_price) BETWEEN :lo AND :hi";
            p.addValue("lo", price.multiply(BigDecimal.valueOf(0.5))).addValue("hi", price.multiply(BigDecimal.valueOf(1.5)));
        }
        return jdbc.queryForList("""
                WITH cand AS (
                    SELECT p.id, COALESCE(p.sold, 0) AS sold, COALESCE(p.rating, 0) AS rating,
                           immutable_unaccent(lower(p.name)) AS name_un
                      FROM products p
                      JOIN users u ON u.id = p.vendor_id
                      JOIN vendor_profiles vp ON vp.user_id = p.vendor_id
                      JOIN LATERAL (SELECT MIN(v.price) AS min_price, MAX(v.stock_count - v.reserved_count) AS max_stock
                                      FROM product_variants v WHERE v.product_id = p.id AND v.deleted_at IS NULL) pv ON TRUE
                     WHERE p.status = 'PUBLISHED' AND u.status = 'ACTIVE' AND vp.kyc_status = 'VERIFIED'
                       AND p.id <> :id AND p.id NOT IN (:exclude) AND p.category_id IN (:cats)
                       AND pv.max_stock > 0
                       AND NOT (vp.vacation_starts_at IS NOT NULL AND vp.vacation_starts_at <= :now
                                AND (vp.vacation_ends_at IS NULL OR vp.vacation_ends_at > :now))
                    """ + priceBand + """
                     ORDER BY p.sold DESC NULLS LAST, p.id DESC
                     LIMIT :candidates)
                SELECT id FROM (
                    SELECT id,
                           :wName * similarity(name_un, :name)
                         + :wSold * COALESCE(ln(1 + sold) / NULLIF(ln(1 + max(sold) OVER ()), 0), 0)
                         + :wRating * rating / 5.0 AS score
                      FROM cand) s
                 ORDER BY score DESC, id DESC
                 LIMIT :limit""", p, Long.class);
    }
}
