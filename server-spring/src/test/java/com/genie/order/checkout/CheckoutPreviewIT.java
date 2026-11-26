package com.genie.order.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.order.cart.CartTestSupport;
import com.genie.support.OrderTestData.Sku;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** UC-ORD-04 / ORD-06 — xem trước & tách giỏ theo shop (plan C2 O06.05). */
class CheckoutPreviewIT extends CartTestSupport {

    private long addLine(long user, long variant, int qty) throws Exception {
        return Long.parseLong(json(mvc.perform(post("/api/v1/me/cart/items").header(HttpHeaders.AUTHORIZATION, auth(user))
                .contentType(MediaType.APPLICATION_JSON).content(addBody(variant, qty))).andReturn(), "$.data.item.id"));
    }

    private ResultActions preview(long user, String body) throws Exception {
        return mvc.perform(post("/api/v1/me/checkout/preview").header(HttpHeaders.AUTHORIZATION, auth(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void splitsByShopWithShippingAndTotals() throws Exception {
        long u = customer();
        data.address(u, 79, "26740", true);
        long hcm = data.shop();
        data.pickup(hcm, 79, "26740");          // cùng tỉnh → 15k
        long hn = data.shop();
        data.pickup(hn, 1, "00001");             // khác tỉnh → 30k
        Sku a = data.sku(hcm, 200_000, 10);
        Sku b = data.sku(hn, 150_000, 10);
        long la = addLine(u, a.variantId(), 2);
        long lb = addLine(u, b.variantId(), 1);

        preview(u, "{\"cart_item_ids\":[" + la + "," + lb + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address.province.id").value(79))
                .andExpect(jsonPath("$.data.shops.length()").value(2))
                .andExpect(jsonPath("$.data.shops[?(@.shop.id == %d)].subtotal".formatted(data.base().shopIdOf(hcm)))
                        .value(400_000))
                .andExpect(jsonPath("$.data.shops[?(@.shop.id == %d)].shipping_fee".formatted(data.base().shopIdOf(hcm)))
                        .value(15_000))
                .andExpect(jsonPath("$.data.shops[?(@.shop.id == %d)].shipping_fee".formatted(data.base().shopIdOf(hn)))
                        .value(30_000))
                .andExpect(jsonPath("$.data.shops[0].is_estimated_fee").value(false))
                .andExpect(jsonPath("$.data.vouchers_applied").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.coins").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.summary.subtotal").value(550_000))
                .andExpect(jsonPath("$.data.summary.shipping_total").value(45_000))
                .andExpect(jsonPath("$.data.summary.platform_discount").value(0))
                .andExpect(jsonPath("$.data.summary.grand_total").value(595_000))
                .andExpect(jsonPath("$.data.summary.order_count").value(2))
                .andExpect(jsonPath("$.data.summary.item_count").value(2));
    }

    @Test
    void freeshipThresholdAndShopWithoutPickup() throws Exception {
        long u = customer();
        data.address(u, 79, "26740", true);
        long free = data.shop();
        data.pickup(free, 79, "26740");
        data.freeship(free, 300_000);
        long noPickup = data.shop();             // chưa cấu hình kho → 30k tạm tính
        long la = addLine(u, data.sku(free, 300_000, 5).variantId(), 1);
        long lb = addLine(u, data.sku(noPickup, 50_000, 5).variantId(), 1);
        String byShop = "$.data.shops[?(@.shop.id == %d)].";
        preview(u, "{\"cart_item_ids\":[" + la + "," + lb + "]}")
                .andExpect(jsonPath(byShop.formatted(data.base().shopIdOf(free)) + "shipping_fee").value(0))
                .andExpect(jsonPath(byShop.formatted(data.base().shopIdOf(free)) + "is_free_shipping_by_shop").value(true))
                .andExpect(jsonPath(byShop.formatted(data.base().shopIdOf(free)) + "base_shipping_fee").value(15_000))
                .andExpect(jsonPath(byShop.formatted(data.base().shopIdOf(free)) + "free_ship_threshold").value(300_000))
                .andExpect(jsonPath(byShop.formatted(data.base().shopIdOf(noPickup)) + "shipping_fee").value(30_000))
                .andExpect(jsonPath(byShop.formatted(data.base().shopIdOf(noPickup)) + "is_estimated_fee").value(true))
                .andExpect(jsonPath("$.data.summary.shipping_total").value(30_000));
    }

    @Test
    void withoutAddressShippingIsNull() throws Exception {
        long u = customer();
        long l = addLine(u, data.sku(data.shop(), 100_000, 5).variantId(), 1);
        preview(u, "{\"cart_item_ids\":[" + l + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.shops[0].shipping_fee").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.summary.grand_total").value(100_000));
    }

    @Test
    void explicitAddressMustBelongToCaller() throws Exception {
        long u = customer();
        long other = customer();
        long foreignAddress = data.address(other, 1, "00001", true);
        long own = data.address(u, 1, "00001", false);
        long l = addLine(u, data.sku(data.shop(), 100_000, 5).variantId(), 1);
        preview(u, "{\"cart_item_ids\":[" + l + "],\"address_id\":" + foreignAddress + "}")
                .andExpect(status().isUnprocessableEntity());
        preview(u, "{\"cart_item_ids\":[" + l + "],\"address_id\":" + own + "}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.address.id").value(own));
    }

    @Test
    void ignoresOtherUsersLinesAndFlagsUnavailableOnes() throws Exception {
        long u = customer();
        long other = customer();
        long shop = data.shop();
        Sku ok = data.sku(shop, 100_000, 5);
        Sku sold = data.sku(shop, 80_000, 5);
        long mine = addLine(u, ok.variantId(), 1);
        long gone = addLine(u, sold.variantId(), 3);
        long theirs = addLine(other, ok.variantId(), 1);
        data.setStock(sold.variantId(), 5, 4);   // còn 1 < 3 trong giỏ
        preview(u, "{\"cart_item_ids\":[" + mine + "," + gone + "," + theirs + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shops[0].items.length()").value(2))
                .andExpect(jsonPath("$.data.shops[0].items[?(@.cart_item_id == %d)].is_available".formatted(gone))
                        .value(false))
                .andExpect(jsonPath("$.data.shops[0].items[?(@.cart_item_id == %d)].unavailable_reason".formatted(gone))
                        .value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.data.summary.subtotal").value(100_000))
                .andExpect(jsonPath("$.data.summary.item_count").value(1));
        preview(u, "{\"cart_item_ids\":[]}").andExpect(status().isUnprocessableEntity());
    }
}
