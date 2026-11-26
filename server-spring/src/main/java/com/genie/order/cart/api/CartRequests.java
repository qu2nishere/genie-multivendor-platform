package com.genie.order.cart.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** Thân yêu cầu của giỏ hàng (API_SPEC 6.3, 6.4, 6.6, 6.7) — JSON snake_case. */
public final class CartRequests {

    private CartRequests() {
    }

    /** {@code POST /me/cart/items}. */
    public record AddItem(@NotNull @Positive Long variantId, @NotNull Integer quantity) {
    }

    /** {@code PATCH /me/cart/items/{id}} — đúng một trong hai trường. */
    public record UpdateItem(Integer quantity, @Positive Long variantId) {
    }

    public record Line(@NotNull @Positive Long variantId, Integer quantity) {
    }

    /** {@code POST /me/cart/items/batch}, {@code POST /me/cart/merge}. */
    public record Lines(@NotNull @Valid List<@NotNull @Valid Line> items) {
    }
}
