package com.genie.order.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.product.application.ProductOrderGuard;
import com.genie.order.order.application.OrderAccountDeletionBlocker;
import com.genie.support.OrderTestData.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Chặn xóa tài khoản / sản phẩm khi còn đơn mở (plan C2 D14, O07.06–O07.07). */
class OrderGuardsIT extends CheckoutTestSupport {

    @Autowired
    private ProductOrderGuard productGuard;

    @Autowired
    private OrderAccountDeletionBlocker accountBlocker;

    @Test
    void openOrdersBlockProductAndAccountDeletionUntilFinished() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long vendor = data.shop();
        Sku s = data.sku(vendor, 100_000, 5);
        Sku untouched = data.sku(vendor, 100_000, 5);
        long l = addLine(u, s.variantId(), 1);
        String code = json(place(u, addr, "VNPAY", l).andReturn(), "$.data.orders[0].code");

        assertThat(productGuard.hasOpenOrders(s.productId())).isTrue();
        assertThat(productGuard.hasOpenOrders(untouched.productId())).isFalse();
        assertThat(accountBlocker.blockingDetails(u)).containsEntry("active_order_codes", java.util.List.of(code));
        assertThat(accountBlocker.blockingDetails(vendor)).isNotEmpty();   // chủ shop cũng còn đơn phải xử lý

        jdbc.update("UPDATE orders SET status = 'AUTO_CANCELLED' WHERE order_code = ?", code);
        assertThat(productGuard.hasOpenOrders(s.productId())).isFalse();
        assertThat(accountBlocker.blockingDetails(u)).isEmpty();
    }
}
