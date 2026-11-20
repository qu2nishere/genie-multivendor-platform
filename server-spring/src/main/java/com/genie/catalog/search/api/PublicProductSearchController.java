package com.genie.catalog.search.api;

import com.genie.catalog.product.application.ProductCard;
import com.genie.catalog.search.application.ProductSearchCriteria;
import com.genie.catalog.search.application.ProductSearchService;
import com.genie.catalog.search.application.SearchFacets;
import com.genie.catalog.search.application.SearchKeywordCounter;
import com.genie.catalog.search.domain.KeywordNormalizer;
import com.genie.catalog.search.domain.ProductSort;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.PageResponse;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/v1/public/products} — tìm kiếm, lọc, sắp xếp sản phẩm (API_SPEC 5.3; plan 2026-11-17 C02.07).
 * Dùng cho trang tìm kiếm, lưới danh mục, lưới & "Tìm trong Shop" của trang shop.
 */
@RestController
@RequestMapping("/api/v1/public/products")
@Tag(name = "Catalog — tìm kiếm")
public class PublicProductSearchController {

    static final int RATE_LIMIT_PER_MINUTE = 60;
    static final int MAX_PAGE = 100;
    static final int MAX_SIZE = 60;
    static final Pattern ATTR_CODE = Pattern.compile("^[a-z][a-z0-9_]*$");

    private final ProductSearchService search;
    private final SearchKeywordCounter keywordCounter;
    private final RateLimiter rateLimiter;
    private final ApiResponseFactory responses;

    public PublicProductSearchController(ProductSearchService search, SearchKeywordCounter keywordCounter,
                                         RateLimiter rateLimiter, ApiResponseFactory responses) {
        this.search = search;
        this.keywordCounter = keywordCounter;
        this.rateLimiter = rateLimiter;
        this.responses = responses;
    }

    @GetMapping
    @Operation(summary = "UC-CAT-02, UC-CAT-05, UC-CAT-16 Tìm kiếm, lọc nâng cao & sắp xếp sản phẩm")
    public ResponseEntity<PageResponse<ProductCard>> search(
            @RequestParam(required = false) String q,
            @RequestParam(name = "shop_id", required = false) Long shopId,
            @RequestParam(name = "category_id", required = false) Long categoryId,
            @RequestParam(name = "brand_ids", required = false) List<Long> brandIds,
            @RequestParam(name = "province_ids", required = false) List<Integer> provinceIds,
            @RequestParam(name = "price_min", required = false) Long priceMin,
            @RequestParam(name = "price_max", required = false) Long priceMax,
            @RequestParam(name = "rating_min", required = false) Integer ratingMin,
            @RequestParam(name = "is_mall", required = false) Boolean isMall,
            @RequestParam(name = "free_shipping", required = false) Boolean freeShipping,
            @RequestParam(name = "has_shop_voucher", required = false) Boolean hasShopVoucher,
            @RequestParam(name = "discount_min", required = false) Integer discountMin,
            @RequestParam(required = false) ProductSort sort,
            @RequestParam(name = "include_facets", defaultValue = "false") boolean includeFacets,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam MultiValueMap<String, String> all,
            ClientInfo client) {
        rateLimiter.check("rl:search:" + client.ip(), RATE_LIMIT_PER_MINUTE, Duration.ofMinutes(1));
        validate(page, size, priceMin, priceMax, ratingMin, discountMin);

        String keyword = KeywordNormalizer.normalizeUnaccented(q);
        ProductSearchCriteria criteria = new ProductSearchCriteria(keyword, shopId, categoryId, brandIds, provinceIds,
                priceMin, priceMax, ratingMin, isMall, freeShipping, hasShopVoucher, attributes(all), discountMin, sort,
                includeFacets, page, size);
        ProductSearchService.SearchResult result = search.search(criteria);

        if (page == 1 && result.totalItems() > 0 && shopId == null) {
            keywordCounter.record(KeywordNormalizer.normalize(q), client);   // UC-CAT-04 bước 5 — chỉ đếm lượt tìm toàn sàn có kết quả
        }

        Object summary = includeFacets ? new SearchFacets.Summary(result.facets()) : null;
        return responses.page(result.items(), PageResponse.Pagination.of(page, size, result.totalItems()), summary);
    }

    private static void validate(int page, int size, Long priceMin, Long priceMax, Integer ratingMin, Integer discountMin) {
        if (page < 1 || page > MAX_PAGE) {
            throw invalid("page phải từ 1 đến " + MAX_PAGE + ".");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw invalid("size phải từ 1 đến " + MAX_SIZE + ".");
        }
        if ((priceMin != null && priceMin < 0) || (priceMax != null && priceMax < 0)
                || (priceMin != null && priceMax != null && priceMin > priceMax)) {
            throw invalid("Khoảng giá không hợp lệ.");
        }
        if (ratingMin != null && (ratingMin < 1 || ratingMin > 5)) {
            throw invalid("rating_min phải từ 1 đến 5.");
        }
        if (discountMin != null && (discountMin < 0 || discountMin > 100)) {
            throw invalid("discount_min phải từ 0 đến 100.");
        }
    }

    /** {@code attr.warranty=12 tháng&attr.warranty=24 tháng} → {@code {warranty: [12 tháng, 24 tháng]}}. */
    private static Map<String, List<String>> attributes(MultiValueMap<String, String> all) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        all.forEach((key, values) -> {
            if (!key.startsWith("attr.")) {
                return;
            }
            String code = key.substring("attr.".length());
            if (!ATTR_CODE.matcher(code).matches()) {
                throw invalid("Mã thuộc tính không hợp lệ: " + code);
            }
            List<String> vals = values.stream().filter(v -> v != null && !v.isBlank()).map(String::strip).toList();
            if (!vals.isEmpty()) {
                out.put(code, vals);
            }
        });
        return out;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.VALIDATION_FAILED, message);
    }
}
