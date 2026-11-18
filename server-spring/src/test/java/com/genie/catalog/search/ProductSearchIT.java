package com.genie.catalog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import com.genie.support.TestDataBuilder;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** {@code GET /public/products} — tìm theo từ khóa (UC-CAT-02; plan 2026-11-17 C02.08). Mỗi test lọc theo shop của nó. */
class ProductSearchIT extends CatalogITSupport {

    static final String URL = "/api/v1/public/products";

    @Test
    void findsWithOrWithoutVietnameseMarks() throws Exception {
        var shop = data.shop().create();
        long id = data.product(shop).name("Áo thun nam cotton").sku(150000, 5).create();
        String shopId = String.valueOf(shop.shopId());

        assertThat(ids(call(URL, "q", "ao thun", "shop_id", shopId))).containsExactly(id);
        assertThat(ids(call(URL, "q", "ÁO THUN", "shop_id", shopId))).containsExactly(id);
        assertThat(ids(call(URL, "q", "áo   thun!!", "shop_id", shopId))).containsExactly(id);
    }

    @Test
    void lastWordMatchesAsPrefix() throws Exception {
        var shop = data.shop().create();
        long id = data.product(shop).name("Tai nghe Bluetooth chống ồn").sku(389000, 5).create();
        assertThat(ids(call(URL, "q", "tai ngh", "shop_id", String.valueOf(shop.shopId())))).containsExactly(id);
    }

    @Test
    void typoFallsBackToTrigram() throws Exception {
        var shop = data.shop().create();
        long id = data.product(shop).name("Tai nghe Bluetooth chống ồn").sku(389000, 5).create();
        data.product(shop).name("Bàn phím cơ RGB").sku(900000, 5).create();

        MvcResult r = call(URL, "q", "tai nge bluetoth", "shop_id", String.valueOf(shop.shopId()));
        assertThat(status(r)).isEqualTo(200);
        assertThat(ids(r)).containsExactly(id);
        assertThat((Integer) read(r, "$.pagination.total_items")).isEqualTo(1);
    }

    @Test
    void nameMatchRanksAboveDescriptionMatch() throws Exception {
        var shop = data.shop().create();
        String w = TestDataBuilder.uniqueWord();
        long inDescription = data.product(shop).name("Phụ kiện điện thoại").description("tương thích " + w).sold(999).sku(1000, 1).create();
        long inName = data.product(shop).name("Ốp lưng " + w).sold(1).sku(1000, 1).create();
        for (int i = 0; i < 4; i++) {   // đủ 5 kết quả full-text → không vào tầng trigram
            data.product(shop).name("Sản phẩm phụ " + i).description(w).sku(1000, 1).create();
        }

        List<Long> found = ids(call(URL, "q", w, "shop_id", String.valueOf(shop.shopId())));
        assertThat(found).hasSize(6);
        assertThat(found.get(0)).isEqualTo(inName);
        assertThat(found).contains(inDescription);
    }

    @Test
    void emptyKeywordReturnsNewestFirst() throws Exception {
        var shop = data.shop().create();
        OffsetDateTime t = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        long older = data.product(shop).createdAt(t).sku(1000, 1).create();
        long newer = data.product(shop).createdAt(t.plusDays(1)).sku(1000, 1).create();

        assertThat(ids(call(URL, "q", "!!!", "shop_id", String.valueOf(shop.shopId())))).containsExactly(newer, older);
    }

    @Test
    void onlyPublishedProductsOfActiveVerifiedShops() throws Exception {
        String w = TestDataBuilder.uniqueWord();
        var shop = data.shop().create();
        long visible = data.product(shop).name("Hàng " + w).sku(1000, 1).create();
        long draft = data.product(shop).name("Nháp " + w).status("DRAFT").sku(1000, 1).create();
        long hidden = data.product(shop).name("Ẩn " + w).status("HIDDEN").sku(1000, 1).create();
        var pending = data.shop().kyc("PENDING").create();
        long unverified = data.product(pending).name("Chưa duyệt " + w).sku(1000, 1).create();
        var suspended = data.shop().userStatus("SUSPENDED").create();
        long locked = data.product(suspended).name("Bị khóa " + w).sku(1000, 1).create();

        List<Long> found = ids(call(URL, "q", w, "size", "60"));
        assertThat(found).contains(visible).doesNotContain(draft, hidden, unverified, locked);
        assertThat(found.get(0)).isEqualTo(visible);
    }

    @Test
    void sortsAndPagesStably() throws Exception {
        var shop = data.shop().create();
        String shopId = String.valueOf(shop.shopId());
        List<Long> created = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            created.add(data.product(shop).sold(i * 10).sku(i * 100_000L, 5).create());
        }

        assertThat(ids(call(URL, "shop_id", shopId, "sort", "PRICE_ASC", "size", "7"))).containsExactlyElementsOf(created);
        assertThat(ids(call(URL, "shop_id", shopId, "sort", "PRICE_DESC", "size", "7")))
                .containsExactlyElementsOf(created.reversed());
        assertThat(ids(call(URL, "shop_id", shopId, "sort", "BEST_SELLING", "size", "7")))
                .containsExactlyElementsOf(created.reversed());

        MvcResult p1 = call(URL, "shop_id", shopId, "sort", "PRICE_ASC", "size", "3", "page", "1");
        MvcResult p3 = call(URL, "shop_id", shopId, "sort", "PRICE_ASC", "size", "3", "page", "3");
        assertThat(ids(p1)).containsExactlyElementsOf(created.subList(0, 3));
        assertThat(ids(p3)).containsExactly(created.get(6));
        assertThat((Integer) read(p1, "$.pagination.total_items")).isEqualTo(7);
        assertThat((Integer) read(p1, "$.pagination.total_pages")).isEqualTo(3);
        assertThat(ids(call(URL, "shop_id", shopId, "page", "9", "size", "3"))).isEmpty();
    }

    @Test
    void productCardFollowsApiSpec() throws Exception {
        var shop = data.shop().name("TechZone Official").freeship(199000).create();
        long id = data.product(shop).name("Tai nghe ANC Pro 5").basePrice(799000).sold(2100).rating(4.8)
                .sku(389000, 10).sku(539000, 3).deletedSku(1000, 9).image("product/1/main.webp").create();

        MvcResult r = call(URL, "shop_id", String.valueOf(shop.shopId()));
        assertThat((String) read(r, "$.code")).isEqualTo("SUCCESS");
        assertThat(((Number) read(r, "$.data[0].id")).longValue()).isEqualTo(id);
        assertThat((String) read(r, "$.data[0].slug")).isEqualTo("tai-nghe-anc-pro-5");
        assertThat((Integer) read(r, "$.data[0].price")).isEqualTo(389000);          // SKU xóa mềm 1.000đ bị bỏ
        assertThat((Integer) read(r, "$.data[0].original_price")).isEqualTo(799000);
        assertThat((Integer) read(r, "$.data[0].discount_percent")).isEqualTo(51);
        assertThat((Double) read(r, "$.data[0].rating_avg")).isEqualTo(4.8);
        assertThat((Integer) read(r, "$.data[0].sold_count")).isEqualTo(2100);
        assertThat((String) read(r, "$.data[0].shop.name")).isEqualTo("TechZone Official");
        assertThat((Boolean) read(r, "$.data[0].shop.is_mall")).isFalse();
        assertThat((Boolean) read(r, "$.data[0].is_free_shipping")).isTrue();
        assertThat((Boolean) read(r, "$.data[0].is_shop_on_vacation")).isFalse();
        assertThat((String) read(r, "$.data[0].image_url")).endsWith("/product-media/product/1/main.webp");
        assertThat((Object) read(r, "$.data[0].flash_sale")).isNull();
        assertThat((Object) read(r, "$.summary")).isNull();
        assertThat(r.getResponse().getHeader("ETag")).isNotBlank();
    }

    @Test
    void invalidParametersGive422() throws Exception {
        assertThat(status(call(URL, "size", "61"))).isEqualTo(422);
        assertThat(status(call(URL, "page", "0"))).isEqualTo(422);
        assertThat(status(call(URL, "page", "101"))).isEqualTo(422);
        assertThat(status(call(URL, "sort", "CHEAPEST"))).isEqualTo(422);
        assertThat(status(call(URL, "price_min", "500", "price_max", "100"))).isEqualTo(422);
        assertThat(status(call(URL, "rating_min", "6"))).isEqualTo(422);
        assertThat(status(call(URL, "attr.Bad-Code", "x"))).isEqualTo(422);
        assertThat((String) read(call(URL, "size", "61"), "$.code")).isEqualTo("GNE-VAL-4220");
    }

    @Test
    void rateLimitedAfterSixtyRequestsPerMinute() throws Exception {
        for (int i = 0; i < 60; i++) {
            assertThat(status(call(URL, "size", "1"))).isEqualTo(200);
        }
        MvcResult r = call(URL, "size", "1");
        assertThat(status(r)).isEqualTo(429);
        assertThat(r.getResponse().getHeader("Retry-After")).isNotBlank();
    }
}
