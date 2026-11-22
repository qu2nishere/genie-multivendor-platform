package com.genie.catalog.search.application;

import com.genie.catalog.product.application.ProductCard;
import com.genie.catalog.product.application.ProductCardAssembler;
import com.genie.catalog.search.application.ProductSearchSqlBuilder.Exclude;
import com.genie.catalog.search.application.ProductSearchSqlBuilder.TextMode;
import com.genie.catalog.search.application.ProductSearchSqlBuilder.Where;
import com.genie.catalog.search.domain.ProductSort;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tìm kiếm & lọc sản phẩm (UC-CAT-02, UC-CAT-05, UC-CAT-16; API_SPEC 5.3; plan 2026-11-17 A2–A4, A13).
 * <p><b>Thuật toán 2 tầng:</b>
 * <ol>
 *   <li>Tầng 1 — full-text: {@code search_vector @@ to_tsquery('simple', 'tai & nghe:*')} (từ cuối khớp tiền tố),
 *       xếp hạng {@code ts_rank_cd} trên vector có trọng số (tên A, thương hiệu B, mô tả D);</li>
 *   <li>Tầng 2 — chỉ khi tầng 1 có ít hơn {@value #FALLBACK_THRESHOLD} kết quả: bù gõ sai bằng trigram
 *       {@code word_similarity ≥ 0,4} trên tên bỏ dấu; kết quả tầng 2 nối sau tầng 1. Ngưỡng 0,4 (UC ghi 0,3) đo trên dữ liệu
 *       thật 2026-11-19: lỗi gõ thật đạt ≥ 0,42 ("tai nge bluetoth" ↔ "Tai nghe Bluetooth…" 0,64), còn 0,30 là khớp nhầm
 *       ("ao thun" ↔ "…thể thao…").</li>
 * </ol>
 * Ở chế độ bù gõ sai, facet đếm trên chính tập kết quả (không còn "disjunctive") — đánh đổi để giữ p95 &lt; 300 ms.
 * Mỗi request chạy trong transaction chỉ-đọc có {@code statement_timeout = 2s} (truy vấn chậm → lỗi thay vì treo pool).
 */
@Service
public class ProductSearchService {

    static final int FALLBACK_THRESHOLD = 5;
    static final int FUZZY_LIMIT = 100;
    static final String SIMILARITY_THRESHOLD = "0.4";

    private final NamedParameterJdbcTemplate jdbc;
    private final ProductCardAssembler cards;
    private final ShopVoucherAvailability vouchers;
    private final FacetService facets;
    private final Clock clock;

    public ProductSearchService(NamedParameterJdbcTemplate jdbc, ProductCardAssembler cards,
                                ShopVoucherAvailability vouchers, FacetService facets, Clock clock) {
        this.jdbc = jdbc;
        this.cards = cards;
        this.vouchers = vouchers;
        this.facets = facets;
        this.clock = clock;
    }

    /** Kết quả một trang. {@code facets} = {@code null} khi không yêu cầu. */
    public record SearchResult(List<ProductCard> items, long totalItems, SearchFacets facets, boolean fuzzy) {
    }

    @Transactional(readOnly = true)
    public SearchResult search(ProductSearchCriteria c) {
        jdbc.getJdbcTemplate().queryForList(
                "SELECT set_config('pg_trgm.word_similarity_threshold', ?, true), set_config('statement_timeout', '2000', true)",
                SIMILARITY_THRESHOLD);
        Instant now = clock.instant();
        List<Long> voucherVendorIds = vouchers.vendorIdsWithActiveVoucher().orElse(null);
        int offset = (c.page() - 1) * c.size();

        if (!c.hasKeyword()) {
            Where w = ProductSearchSqlBuilder.where(c, TextMode.NONE, Exclude.NONE, now, voucherVendorIds);
            long total = count(w);
            List<Long> ids = total <= offset ? List.of() : ids(w, c.effectiveSort(), c.size(), offset);
            return new SearchResult(cards.load(ids), total, facetsOrNull(c, TextMode.NONE, now, voucherVendorIds), false);
        }

        Where fulltext = ProductSearchSqlBuilder.where(c, TextMode.FULLTEXT, Exclude.NONE, now, voucherVendorIds);
        long fulltextTotal = count(fulltext);
        if (fulltextTotal >= FALLBACK_THRESHOLD) {
            List<Long> ids = fulltextTotal <= offset ? List.of() : ids(fulltext, c.effectiveSort(), c.size(), offset);
            return new SearchResult(cards.load(ids), fulltextTotal, facetsOrNull(c, TextMode.FULLTEXT, now, voucherVendorIds), false);
        }

        // Tầng 2 — gộp tầng 1 (ít) + trigram, phân trang trong bộ nhớ (tối đa FALLBACK_THRESHOLD + FUZZY_LIMIT dòng)
        List<Long> all = new ArrayList<>(ids(fulltext, ProductSort.RELEVANCE, FALLBACK_THRESHOLD, 0));
        Where fuzzy = ProductSearchSqlBuilder.where(c, TextMode.FUZZY_ONLY, Exclude.NONE, now, voucherVendorIds);
        MapSqlParameterSource p = fuzzy.params().addValue("limit", FUZZY_LIMIT);
        all.addAll(jdbc.queryForList(ProductSearchSqlBuilder.selectFuzzyIds(fuzzy), p, Long.class));
        List<Long> page;
        if (c.effectiveSort() == ProductSort.RELEVANCE || all.isEmpty()) {
            page = offset >= all.size() ? List.of() : all.subList(offset, Math.min(all.size(), offset + c.size()));
        } else {
            // Người dùng chọn Giá / Mới nhất / Bán chạy → sắp lại đúng tập kết quả theo tiêu chí đó (không giữ thứ tự độ liên quan)
            Where inResult = ProductSearchSqlBuilder.where(c, TextMode.RESULT_IDS, Exclude.NONE, now, voucherVendorIds, all);
            page = ids(inResult, c.effectiveSort(), c.size(), offset);
        }
        boolean usedFuzzy = all.size() > fulltextTotal;
        // Facet ở chế độ bù gõ sai đếm trên đúng tập kết quả (≤ 105 ID) — không quét trigram lại cho từng facet
        SearchFacets f = !c.includeFacets() ? null : usedFuzzy
                ? facets.compute(c, TextMode.RESULT_IDS, now, voucherVendorIds, all)
                : facets.compute(c, TextMode.FULLTEXT, now, voucherVendorIds, List.of());
        return new SearchResult(cards.load(page), all.size(), f, usedFuzzy);
    }

    private SearchFacets facetsOrNull(ProductSearchCriteria c, TextMode mode, Instant now, List<Long> voucherVendorIds) {
        return c.includeFacets() ? facets.compute(c, mode, now, voucherVendorIds, List.of()) : null;
    }

    private long count(Where w) {
        Long n = jdbc.queryForObject(ProductSearchSqlBuilder.count(w), w.params(), Long.class);
        return n == null ? 0 : n;
    }

    private List<Long> ids(Where w, ProductSort sort, int limit, int offset) {
        MapSqlParameterSource p = w.params().addValue("limit", limit).addValue("offset", offset);
        return jdbc.queryForList(ProductSearchSqlBuilder.selectIds(w, sort), p, Long.class);
    }
}
