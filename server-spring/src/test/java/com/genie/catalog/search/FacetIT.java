package com.genie.catalog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import com.genie.support.TestDataBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Lọc nâng cao & facet (UC-CAT-05; API_SPEC 5.3; plan 2026-11-17 C02.09). */
class FacetIT extends CatalogITSupport {

    static final String URL = "/api/v1/public/products";

    @Test
    void filtersByPriceRatingMallFreeshipAndProvince() throws Exception {
        String w = TestDataBuilder.uniqueWord();
        var mallHcm = data.shop().mall().freeship(199000).province(202, "TP. Hồ Chí Minh").create();
        var plainHn = data.shop().province(201, "Hà Nội").create();
        long cheapGood = data.product(mallHcm).name("A " + w).rating(4.6).sku(100_000, 5).create();
        long pricey = data.product(mallHcm).name("B " + w).rating(4.9).sku(900_000, 5).create();
        long lowRated = data.product(plainHn).name("C " + w).rating(3.0).sku(150_000, 5).create();

        assertThat(ids(call(URL, "q", w, "price_min", "90000", "price_max", "200000"))).containsExactlyInAnyOrder(cheapGood, lowRated);
        assertThat(ids(call(URL, "q", w, "rating_min", "4"))).containsExactlyInAnyOrder(cheapGood, pricey);
        assertThat(ids(call(URL, "q", w, "is_mall", "true"))).containsExactlyInAnyOrder(cheapGood, pricey);
        assertThat(ids(call(URL, "q", w, "free_shipping", "true"))).containsExactlyInAnyOrder(cheapGood, pricey);
        assertThat(ids(call(URL, "q", w, "province_ids", "201"))).containsExactly(lowRated);
        assertThat(ids(call(URL, "q", w, "province_ids", "201,202"))).hasSize(3);
    }

    @Test
    void categoryIncludesChildCategories() throws Exception {
        long root = data.category("Điện tử", null);
        long child = data.category("Tai nghe", root);
        long grandChild = data.category("Tai nghe không dây", child);
        long other = data.category("Thời trang", null);
        var shop = data.shop().create();
        long a = data.product(shop).category(root).sku(1000, 1).create();
        long b = data.product(shop).category(grandChild).sku(1000, 1).create();
        data.product(shop).category(other).sku(1000, 1).create();

        assertThat(ids(call(URL, "category_id", String.valueOf(root)))).containsExactlyInAnyOrder(a, b);
        assertThat(ids(call(URL, "category_id", String.valueOf(child)))).containsExactly(b);
    }

    @Test
    void brandFacetIgnoresItsOwnFilter() throws Exception {
        long sony = data.brand("Sony", true);
        long apple = data.brand("Apple", true);
        var shop = data.shop().province(202, "TP. Hồ Chí Minh").create();
        String shopId = String.valueOf(shop.shopId());
        long s1 = data.product(shop).brand(sony, "Sony").sku(1000, 1).create();
        long s2 = data.product(shop).brand(sony, "Sony").sku(1000, 1).create();
        data.product(shop).brand(apple, "Apple").sku(1000, 1).create();

        MvcResult r = call(URL, "shop_id", shopId, "brand_ids", String.valueOf(sony), "include_facets", "true");
        assertThat(ids(r)).containsExactlyInAnyOrder(s1, s2);
        List<Map<String, Object>> brands = read(r, "$.summary.facets.brands");
        assertThat(brands).extracting(m -> m.get("name"), m -> ((Number) m.get("count")).intValue())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Sony", 2), org.assertj.core.groups.Tuple.tuple("Apple", 1));
        List<Map<String, Object>> locations = read(r, "$.summary.facets.locations");
        assertThat(locations).hasSize(1);
        assertThat(((Number) locations.get(0).get("count")).intValue()).isEqualTo(2);   // nơi bán vẫn áp lọc thương hiệu
        assertThat((Object) read(r, "$.summary.facets.attribute")).isNull();
        assertThat((Boolean) read(r, "$.summary.facets.has_shop_voucher_supported")).isFalse();
    }

    @Test
    void attributeFilterAndFacetNeedOneCategory() throws Exception {
        long cat = data.category("Điện thoại", null);
        jdbc.update("""
                INSERT INTO category_attributes (category_id, code, name, data_type, is_filterable, display_order, created_at, updated_at)
                VALUES (?, 'warranty', 'Bảo hành', 'ENUM', TRUE, 0, now(), now())""", cat);
        var shop = data.shop().create();
        long m12 = data.product(shop).category(cat).specs("{\"warranty\":\"12 tháng\"}").sku(1000, 1).create();
        long m24 = data.product(shop).category(cat).specs("{\"warranty\":\"24 tháng\"}").sku(1000, 1).create();
        data.product(shop).category(cat).sku(1000, 1).create();

        MvcResult r = call(URL, "category_id", String.valueOf(cat), "attr.warranty", "12 tháng", "include_facets", "true");
        assertThat(ids(r)).containsExactly(m12);
        assertThat((String) read(r, "$.summary.facets.attribute.name")).isEqualTo("Bảo hành");
        List<String> values = read(r, "$.summary.facets.attribute.values[*].value");
        assertThat(values).containsExactlyInAnyOrder("12 tháng", "24 tháng");   // facet thuộc tính không tự co

        MvcResult both = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(URL)
                .param("category_id", String.valueOf(cat)).param("attr.warranty", "12 tháng", "24 tháng")).andReturn();
        assertThat(ids(both)).containsExactlyInAnyOrder(m12, m24);

        // Không chọn danh mục → bỏ qua lọc thuộc tính
        assertThat(ids(call(URL, "shop_id", String.valueOf(shop.shopId()), "attr.warranty", "12 tháng"))).hasSize(3);
    }

    @Test
    void discountMinUsesBasePriceAsOriginal() throws Exception {
        var shop = data.shop().create();
        long half = data.product(shop).basePrice(200_000).sku(100_000, 1).create();
        data.product(shop).basePrice(110_000).sku(100_000, 1).create();
        data.product(shop).sku(100_000, 1).create();

        assertThat(ids(call(URL, "shop_id", String.valueOf(shop.shopId()), "discount_min", "30"))).containsExactly(half);
    }

    @Test
    void shopVoucherFilterIsIgnoredUntilPromotionModuleExists() throws Exception {
        var shop = data.shop().create();
        data.product(shop).sku(1000, 1).create();
        assertThat(ids(call(URL, "shop_id", String.valueOf(shop.shopId()), "has_shop_voucher", "true"))).hasSize(1);
    }
}
