package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Trang gian hàng công khai (UC-CAT-16; API_SPEC 5.11, 5.12; plan 2026-11-17 C06.03). */
class PublicShopIT extends CatalogITSupport {

    private String url(long id) {
        return "/api/v1/public/shops/" + id;
    }

    @Test
    void showsShopInfoStatsAndRootCategories() throws Exception {
        long electronics = data.category("Điện tử", null);
        long audio = data.category("Âm thanh", electronics);
        long fashion = data.category("Thời trang", null);
        data.category("Không có hàng", null);
        var shop = data.shop().name("TechZone Official").mall().rating(4.9).followers(128000).create();
        data.product(shop).category(audio).sku(1000, 1).create();
        data.product(shop).category(fashion).sku(1000, 1).create();
        data.product(shop).category(fashion).status("HIDDEN").sku(1000, 1).create();

        MvcResult r = call(url(shop.shopId()));
        assertThat(status(r)).isEqualTo(200);
        assertThat((String) read(r, "$.data.name")).isEqualTo("TechZone Official");
        assertThat((Boolean) read(r, "$.data.is_mall")).isTrue();
        assertThat((Double) read(r, "$.data.rating_avg")).isEqualTo(4.9);
        assertThat((Integer) read(r, "$.data.follower_count")).isEqualTo(128000);
        assertThat((Integer) read(r, "$.data.product_count")).isEqualTo(2);
        assertThat((Double) read(r, "$.data.chat_response_rate")).isEqualTo(95.5);
        assertThat((Integer) read(r, "$.data.avg_response_minutes")).isEqualTo(6);
        assertThat((String) read(r, "$.data.joined_at")).isEqualTo("2021-03-02");
        assertThat((Boolean) read(r, "$.data.is_online")).isFalse();
        assertThat((Object) read(r, "$.data.vacation")).isNull();
        assertThat((List<String>) read(r, "$.data.categories[*].name")).containsExactlyInAnyOrder("Điện tử", "Thời trang");
    }

    @Test
    void productGridAndSearchInShopUsePublicProducts() throws Exception {
        var shop = data.shop().create();
        var other = data.shop().create();
        long a = data.product(shop).name("Bàn phím cơ").sku(1000, 1).create();
        data.product(shop).name("Chuột không dây").sku(1000, 1).create();
        data.product(other).name("Bàn phím cơ khác shop").sku(1000, 1).create();

        assertThat(ids(call("/api/v1/public/products", "shop_id", String.valueOf(shop.shopId())))).hasSize(2);
        assertThat(ids(call("/api/v1/public/products", "shop_id", String.valueOf(shop.shopId()), "q", "ban phim")))
                .containsExactly(a);
    }

    @Test
    void emptyShopHasZeroProducts() throws Exception {
        var shop = data.shop().create();
        MvcResult r = call(url(shop.shopId()));
        assertThat((Integer) read(r, "$.data.product_count")).isZero();
        assertThat((List<Object>) read(r, "$.data.categories")).isEmpty();
    }

    @Test
    void vacationBanner() throws Exception {
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        var shop = data.shop().vacation(now.minusHours(1), now.plusDays(5)).create();
        assertThat((String) read(call(url(shop.shopId())), "$.data.vacation.until")).isNotBlank();
    }

    @Test
    void unverifiedLockedOrMissingShopGives404() throws Exception {
        var pending = data.shop().kyc("PENDING").create();
        var suspended = data.shop().userStatus("SUSPENDED").create();
        for (long id : new long[] {pending.shopId(), suspended.shopId(), 999_999_999L}) {
            MvcResult r = call(url(id));
            assertThat(status(r)).isEqualTo(404);
            assertThat((String) read(r, "$.code")).isEqualTo("GNE-VND-4041");
        }
    }

    @Test
    void featuredShopsListOnlyVerifiedActiveShops() throws Exception {
        var mall = data.shop().mall().rating(5.0).followers(999_999).create();
        var pending = data.shop().kyc("PENDING").mall().rating(5.0).followers(999_999).create();
        MvcResult r = call("/api/v1/public/shops", "featured", "true", "limit", "24");
        List<Number> found = read(r, "$.data[*].id");
        assertThat(found.stream().map(Number::longValue)).contains(mall.shopId()).doesNotContain(pending.shopId());
        assertThat(status(call("/api/v1/public/shops", "limit", "25"))).isEqualTo(422);
    }
}
