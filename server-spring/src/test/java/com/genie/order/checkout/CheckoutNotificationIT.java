// Đặt đơn COD → thông báo người mua + từng shop; đơn online chưa báo (plan 2026-11-26 thông báo D11e, N03.09)
package com.genie.order.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.support.OrderTestData.Sku;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CheckoutNotificationIT extends CheckoutTestSupport {

    private List<Map<String, Object>> notes(long user) {
        return jdbc.queryForList("SELECT type, title, body, reference_type, reference_id FROM notifications "
                + "WHERE user_id = ? ORDER BY id", user);
    }

    @Test
    void codCheckoutNotifiesBuyerAndEveryShop() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long shopA = data.shop();
        data.pickup(shopA, 79, "26740");
        long shopB = data.shop();
        data.pickup(shopB, 1, "00001");
        Sku a = data.sku(shopA, 200_000, 10);
        Sku b = data.sku(shopB, 150_000, 5);
        long la = addLine(u, a.variantId(), 1);
        long lb = addLine(u, b.variantId(), 1);

        place(u, addr, "COD", la, lb).andExpect(status().isCreated());

        List<Map<String, Object>> buyer = notes(u);
        assertThat(buyer).hasSize(1);
        assertThat(buyer.get(0).get("type")).isEqualTo("ORDER_STATUS_CHANGED");
        assertThat(buyer.get(0).get("title")).isEqualTo("Đặt hàng thành công");
        assertThat(buyer.get(0).get("body").toString()).startsWith("2 đơn TR-");
        assertThat(buyer.get(0).get("reference_type")).isEqualTo("ORDER");
        for (long shop : new long[] {shopA, shopB}) {
            List<Map<String, Object>> rows = notes(shop);
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0).get("type")).isEqualTo("NEW_ORDER_FOR_VENDOR");
            assertThat(rows.get(0).get("title").toString()).startsWith("Đơn hàng mới TR-");
        }
    }

    @Test
    void onlinePaymentIsNotAnnouncedYet() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long shop = data.shop();
        data.pickup(shop, 79, "26740");
        Sku a = data.sku(shop, 120_000, 3);
        long la = addLine(u, a.variantId(), 1);

        place(u, addr, "VNPAY", la).andExpect(status().isCreated());

        assertThat(notes(u)).isEmpty();
        assertThat(notes(shop)).isEmpty();
    }
}
