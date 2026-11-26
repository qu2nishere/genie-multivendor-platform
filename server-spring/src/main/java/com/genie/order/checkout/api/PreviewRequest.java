package com.genie.order.checkout.api;

import com.genie.order.checkout.application.CheckoutDiscountPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** {@code POST /me/checkout/preview} (API_SPEC 6.8). {@code vouchers}, {@code use_coins} nhận nhưng C2 bỏ qua (D2). */
public record PreviewRequest(@NotEmpty List<@NotNull @Positive Long> cartItemIds, @Positive Long addressId,
                             @Valid Vouchers vouchers, Boolean useCoins) {

    public record Vouchers(Long platformVoucherId, Long freeshipVoucherId, List<@Valid ShopVoucher> shopVouchers) {
    }

    public record ShopVoucher(@NotNull Long shopId, @NotNull Long voucherId) {
    }

    /** Lựa chọn giảm giá chuyển cho cổng {@link CheckoutDiscountPolicy}. */
    static CheckoutDiscountPolicy.Selection selection(Vouchers v, Boolean useCoins) {
        boolean coins = Boolean.TRUE.equals(useCoins);
        if (v == null) {
            return new CheckoutDiscountPolicy.Selection(null, null, List.of(), coins);
        }
        List<CheckoutDiscountPolicy.ShopVoucher> shops = v.shopVouchers() == null ? List.of()
                : v.shopVouchers().stream()
                        .map(s -> new CheckoutDiscountPolicy.ShopVoucher(s.shopId(), s.voucherId())).toList();
        return new CheckoutDiscountPolicy.Selection(v.platformVoucherId(), v.freeshipVoucherId(), shops, coins);
    }
}
