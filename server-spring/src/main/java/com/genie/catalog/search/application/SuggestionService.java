package com.genie.catalog.search.application;

import com.genie.catalog.product.application.MediaUrls;
import com.genie.catalog.search.domain.KeywordNormalizer;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gợi ý khi gõ — {@code GET /public/search/suggestions?q=tai} (UC-CAT-04 bước 2–3; API_SPEC 5.4; plan 2026-11-17 A5).
 * <p><b>Thuật toán:</b> ba nguồn — danh mục (tối đa 2), shop (2), sản phẩm (5) — mỗi nguồn khớp trên tên đã bỏ dấu:
 * <ol>
 *   <li>khớp <i>tiền tố</i> ({@code LIKE 'tai%'}) xếp trước;</li>
 *   <li>sau đó khớp <i>trigram</i> {@code word_similarity ≥ 0,3} (chịu gõ sai, dùng index GIN trên tên bỏ dấu của sản phẩm);</li>
 *   <li>cùng nhóm thì độ giống cao hơn trước, sản phẩm bán chạy hơn trước.</li>
 * </ol>
 * Dưới {@value #MIN_LENGTH} ký tự → rỗng (client cũng chặn + debounce 250 ms).
 */
@Service
public class SuggestionService {

    static final int MIN_LENGTH = 2;

    public record CategorySuggestion(long id, String name, String slug, String icon) {
    }

    public record ShopSuggestion(long id, String name) {
    }

    public record ProductSuggestion(long id, String name, String imageUrl) {
    }

    public record Suggestions(List<CategorySuggestion> categories, List<ShopSuggestion> shops, List<ProductSuggestion> products) {

        static Suggestions empty() {
            return new Suggestions(List.of(), List.of(), List.of());
        }
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final MediaUrls media;

    public SuggestionService(NamedParameterJdbcTemplate jdbc, MediaUrls media) {
        this.jdbc = jdbc;
        this.media = media;
    }

    @Transactional(readOnly = true)
    public Suggestions suggest(String rawQuery) {
        String q = KeywordNormalizer.normalizeUnaccented(rawQuery);
        if (q.length() < MIN_LENGTH) {
            return Suggestions.empty();
        }
        jdbc.getJdbcTemplate().queryForList(
                "SELECT set_config('pg_trgm.word_similarity_threshold', '0.3', true), set_config('statement_timeout', '1000', true)");
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("q", q).addValue("prefix", q + "%");

        List<CategorySuggestion> categories = jdbc.query("""
                SELECT id, name, slug, icon FROM categories
                 WHERE is_active = TRUE
                   AND (immutable_unaccent(lower(name)) LIKE :prefix OR :q <% immutable_unaccent(lower(name)))
                 ORDER BY (immutable_unaccent(lower(name)) LIKE :prefix) DESC,
                          word_similarity(:q, immutable_unaccent(lower(name))) DESC, id
                 LIMIT 2""", p,
                (rs, i) -> new CategorySuggestion(rs.getLong("id"), rs.getString("name"), rs.getString("slug"), rs.getString("icon")));

        List<ShopSuggestion> shops = jdbc.query("""
                SELECT vp.id, vp.shop_name FROM vendor_profiles vp JOIN users u ON u.id = vp.user_id
                 WHERE vp.kyc_status = 'VERIFIED' AND u.status = 'ACTIVE'
                   AND (immutable_unaccent(lower(vp.shop_name)) LIKE :prefix OR :q <% immutable_unaccent(lower(vp.shop_name)))
                 ORDER BY (immutable_unaccent(lower(vp.shop_name)) LIKE :prefix) DESC,
                          word_similarity(:q, immutable_unaccent(lower(vp.shop_name))) DESC, vp.follower_count DESC, vp.id
                 LIMIT 2""", p, (rs, i) -> new ShopSuggestion(rs.getLong("id"), rs.getString("shop_name")));

        List<ProductSuggestion> products = jdbc.query("""
                SELECT p.id, p.name,
                       (SELECT i.image_url FROM product_images i WHERE i.product_id = p.id AND i.variant_id IS NULL
                         ORDER BY i.is_primary DESC NULLS LAST, i.display_order, i.id LIMIT 1) AS image_url
                  FROM products p
                  JOIN users u ON u.id = p.vendor_id
                  JOIN vendor_profiles vp ON vp.user_id = p.vendor_id
                 WHERE p.status = 'PUBLISHED' AND u.status = 'ACTIVE' AND vp.kyc_status = 'VERIFIED'
                   AND :q <% immutable_unaccent(lower(p.name))
                 ORDER BY (immutable_unaccent(lower(p.name)) LIKE :prefix) DESC,
                          word_similarity(:q, immutable_unaccent(lower(p.name))) DESC, p.sold DESC NULLS LAST, p.id DESC
                 LIMIT 5""", p,
                (rs, i) -> new ProductSuggestion(rs.getLong("id"), rs.getString("name"), media.product(rs.getString("image_url"))));

        return new Suggestions(categories, shops, products);
    }
}
