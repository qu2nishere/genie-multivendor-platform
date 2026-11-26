package com.genie.order.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.support.OrderTestData.Sku;
import java.math.BigDecimal;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** UC-ORD-07 — đặt hàng: tách đơn, snapshot, giữ chỗ / chốt tồn, xóa giỏ, idempotency, lỗi (plan C2 O07.07). */
class CheckoutSessionIT extends CheckoutTestSupport {

    @Test
    void codSplitsOrdersPerShopCommitsStockAndClearsCart() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long shopA = data.shop();
        data.pickup(shopA, 79, "26740");
        long shopB = data.shop();
        data.pickup(shopB, 1, "00001");
        Sku a = data.sku(shopA, 200_000, 10);
        Sku b = data.sku(shopB, 150_000, 5);
        Sku keep = data.sku(shopB, 10_000, 5);
        long la = addLine(u, a.variantId(), 2);
        long lb = addLine(u, b.variantId(), 1);
        long lk = addLine(u, keep.variantId(), 1);

        var r = place(u, addr, "COD", la, lb).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.payment_method").value("COD"))
                .andExpect(jsonPath("$.data.code").value(Matchers.startsWith("CS-")))
                .andExpect(jsonPath("$.data.orders.length()").value(2))
                .andExpect(jsonPath("$.data.orders[0].code").value(Matchers.matchesPattern("TR-\\d{8}-\\d{6}")))
                .andExpect(jsonPath("$.data.summary.subtotal").value(550_000))
                .andExpect(jsonPath("$.data.summary.shipping_total").value(45_000))
                .andExpect(jsonPath("$.data.summary.grand_total").value(595_000))
                .andExpect(jsonPath("$.data.payment.method").value("COD"))
                .andReturn();
        long sessionId = Long.parseLong(json(r, "$.data.id"));

        assertThat(data.stock(a.variantId())).isEqualTo(8);
        assertThat(data.reserved(a.variantId())).isZero();
        assertThat(data.stock(b.variantId())).isEqualTo(4);
        assertThat(data.cartLines(u)).isEqualTo(1);   // chỉ còn dòng không đặt
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cart_items WHERE id = ?", Integer.class, lk)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT status FROM orders WHERE checkout_session_id = ?", String.class, sessionId))
                .containsOnly("PENDING_PAYMENT");
        assertThat(jdbc.queryForList("SELECT r.status FROM stock_reservations r JOIN orders o ON o.id = r.order_id "
                + "WHERE o.checkout_session_id = ?", String.class, sessionId)).containsOnly("CONFIRMED");

        // snapshot dòng đơn + hoa hồng mặc định 5%, không đổi khi người bán đổi giá sau đó
        data.setPrice(a.variantId(), 1_000);
        Map<String, Object> item = jdbc.queryForMap("SELECT unit_price, line_total, commission_rate, commission_amount, "
                + "vendor_receivable, product_name_snapshot FROM order_items WHERE product_variant_id = ?", a.variantId());
        assertThat((BigDecimal) item.get("unit_price")).isEqualByComparingTo("200000");
        assertThat((BigDecimal) item.get("line_total")).isEqualByComparingTo("400000");
        assertThat((BigDecimal) item.get("commission_rate")).isEqualByComparingTo("0.05");
        assertThat((BigDecimal) item.get("commission_amount")).isEqualByComparingTo("20000");
        assertThat((BigDecimal) item.get("vendor_receivable")).isEqualByComparingTo("380000");
        assertThat(item.get("product_name_snapshot")).isNotNull();
        String address = jdbc.queryForObject("SELECT shipping_address_snapshot_json::text FROM orders WHERE "
                + "checkout_session_id = ? LIMIT 1", String.class, sessionId);
        assertThat(address).contains("25 Nguyễn Huệ").contains("\"province_id\": 79");

        mvc.perform(get("/api/v1/me/checkout-sessions/" + sessionId).header(HttpHeaders.AUTHORIZATION, auth(u)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.reservation_expires_at").exists());
        mvc.perform(get("/api/v1/me/checkout-sessions/" + sessionId).header(HttpHeaders.AUTHORIZATION, auth(buyer())))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderCodesContinueAfterRedisLosesTheCounter() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long shop = data.shop();
        long l1 = addLine(u, data.sku(shop, 1_000, 5).variantId(), 1);
        String first = json(place(u, addr, "COD", l1).andReturn(), "$.data.orders[0].code");
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();   // Redis khởi động lại / mất dữ liệu
        long l2 = addLine(u, data.sku(shop, 1_000, 5).variantId(), 1);
        String second = json(place(u, addr, "COD", l2).andExpect(status().isCreated()).andReturn(),
                "$.data.orders[0].code");
        assertThat(second).isNotEqualTo(first);
        assertThat(Long.parseLong(second.substring(12))).isGreaterThan(Long.parseLong(first.substring(12)));
    }

    @Test
    void onlinePaymentKeepsReservationPending() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        Sku s = data.sku(data.shop(), 100_000, 5);
        long l = addLine(u, s.variantId(), 2);
        place(u, addr, "VNPAY", l).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.data.payment.method").value("VNPAY"))
                .andExpect(jsonPath("$.data.payment.payment_url").value(Matchers.nullValue()));
        assertThat(data.reserved(s.variantId())).isEqualTo(2);
        assertThat(data.stock(s.variantId())).isEqualTo(5);
        // giữ chỗ 15 phút tính theo đồng hồ hệ thống (Clock) — cùng hạn với phiên
        java.sql.Timestamp exp = jdbc.queryForObject("SELECT expires_at FROM stock_reservations WHERE product_variant_id = ?",
                java.sql.Timestamp.class, s.variantId());
        assertThat(exp.toInstant()).isEqualTo(java.time.Instant.parse(START).plusSeconds(15 * 60));
    }

    @Test
    void sameIdempotencyKeyCreatesOneSession() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        Sku s = data.sku(data.shop(), 100_000, 5);
        long l = addLine(u, s.variantId(), 1);
        String key = com.genie.support.OrderTestData.key();
        String first = json(place(u, addr, "COD", key, l).andExpect(status().isCreated()).andReturn(), "$.data.id");
        String again = json(place(u, addr, "COD", key, l).andExpect(status().isCreated()).andReturn(), "$.data.id");
        assertThat(again).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM checkout_sessions WHERE user_id = ?", Integer.class, u))
                .isEqualTo(1);
        place(u, addr, "COD", null, l).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void outOfStockRollsBackEverything() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long shop = data.shop();
        Sku ok = data.sku(shop, 100_000, 5);
        Sku low = data.sku(shop, 100_000, 5);
        long l1 = addLine(u, ok.variantId(), 1);
        long l2 = addLine(u, low.variantId(), 3);
        data.setStock(low.variantId(), 5, 4);   // người khác giữ chỗ trước, còn 1
        place(u, addr, "COD", l1, l2).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-INV-4090"))
                .andExpect(jsonPath("$.details.out_of_stock_cart_item_ids[0]").value(l2));
        assertThat(data.reserved(ok.variantId())).isZero();
        assertThat(data.cartLines(u)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM checkout_sessions WHERE user_id = ?", Integer.class, u))
                .isZero();
    }

    @Test
    void vacationShopAndMissingAddressAreRejected() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        long resting = data.shop();
        Sku s = data.sku(resting, 100_000, 5);
        long l = addLine(u, s.variantId(), 1);
        data.vacation(resting, "2026-06-01T00:00:00Z", "2026-06-20T00:00:00Z");
        place(u, addr, "COD", l).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GNE-ORD-4093"))
                .andExpect(jsonPath("$.details.shop_ids[0]").value(data.base().shopIdOf(resting)));
        long other = data.address(buyer(), 1, "00001", true);
        place(u, other, "COD", l).andExpect(status().isUnprocessableEntity());
    }
}
