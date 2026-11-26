package com.genie.order.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.genie.order.cart.CartTestSupport;
import com.genie.support.OrderTestData;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Tiện ích đặt hàng cho test UC-ORD-07 / 08 (plan C2 O07.07, O08.04). */
public abstract class CheckoutTestSupport extends CartTestSupport {

    protected long addLine(long user, long variant, int qty) throws Exception {
        return Long.parseLong(json(mvc.perform(post("/api/v1/me/cart/items").header(HttpHeaders.AUTHORIZATION, auth(user))
                .contentType(MediaType.APPLICATION_JSON).content(addBody(variant, qty))).andReturn(), "$.data.item.id"));
    }

    protected ResultActions place(long user, long addressId, String method, String key, long... cartItemIds)
            throws Exception {
        String ids = Arrays.stream(cartItemIds).mapToObj(String::valueOf).collect(Collectors.joining(","));
        var req = post("/api/v1/me/checkout-sessions").header(HttpHeaders.AUTHORIZATION, auth(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cart_item_ids\":[" + ids + "],\"address_id\":" + addressId + ",\"payment_method\":\"" + method
                        + "\",\"return_url\":\"http://localhost:4321/checkout/result\"}");
        if (key != null) {
            req.header("Idempotency-Key", key);
        }
        return mvc.perform(req);
    }

    protected ResultActions place(long user, long addressId, String method, long... cartItemIds) throws Exception {
        return place(user, addressId, method, OrderTestData.key(), cartItemIds);
    }

    protected long buyer() {
        return customer();
    }
}
