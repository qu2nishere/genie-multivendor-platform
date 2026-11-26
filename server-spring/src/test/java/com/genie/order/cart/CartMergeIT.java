package com.genie.order.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.support.OrderTestData.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** UC-ORD-01 — gộp giỏ khách (6.7), Mua lại (6.6), tra biến thể công khai (5.18) — plan C2 O02.05, D8. */
class CartMergeIT extends CartTestSupport {

    private static String items(String... pairs) {
        return "{\"items\":[" + String.join(",", pairs) + "]}";
    }

    private static String it(long variant, int qty) {
        return "{\"variant_id\":" + variant + ",\"quantity\":" + qty + "}";
    }

    @Test
    void mergeAccumulatesCapsAtStockAndReportsSkipped() throws Exception {
        long u = customer();
        long shop = data.shop();
        Sku a = data.sku(shop, 1000, 5);
        Sku b = data.sku(shop, 2000, 10);
        Sku hidden = data.sku(shop, 3000, 5);
        data.setProductStatus(hidden.productId(), "HIDDEN");
        long resting = data.shop();
        Sku vac = data.sku(resting, 4000, 5);
        data.vacation(resting, "2026-06-01T00:00:00Z", "2026-06-20T00:00:00Z");
        Sku own = data.sku(u, 1000, 5);   // dòng do chính người mua bán (vendor_id = u)

        mvc.perform(post("/api/v1/me/cart/items").header(HttpHeaders.AUTHORIZATION, auth(u))
                .contentType(MediaType.APPLICATION_JSON).content(addBody(a.variantId(), 3))).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/me/cart/merge").header(HttpHeaders.AUTHORIZATION, auth(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(items(it(a.variantId(), 4), it(b.variantId(), 1), it(b.variantId(), 1),
                                it(hidden.variantId(), 1), it(vac.variantId(), 1), it(own.variantId(), 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merged").value(2))
                .andExpect(jsonPath("$.data.adjusted.length()").value(1))
                .andExpect(jsonPath("$.data.adjusted[0].variant_id").value(a.variantId()))
                .andExpect(jsonPath("$.data.adjusted[0].quantity").value(5))
                .andExpect(jsonPath("$.data.skipped[?(@.variant_id == %d)].reason".formatted(hidden.variantId()))
                        .value("PRODUCT_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.skipped[?(@.variant_id == %d)].reason".formatted(vac.variantId()))
                        .value("SHOP_ON_VACATION"))
                .andExpect(jsonPath("$.data.skipped[?(@.variant_id == %d)].reason".formatted(own.variantId()))
                        .value("OWN_PRODUCT"))
                .andExpect(jsonPath("$.data.cart_item_count").value(2));
        Integer qtyB = jdbc.queryForObject("SELECT quantity FROM cart_items WHERE product_variant_id = ?",
                Integer.class, b.variantId());
        assertThat(qtyB).isEqualTo(2);
    }

    @Test
    void batchAddsWhatIsStillOnSale() throws Exception {
        long u = customer();
        long shop = data.shop();
        Sku a = data.sku(shop, 1000, 5);
        Sku sold = data.sku(shop, 1000, 2);
        data.setStock(sold.variantId(), 2, 2);
        mvc.perform(post("/api/v1/me/cart/items/batch").header(HttpHeaders.AUTHORIZATION, auth(u))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(items(it(a.variantId(), 1), it(sold.variantId(), 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.added").value(1))
                .andExpect(jsonPath("$.data.skipped[0].variant_id").value(sold.variantId()))
                .andExpect(jsonPath("$.data.skipped[0].reason").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.data.cart_item_count").value(1));
        mvc.perform(post("/api/v1/me/cart/merge").header(HttpHeaders.AUTHORIZATION, auth(u))
                .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void publicVariantsServeGuestCartWithoutLogin() throws Exception {
        long shop = data.shop();
        Sku a = data.sku(shop, 459000, 7);
        Sku hidden = data.sku(shop, 1000, 5);
        data.setProductStatus(hidden.productId(), "HIDDEN");
        mvc.perform(get("/api/v1/public/variants").param("ids", a.variantId() + "," + hidden.variantId() + ",999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].variant_id").value(a.variantId()))
                .andExpect(jsonPath("$.data[0].unit_price").value(459000))
                .andExpect(jsonPath("$.data[0].stock").value(7))
                .andExpect(jsonPath("$.data[0].is_available").value(true))
                .andExpect(jsonPath("$.data[0].shop.id").value(data.base().shopIdOf(shop)))
                .andExpect(jsonPath("$.data[1].unavailable_reason").value("PRODUCT_UNAVAILABLE"));
        mvc.perform(get("/api/v1/public/variants").param("ids", "abc")).andExpect(status().isUnprocessableEntity());
    }
}
