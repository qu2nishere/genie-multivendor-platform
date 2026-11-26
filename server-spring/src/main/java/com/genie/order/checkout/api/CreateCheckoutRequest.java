package com.genie.order.checkout.api;

import com.genie.common.domain.PaymentMethod;
import com.genie.order.checkout.application.CheckoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** {@code POST /me/checkout-sessions} (API_SPEC 6.10). {@code vouchers}, {@code use_coins} C2 nhận nhưng bỏ qua (D2). */
public record CreateCheckoutRequest(@NotEmpty List<@NotNull @Positive Long> cartItemIds, @NotNull @Positive Long addressId,
                                    @Valid PreviewRequest.Vouchers vouchers, Boolean useCoins,
                                    List<@Valid Note> notes, @NotNull PaymentMethod paymentMethod,
                                    @Size(max = 500) String returnUrl) {

    public record Note(@NotNull Long shopId, @Size(max = CheckoutService.NOTE_MAX) String note) {
    }

    CheckoutService.PlaceCommand command() {
        Map<Long, String> byShop = new LinkedHashMap<>();
        if (notes != null) {
            notes.forEach(n -> byShop.put(n.shopId(), n.note()));
        }
        return new CheckoutService.PlaceCommand(cartItemIds, addressId, PreviewRequest.selection(vouchers, useCoins),
                byShop, paymentMethod, returnUrl);
    }
}
