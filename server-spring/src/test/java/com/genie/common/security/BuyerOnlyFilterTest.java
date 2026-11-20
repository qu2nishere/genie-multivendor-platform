package com.genie.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Đường dẫn mua hàng bị chặn với tài khoản nội bộ (plan B1 D16 hướng B; API_SPEC 6.1–6.13). */
class BuyerOnlyFilterTest {

    @Test
    void purchasePaths() {
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/cart")).isTrue();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/cart/items")).isTrue();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/cart/merge")).isTrue();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/checkout/preview")).isTrue();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/checkout-sessions")).isTrue();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/checkout-sessions/9/payments")).isTrue();
    }

    @Test
    void otherPathsPass() {
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me")).isFalse();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/orders")).isFalse();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/me/cartography")).isFalse();
        assertThat(BuyerOnlyFilter.isPurchasePath("/api/v1/admin/staff")).isFalse();
        assertThat(BuyerOnlyFilter.isPurchasePath(null)).isFalse();
    }
}
