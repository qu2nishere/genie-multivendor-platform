package com.genie.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** {@code GET /public/products/{id}/similar} (UC-CAT-07; API_SPEC 5.9; plan 2026-11-17 C05.04). */
class SimilarProductIT extends CatalogITSupport {

    private List<Long> similar(long id, int limit) throws Exception {
        MvcResult r = call("/api/v1/public/products/" + id + "/similar", "limit", String.valueOf(limit));
        assertThat(status(r)).isEqualTo(200);
        return ids(r);
    }

    @Test
    void sameLeafCategoryWithinPriceBandRankedByNameSalesAndRating() throws Exception {
        long root = data.category("Âm thanh", null);
        long leaf = data.category("Tai nghe", root);
        var shop = data.shop().create();
        long base = data.product(shop).name("Tai nghe Bluetooth chống ồn ANC").category(leaf).sku(400_000, 5).create();
        long closeName = data.product(shop).name("Tai nghe Bluetooth chống ồn Pro").category(leaf).sold(10).rating(4.0).sku(450_000, 5).create();
        long bestSeller = data.product(shop).name("Loa mini").category(leaf).sold(5000).rating(4.9).sku(300_000, 5).create();
        data.product(shop).name("Tai nghe Bluetooth chống ồn đắt").category(leaf).sku(900_000, 5).create();     // > +50%
        data.product(shop).name("Tai nghe Bluetooth chống ồn hết").category(leaf).sku(400_000, 0).create();    // hết hàng
        data.product(shop).name("Tai nghe Bluetooth chống ồn ẩn").category(leaf).status("HIDDEN").sku(400_000, 5).create();
        var resting = data.shop().vacation(OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).minusDays(1), null).create();
        data.product(resting).name("Tai nghe Bluetooth chống ồn nghỉ").category(leaf).sku(400_000, 5).create();

        List<Long> found = similar(base, 6);
        assertThat(found).containsExactlyInAnyOrder(closeName, bestSeller).doesNotContain(base);
    }

    @Test
    void fallsBackToSiblingCategoriesWithoutPriceBand() throws Exception {
        long parent = data.category("Thời trang", null);
        long a = data.category("Áo", parent);
        long b = data.category("Quần", parent);
        var shop = data.shop().create();
        long base = data.product(shop).name("Áo thun").category(a).sku(100_000, 5).create();
        long sibling = data.product(shop).name("Quần jean").category(b).sku(900_000, 5).create();

        assertThat(similar(base, 6)).containsExactly(sibling);
    }

    @Test
    void worksForHiddenProductAndIsEmptyForUnknownOne() throws Exception {
        long leaf = data.category("Bàn phím", null);
        var shop = data.shop().create();
        long gone = data.product(shop).name("Bàn phím cơ").category(leaf).status("HIDDEN").sku(500_000, 5).create();
        long other = data.product(shop).name("Bàn phím cơ RGB").category(leaf).sku(500_000, 5).create();

        assertThat(similar(gone, 6)).containsExactly(other);
        assertThat(similar(999_999_999L, 6)).isEmpty();
        assertThat(status(call("/api/v1/public/products/1/similar", "limit", "13"))).isEqualTo(422);
    }
}
