package com.genie.catalog.product.application;

import com.genie.catalog.search.domain.SlugUtil;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Dựng {@link ProductCard} THEO LÔ cho một danh sách ID đã xếp thứ tự (plan 2026-11-17 C02.04) — một câu SQL cho cả
 * trang, không N+1. Dùng chung cho tìm kiếm, shop, tương tự, gợi ý.
 * <p>Đọc thẳng bảng {@code vendor_profiles}, {@code vendor_shipping_config} (read-model — quyết định D6).
 */
@Component
public class ProductCardAssembler {

    static final String SQL = """
            SELECT p.id, p.name, p.base_price, p.sold, p.rating, p.is_pre_order,
                   vp.id AS shop_id, vp.shop_name, vp.is_mall,
                   (vsc.freeship_threshold IS NOT NULL) AS free_ship,
                   (vp.vacation_starts_at IS NOT NULL AND vp.vacation_starts_at <= :now
                        AND (vp.vacation_ends_at IS NULL OR vp.vacation_ends_at > :now)) AS on_vacation,
                   (SELECT MIN(v.price) FROM product_variants v
                     WHERE v.product_id = p.id AND v.deleted_at IS NULL) AS min_price,
                   (SELECT i.image_url FROM product_images i
                     WHERE i.product_id = p.id AND i.variant_id IS NULL
                     ORDER BY i.is_primary DESC NULLS LAST, i.display_order, i.id LIMIT 1) AS image_url
              FROM products p
              JOIN vendor_profiles vp ON vp.user_id = p.vendor_id
              LEFT JOIN vendor_shipping_config vsc ON vsc.vendor_id = p.vendor_id
             WHERE p.id IN (:ids)
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final MediaUrls media;
    private final Clock clock;

    public ProductCardAssembler(NamedParameterJdbcTemplate jdbc, MediaUrls media, Clock clock) {
        this.jdbc = jdbc;
        this.media = media;
        this.clock = clock;
    }

    /** Thẻ theo đúng thứ tự {@code ids}; ID không còn tồn tại bị bỏ qua. */
    public List<ProductCard> load(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", ids)
                .addValue("now", Timestamp.from(clock.instant()));
        Map<Long, ProductCard> byId = new HashMap<>();
        jdbc.query(SQL, params, rs -> {
            ProductCard card = map(rs);
            byId.put(card.id(), card);
        });
        List<ProductCard> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            ProductCard c = byId.get(id);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    private ProductCard map(ResultSet rs) throws SQLException {
        BigDecimal base = rs.getBigDecimal("base_price");
        long price = Pricing.price(rs.getBigDecimal("min_price"), base);
        Long original = Pricing.originalPrice(price, base);
        String name = rs.getString("name");
        double rating = rs.getDouble("rating");
        return new ProductCard(
                rs.getLong("id"),
                SlugUtil.slugify(name),
                name,
                media.product(rs.getString("image_url")),
                price,
                original,
                Pricing.discountPercent(price, original),
                Math.round(rating * 10) / 10.0,
                rs.getLong("sold"),
                new ProductCard.ShopRef(rs.getLong("shop_id"), rs.getString("shop_name"), rs.getBoolean("is_mall")),
                rs.getBoolean("free_ship"),
                rs.getBoolean("is_pre_order"),
                rs.getBoolean("on_vacation"),
                null);
    }
}
