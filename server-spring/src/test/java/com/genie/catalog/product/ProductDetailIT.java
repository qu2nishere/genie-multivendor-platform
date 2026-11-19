package com.genie.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import com.genie.catalog.product.application.ProductDetailService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** {@code GET /public/products/{id}} + {@code /variants} (UC-CAT-06; API_SPEC 5.6, 5.10; plan 2026-11-17 C04.06). */
class ProductDetailIT extends CatalogITSupport {

    @Autowired
    ProductDetailService details;

    private String url(long id) {
        return "/api/v1/public/products/" + id;
    }

    @Test
    void showsSkuMatrixStockSpecsAndShop() throws Exception {
        long root = data.category("Điện tử", null);
        long leaf = data.category("Thiết bị âm thanh", root);
        jdbc.update("""
                INSERT INTO category_attributes (category_id, code, name, data_type, is_filterable, display_order, created_at, updated_at)
                VALUES (?, 'warranty', 'Bảo hành', 'ENUM', TRUE, 0, now(), now())""", leaf);
        long sony = data.brand("Sony", true);
        var shop = data.shop().name("TechZone Official " + com.genie.support.TestDataBuilder.uniqueWord()).mall().province(202, "TP. Hồ Chí Minh").rating(4.9).followers(128000).create();
        long id = data.product(shop).name("Tai nghe Bluetooth ANC Pro 5").category(leaf).brand(sony, "Sony")
                .basePrice(799000).sold(2100).rating(4.8)
                .specs("{\"warranty\":\"12 tháng chính hãng\",\"ket_noi\":\"Bluetooth 5.3\"}")
                .sku(389000, 40, 2, "{\"Màu sắc\":\"Đen\",\"Phiên bản\":\"Tiêu chuẩn\"}")
                .sku(539000, 3, 3, "{\"Màu sắc\":\"Đen\",\"Phiên bản\":\"Bản Pro\"}")
                .sku(389000, 5, 0, "{\"Màu sắc\":\"Trắng\",\"Phiên bản\":\"Tiêu chuẩn\"}")
                .deletedSku(1000, 99)
                .image("p/1.webp").image("p/2.webp").create();
        jdbc.update("UPDATE products SET variant_image_attribute = 'Màu sắc' WHERE id = ?", id);

        MvcResult r = call(url(id));
        assertThat(status(r)).isEqualTo(200);
        assertThat((String) read(r, "$.data.slug")).isEqualTo("tai-nghe-bluetooth-anc-pro-5");
        assertThat((List<String>) read(r, "$.data.category_path[*].name")).containsExactly("Điện tử", "Thiết bị âm thanh");
        assertThat((String) read(r, "$.data.brand.name")).isEqualTo("Sony");
        assertThat((Boolean) read(r, "$.data.brand.is_verified")).isTrue();
        assertThat((List<String>) read(r, "$.data.images")).hasSize(2);
        assertThat((Integer) read(r, "$.data.price")).isEqualTo(389000);
        assertThat((Integer) read(r, "$.data.original_price")).isEqualTo(799000);
        assertThat((Integer) read(r, "$.data.discount_percent")).isEqualTo(51);
        assertThat((List<String>) read(r, "$.data.option_groups[*].name")).containsExactlyInAnyOrder("Màu sắc", "Phiên bản");
        List<Map<String, Object>> variants = read(r, "$.data.variants");
        assertThat(variants).hasSize(3);                                            // SKU xóa mềm bị loại
        assertThat(variants).extracting(v -> v.get("stock")).containsExactly(38, 0, 5); // tồn = stock − reserved
        assertThat((List<String>) read(r, "$.data.specs[*].name")).containsExactly("Bảo hành", "ket_noi");
        assertThat((String) read(r, "$.data.ships_from")).isEqualTo("TP. Hồ Chí Minh");
        assertThat((String) read(r, "$.data.condition")).isEqualTo("NEW");
        assertThat((String) read(r, "$.data.shop.name")).isEqualTo(shop.name());
        assertThat((Boolean) read(r, "$.data.shop.is_mall")).isTrue();
        assertThat((Integer) read(r, "$.data.shop.follower_count")).isEqualTo(128000);
        assertThat((Integer) read(r, "$.data.shop.joined_year")).isEqualTo(2021);
        assertThat((Object) read(r, "$.data.shop.vacation")).isNull();
        assertThat((Object) read(r, "$.data.flash_sale")).isNull();
        assertThat(r.getResponse().getHeader("Cache-Control")).isEqualTo("no-cache");
    }

    @Test
    void stockIsLiveWhileStaticPartIsCached() throws Exception {
        var shop = data.shop().create();
        long id = data.product(shop).name("Tên ban đầu").sku(100000, 10).create();
        assertThat((Integer) read(call(url(id)), "$.data.variants[0].stock")).isEqualTo(10);

        jdbc.update("UPDATE product_variants SET reserved_count = 4 WHERE product_id = ?", id);
        jdbc.update("UPDATE products SET name = 'Tên mới' WHERE id = ?", id);
        MvcResult cached = call(url(id));
        assertThat((Integer) read(cached, "$.data.variants[0].stock")).isEqualTo(6);     // tồn đọc thẳng DB
        assertThat((String) read(cached, "$.data.name")).isEqualTo("Tên ban đầu");      // phần tĩnh còn trong cache

        details.evict(id);
        assertThat((String) read(call(url(id)), "$.data.name")).isEqualTo("Tên mới");
    }

    @Test
    void vacationIsComputedAtRequestTime() throws Exception {
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        var shop = data.shop().vacation(now.minusDays(1), now.plusDays(3)).create();
        long id = data.product(shop).sku(1000, 1).create();
        assertThat((String) read(call(url(id)), "$.data.shop.vacation.until")).isNotBlank();
        clock.set(now.plusDays(4).toInstant());
        assertThat((Object) read(call(url(id)), "$.data.shop.vacation")).isNull();
    }

    @Test
    void unavailableProductsGive404() throws Exception {
        var shop = data.shop().create();
        long hidden = data.product(shop).status("HIDDEN").sku(1000, 1).create();
        var pending = data.shop().kyc("PENDING").create();
        long ofPending = data.product(pending).sku(1000, 1).create();

        for (long id : new long[] {hidden, ofPending, 999_999_999L}) {
            MvcResult r = call(url(id));
            assertThat(status(r)).isEqualTo(404);
            assertThat((String) read(r, "$.code")).isEqualTo("GNE-CAT-4042");
        }
        assertThat(status(call("/api/v1/public/products/abc"))).isEqualTo(422);
    }

    @Test
    void variantChoicesForCartModal() throws Exception {
        var shop = data.shop().create();
        long id = data.product(shop).sku(100000, 2, 0, "{\"Màu\":\"Đỏ\"}").sku(120000, 0, 0, "{\"Màu\":\"Xanh\"}")
                .image("p/main.webp").create();
        MvcResult r = call(url(id) + "/variants");
        assertThat((List<String>) read(r, "$.data.option_groups[0].values")).containsExactly("Đỏ", "Xanh");
        assertThat((List<Integer>) read(r, "$.data.variants[*].stock")).containsExactly(2, 0);
        assertThat((String) read(r, "$.data.image_url")).endsWith("/product-media/p/main.webp");
    }
}
