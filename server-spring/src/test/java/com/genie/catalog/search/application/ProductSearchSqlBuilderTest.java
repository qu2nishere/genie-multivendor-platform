package com.genie.catalog.search.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.search.application.ProductSearchSqlBuilder.Exclude;
import com.genie.catalog.search.application.ProductSearchSqlBuilder.TextMode;
import com.genie.catalog.search.domain.ProductSort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Bộ dựng SQL động (plan 2026-11-17 C02.02) — giá trị luôn là tham số, không nối vào chuỗi SQL. */
class ProductSearchSqlBuilderTest {

    static final Instant NOW = Instant.parse("2026-06-08T07:00:00Z");

    private static ProductSearchCriteria criteria(String kw, List<Long> brands, Map<String, List<String>> attrs, Long categoryId) {
        return new ProductSearchCriteria(kw, 15L, categoryId, brands, List.of(202), 100_000L, 500_000L, 4, true, true, null,
                attrs, 20, ProductSort.PRICE_ASC, true, 1, 20);
    }

    @Test
    void everyFilterBecomesNamedParameter() {
        var w = ProductSearchSqlBuilder.where(criteria("tai nghe'; DROP TABLE products; --", List.of(3L, 7L),
                Map.of("warranty", List.of("12 tháng")), 1L), TextMode.FULLTEXT, Exclude.NONE, NOW, null);

        assertThat(w.sql()).doesNotContain("DROP TABLE").doesNotContain("12 tháng");
        assertThat(w.sql()).contains("p.search_vector @@ to_tsquery('simple', :tsq)", "vp.id = :shopId",
                "p.brand_id IN (:brandIds)", "vp.pickup_province_id IN (:provinceIds)", ":priceMin", ":priceMax",
                ":ratingMin", "vp.is_mall = TRUE", "vsc.freeship_threshold IS NOT NULL", ":discountMin",
                "(p.specs_json ->> :ac0) IN (:av0)", "WITH RECURSIVE");
        assertThat(w.params().getValue("brandIds")).isEqualTo(List.of(3L, 7L));
        assertThat(w.params().getValue("av0")).isEqualTo(List.of("12 tháng"));
    }

    @Test
    void facetExcludesItsOwnFilter() {
        var c = criteria("", List.of(3L), Map.of("warranty", List.of("12 tháng")), 1L);
        assertThat(ProductSearchSqlBuilder.where(c, TextMode.NONE, Exclude.BRAND, NOW, null).sql()).doesNotContain(":brandIds")
                .contains(":provinceIds");
        assertThat(ProductSearchSqlBuilder.where(c, TextMode.NONE, Exclude.LOCATION, NOW, null).sql()).doesNotContain(":provinceIds")
                .contains(":brandIds");
        assertThat(ProductSearchSqlBuilder.where(c, TextMode.NONE, Exclude.ATTRIBUTE, NOW, null).sql()).doesNotContain(":ac0");
    }

    @Test
    void attributeFilterNeedsExactlyOneCategory() {
        var noCategory = criteria("", List.of(), Map.of("warranty", List.of("12 tháng")), null);
        assertThat(ProductSearchSqlBuilder.where(noCategory, TextMode.NONE, Exclude.NONE, NOW, null).sql()).doesNotContain(":ac0");
    }

    @Test
    void fuzzyOnlyExcludesFullTextMatches() {
        var w = ProductSearchSqlBuilder.where(criteria("tai nge", List.of(), Map.of(), null), TextMode.FUZZY_ONLY,
                Exclude.NONE, NOW, null);
        assertThat(w.sql()).contains(":kw <% immutable_unaccent(lower(p.name)) AND NOT (p.search_vector @@");
        assertThat(w.params().getValue("tsq")).isEqualTo("tai & nge:*");
    }

    @Test
    void voucherFilterIgnoredWhenUnsupportedAndEmptyWhenNoShopHasOne() {
        var c = new ProductSearchCriteria("", null, null, null, null, null, null, null, null, null, true, null, null, null,
                false, 1, 20);
        assertThat(ProductSearchSqlBuilder.where(c, TextMode.NONE, Exclude.NONE, NOW, null).sql()).doesNotContain("voucher");
        assertThat(ProductSearchSqlBuilder.where(c, TextMode.NONE, Exclude.NONE, NOW, List.of()).sql()).contains("FALSE");
    }

    @Test
    void orderByAlwaysEndsWithIdForStablePaging() {
        for (ProductSort s : ProductSort.values()) {
            assertThat(ProductSearchSqlBuilder.orderBy(s)).endsWith("p.id DESC");
        }
    }

    @Test
    void effectiveSortDefaults() {
        assertThat(ProductSearchCriteria.simple("ao", 1, 20).effectiveSort()).isEqualTo(ProductSort.RELEVANCE);
        assertThat(ProductSearchCriteria.simple("", 1, 20).effectiveSort()).isEqualTo(ProductSort.NEWEST);
    }
}
