package com.genie.catalog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import com.genie.support.TestDataBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** {@code GET /public/search/suggestions} (UC-CAT-04; API_SPEC 5.4; plan 2026-11-17 C03.06). */
class SuggestionIT extends CatalogITSupport {

    static final String URL = "/api/v1/public/search/suggestions";

    @Test
    void suggestsCategoriesShopsAndProductsWithoutMarks() throws Exception {
        String w = TestDataBuilder.uniqueWord();
        long cat = data.category("Đồng hồ " + w, null);
        var shop = data.shop().name("Đồng hồ " + w + " Official").create();
        long product = data.product(shop).name("Đồng hồ " + w + " dây da").sold(10).image("p/1.webp").sku(990000, 3).create();

        MvcResult r = call(URL, "q", "dong ho " + w);
        assertThat(status(r)).isEqualTo(200);
        assertThat(((Number) read(r, "$.data.categories[0].id")).longValue()).isEqualTo(cat);
        assertThat(((Number) read(r, "$.data.shops[0].id")).longValue()).isEqualTo(shop.shopId());
        assertThat(((Number) read(r, "$.data.products[0].id")).longValue()).isEqualTo(product);
        assertThat((String) read(r, "$.data.products[0].image_url")).endsWith("/product-media/p/1.webp");
        assertThat(r.getResponse().getHeader("Cache-Control")).contains("max-age=60");
    }

    @Test
    void toleratesTyposAndCapsResultCounts() throws Exception {
        String w = TestDataBuilder.uniqueWord();
        var shop = data.shop().create();
        for (int i = 0; i < 7; i++) {
            data.product(shop).name(w + " tai nghe bluetooth " + i).sku(1000, 1).create();
        }
        MvcResult r = call(URL, "q", w + " tai nge");
        List<Object> products = read(r, "$.data.products");
        assertThat(products).hasSize(5);
        assertThat((List<Object>) read(r, "$.data.categories")).hasSizeLessThanOrEqualTo(2);
        assertThat((List<Object>) read(r, "$.data.shops")).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void hiddenProductsAndUnverifiedShopsAreNotSuggested() throws Exception {
        String w = TestDataBuilder.uniqueWord();
        var pending = data.shop().name(w + " shop chờ duyệt").kyc("PENDING").create();
        data.product(pending).name(w + " sản phẩm").sku(1000, 1).create();
        var ok = data.shop().create();
        data.product(ok).name(w + " sản phẩm ẩn").status("HIDDEN").sku(1000, 1).create();

        MvcResult r = call(URL, "q", w);
        List<Number> shopIds = read(r, "$.data.shops[*].id");
        assertThat(shopIds.stream().map(Number::longValue)).doesNotContain(pending.shopId());
        List<String> names = read(r, "$.data.products[*].name");
        assertThat(names).noneMatch(n -> n.startsWith(w));
    }

    @Test
    void shortQueryReturnsEmptyLists() throws Exception {
        MvcResult r = call(URL, "q", "a");
        assertThat((List<Object>) read(r, "$.data.products")).isEmpty();
        assertThat((List<Object>) read(r, "$.data.categories")).isEmpty();
        assertThat(status(call(URL))).isEqualTo(200);
    }
}
