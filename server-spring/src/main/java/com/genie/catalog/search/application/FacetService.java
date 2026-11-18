package com.genie.catalog.search.application;

import com.genie.catalog.search.application.ProductSearchSqlBuilder.Exclude;
import com.genie.catalog.search.application.ProductSearchSqlBuilder.TextMode;
import com.genie.catalog.search.application.ProductSearchSqlBuilder.Where;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bộ đếm sidebar bộ lọc (UC-CAT-05; API_SPEC 5.3 {@code summary.facets}; plan 2026-11-17 A4).
 * <p><b>Disjunctive faceting:</b> facet của một nhóm được đếm trên tập đã áp mọi bộ lọc <i>trừ chính nhóm đó</i> —
 * đang chọn Sony vẫn thấy số sản phẩm của Apple để chọn thêm. Mỗi facet một câu {@code GROUP BY}, tối đa 20 dòng.
 */
@Component
public class FacetService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ShopVoucherAvailability vouchers;

    public FacetService(NamedParameterJdbcTemplate jdbc, ShopVoucherAvailability vouchers) {
        this.jdbc = jdbc;
        this.vouchers = vouchers;
    }

    public SearchFacets compute(ProductSearchCriteria c, TextMode mode, Instant now, List<Long> voucherVendorIds,
                                List<Long> resultIds) {
        Where brandWhere = ProductSearchSqlBuilder.where(c, mode, Exclude.BRAND, now, voucherVendorIds, resultIds);
        List<SearchFacets.BrandCount> brands = jdbc.query(ProductSearchSqlBuilder.brandFacet(brandWhere), brandWhere.params(),
                (rs, i) -> new SearchFacets.BrandCount(rs.getLong("id"), rs.getString("name"), rs.getLong("cnt")));

        Where locWhere = ProductSearchSqlBuilder.where(c, mode, Exclude.LOCATION, now, voucherVendorIds, resultIds);
        List<SearchFacets.LocationCount> locations = jdbc.query(ProductSearchSqlBuilder.locationFacet(locWhere), locWhere.params(),
                (rs, i) -> new SearchFacets.LocationCount(rs.getInt("province_id"), rs.getString("name"), rs.getLong("cnt")));

        return new SearchFacets(brands, locations, attribute(c, mode, now, voucherVendorIds, resultIds),
                vouchers.vendorIdsWithActiveVoucher().isPresent());
    }

    /** Thuộc tính lọc đầu tiên ({@code is_filterable}, theo {@code display_order}) của danh mục đang chọn. */
    private SearchFacets.AttributeFacet attribute(ProductSearchCriteria c, TextMode mode, Instant now,
                                                 List<Long> voucherVendorIds, List<Long> resultIds) {
        if (c.categoryId() == null) {
            return null;
        }
        List<Map<String, Object>> attr = jdbc.queryForList("""
                SELECT code, name FROM category_attributes
                 WHERE category_id = :cid AND is_filterable = TRUE AND is_active = TRUE
                 ORDER BY display_order, id LIMIT 1""", new MapSqlParameterSource("cid", c.categoryId()));
        if (attr.isEmpty()) {
            return null;
        }
        String code = (String) attr.get(0).get("code");
        Where w = ProductSearchSqlBuilder.where(c, mode, Exclude.ATTRIBUTE, now, voucherVendorIds, resultIds);
        MapSqlParameterSource p = w.params().addValue("facetCode", code);
        List<SearchFacets.ValueCount> values = jdbc.query(ProductSearchSqlBuilder.attributeFacet(w), p,
                (rs, i) -> new SearchFacets.ValueCount(rs.getString("value"), rs.getLong("cnt")));
        return new SearchFacets.AttributeFacet(code, (String) attr.get(0).get("name"), values);
    }
}
