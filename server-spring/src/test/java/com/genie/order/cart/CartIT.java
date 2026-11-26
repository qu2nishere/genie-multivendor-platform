package com.genie.order.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.support.OrderTestData.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** UC-ORD-01 — thêm / xem / đổi / xóa dòng giỏ (plan C2 O02.05). */
class CartIT extends CartTestSupport {

    private static final String ITEMS = "/api/v1/me/cart/items";

    private ResultActions add(long user, long variant, int qty) throws Exception {
        return mvc.perform(post(ITEMS).header(HttpHeaders.AUTHORIZATION, auth(user))
                .contentType(MediaType.APPLICATION_JSON).content(addBody(variant, qty)));
    }

    @Test
    void addingSameSkuTwiceAccumulatesIntoOneLine() throws Exception {
        long u = customer();
        Sku s = data.sku(data.shop(), 459000, 10);
        add(u, s.variantId(), 1).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.item.quantity").value(1))
                .andExpect(jsonPath("$.data.item.price_at_add").value(459000))
                .andExpect(jsonPath("$.data.cart_item_count").value(1));
        add(u, s.variantId(), 2).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.quantity").value(3))
                .andExpect(jsonPath("$.data.cart_item_count").value(1));
        assertThat(data.cartLines(u)).isEqualTo(1);

        mvc.perform(get("/api/v1/me/cart").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item_count").value(1))
                .andExpect(jsonPath("$.data.shops[0].items[0].variant_id").value(s.variantId()))
                .andExpect(jsonPath("$.data.shops[0].items[0].stock").value(10))
                .andExpect(jsonPath("$.data.shops[0].items[0].is_available").value(true))
                .andExpect(jsonPath("$.data.shops[0].shop.id").value(data.base().shopIdOf(s.vendorUserId())));
    }

    @Test
    void cannotExceedAvailableStock() throws Exception {
        long u = customer();
        Sku s = data.sku(data.shop(), 100000, 5);
        data.setStock(s.variantId(), 5, 2);   // còn khả dụng 3
        add(u, s.variantId(), 4).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-INV-4094"))
                .andExpect(jsonPath("$.details.stock").value(3));
        add(u, s.variantId(), 3).andExpect(status().isCreated());
        add(u, s.variantId(), 1).andExpect(jsonPath("$.code").value("GNE-INV-4094"));
    }

    @Test
    void ownShopAndVacationShopAreBlocked() throws Exception {
        long vendor = data.shop();
        Sku own = data.sku(vendor, 100000, 5);
        add(vendor, own.variantId(), 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-ORD-4094"));

        long u = customer();
        long resting = data.shop();
        Sku s = data.sku(resting, 100000, 5);
        data.vacation(resting, "2026-06-01T00:00:00Z", "2026-06-20T00:00:00Z");
        add(u, s.variantId(), 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-ORD-4093"));
    }

    @Test
    void unpublishedProductsAndRemovedVariantsCannotBeAdded() throws Exception {
        long u = customer();
        long shop = data.shop();
        for (String st : new String[] {"HIDDEN", "PENDING_APPROVAL", "DELETED", "FLAGGED"}) {
            Sku s = data.sku(shop, 1000, 5);
            data.setProductStatus(s.productId(), st);
            add(u, s.variantId(), 1).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GNE-CAT-4042"));
        }
        Sku removed = data.sku(shop, 1000, 5);
        data.deleteVariant(removed.variantId());
        add(u, removed.variantId(), 1).andExpect(jsonPath("$.code").value("GNE-CAT-4042"));
        add(u, 999_999_999L, 1).andExpect(status().isNotFound());
    }

    @Test
    void cartShowsUnavailableReasonAndPriceChange() throws Exception {
        long u = customer();
        long shop = data.shop();
        Sku a = data.sku(shop, 200000, 5);
        Sku b = data.sku(shop, 300000, 5);
        add(u, a.variantId(), 1).andExpect(status().isCreated());
        add(u, b.variantId(), 1).andExpect(status().isCreated());
        data.setPrice(a.variantId(), 180000);          // người bán giảm giá sau khi thêm
        data.setProductStatus(b.productId(), "HIDDEN"); // người bán ẩn sản phẩm
        String byVariant = "$.data.shops[0].items[?(@.variant_id == %d)].";
        mvc.perform(get("/api/v1/me/cart").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(jsonPath(byVariant.formatted(a.variantId()) + "unit_price").value(180000))
                .andExpect(jsonPath(byVariant.formatted(a.variantId()) + "price_at_add").value(200000))
                .andExpect(jsonPath(byVariant.formatted(b.variantId()) + "is_available").value(false))
                .andExpect(jsonPath(byVariant.formatted(b.variantId()) + "unavailable_reason")
                        .value("PRODUCT_UNAVAILABLE"));
    }

    @Test
    void changeQuantityAndVariantMergesLines() throws Exception {
        long u = customer();
        Sku s = data.sku(data.shop(), 100000, 10);
        long other = data.extraVariant(s.productId(), 120000, 10);
        long lineA = Long.parseLong(json(add(u, s.variantId(), 2).andReturn(), "$.data.item.id"));
        long lineB = Long.parseLong(json(add(u, other, 3).andReturn(), "$.data.item.id"));

        mvc.perform(patch(ITEMS + "/" + lineA).header(HttpHeaders.AUTHORIZATION, auth(u))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":11}"))
                .andExpect(jsonPath("$.code").value("GNE-INV-4094"));
        mvc.perform(patch(ITEMS + "/" + lineA).header(HttpHeaders.AUTHORIZATION, auth(u))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":4}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.item.quantity").value(4));

        // đổi dòng A sang phân loại của dòng B → gộp vào B
        mvc.perform(patch(ITEMS + "/" + lineA).header(HttpHeaders.AUTHORIZATION, auth(u))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"variant_id\":" + other + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.item.id").value(lineB))
                .andExpect(jsonPath("$.data.item.quantity").value(7))
                .andExpect(jsonPath("$.data.merged_from_id").value(lineA))
                .andExpect(jsonPath("$.data.cart_item_count").value(1));

        // phân loại của sản phẩm khác → 422
        Sku foreign = data.sku(data.shop(), 1000, 5);
        mvc.perform(patch(ITEMS + "/" + lineB).header(HttpHeaders.AUTHORIZATION, auth(u))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"variant_id\":" + foreign.variantId() + "}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void usersOnlyTouchTheirOwnLines() throws Exception {
        long a = customer();
        long b = customer();
        Sku s = data.sku(data.shop(), 1000, 5);
        long line = Long.parseLong(json(add(a, s.variantId(), 1).andReturn(), "$.data.item.id"));

        mvc.perform(patch(ITEMS + "/" + line).header(HttpHeaders.AUTHORIZATION, auth(b))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(ITEMS).param("ids", String.valueOf(line)).header(HttpHeaders.AUTHORIZATION, auth(b)))
                .andExpect(status().isNoContent());
        assertThat(data.cartLines(a)).isEqualTo(1);

        mvc.perform(delete(ITEMS).param("ids", line + ",999999").header(HttpHeaders.AUTHORIZATION, auth(a)))
                .andExpect(status().isNoContent());
        assertThat(data.cartLines(a)).isZero();
    }

    @Test
    void enforcesQuantityAndLineLimits() throws Exception {
        long u = customer();
        long shop = data.shop();
        Sku s = data.sku(shop, 1000, 5000);
        add(u, s.variantId(), 0).andExpect(status().isUnprocessableEntity());
        add(u, s.variantId(), 1000).andExpect(status().isUnprocessableEntity());
        add(u, s.variantId(), 999).andExpect(status().isCreated());
        add(u, s.variantId(), 1).andExpect(status().isUnprocessableEntity());

        long cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id = ?", Long.class, u);
        long productId = s.productId();
        for (int i = 0; i < 99; i++) {
            long v = data.extraVariant(productId, 1000, 5);
            jdbc.update("INSERT INTO cart_items (cart_id, product_variant_id, quantity, price_snapshot, added_at) "
                    + "VALUES (?, ?, 1, 1000, now())", cartId, v);
        }
        Sku extra = data.sku(shop, 1000, 5);
        add(u, extra.variantId(), 1).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].code").value("CART_FULL"));
    }

    @Test
    void miniCartListsNewestFirstWithTotal() throws Exception {
        long u = customer();
        long shop = data.shop();
        for (int i = 0; i < 3; i++) {
            Sku s = data.sku(shop, 1000 + i, 5);
            add(u, s.variantId(), 1).andExpect(status().isCreated());
            clock.advance(java.time.Duration.ofSeconds(5));
        }
        mvc.perform(get(ITEMS).param("limit", "2").header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].unit_price").value(1002))
                .andExpect(jsonPath("$.summary.total").value(3));
    }

    @Test
    void guestsMustLogIn() throws Exception {
        mvc.perform(get("/api/v1/me/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post(ITEMS).contentType(MediaType.APPLICATION_JSON).content(addBody(1, 1)))
                .andExpect(status().isUnauthorized());
    }
}
