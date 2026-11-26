package com.genie.order.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.order.checkout.CheckoutTestSupport;
import com.genie.order.reservation.application.ExpiredSessionSweeper;
import com.genie.support.OrderTestData.Sku;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** UC-ORD-08 — hủy phiên quá hạn thanh toán & hoàn kho (plan C2 O08.04, D13). */
class ExpiredSessionSweeperIT extends CheckoutTestSupport {

    @Autowired
    private ExpiredSessionSweeper sweeper;

    private record Placed(long sessionId, Sku sku) {
    }

    private Placed placeOnline(int stock, int qty) throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        Sku s = data.sku(data.shop(), 100_000, stock);
        long line = addLine(u, s.variantId(), qty);
        long id = Long.parseLong(json(place(u, addr, "VNPAY", line).andReturn(), "$.data.id"));
        return new Placed(id, s);
    }

    private String sessionStatus(long id) {
        return jdbc.queryForObject("SELECT status FROM checkout_sessions WHERE id = ?", String.class, id);
    }

    private List<String> orderStatuses(long sessionId) {
        return jdbc.queryForList("SELECT status FROM orders WHERE checkout_session_id = ?", String.class, sessionId);
    }

    @Test
    void expiredOnlineSessionIsCancelledAndStockReturned() throws Exception {
        Placed p = placeOnline(5, 2);
        assertThat(data.reserved(p.sku().variantId())).isEqualTo(2);

        clock.advance(Duration.ofMinutes(14));
        sweeper.run();
        assertThat(sessionStatus(p.sessionId())).isEqualTo("PAYMENT_PENDING");   // chưa tới hạn

        clock.advance(Duration.ofMinutes(2));
        assertThat(sweeper.run()).isGreaterThanOrEqualTo(1);
        assertThat(sessionStatus(p.sessionId())).isEqualTo("EXPIRED");
        assertThat(orderStatuses(p.sessionId())).containsOnly("AUTO_CANCELLED");
        assertThat(data.reserved(p.sku().variantId())).isZero();
        assertThat(data.stock(p.sku().variantId())).isEqualTo(5);
        assertThat(jdbc.queryForList("SELECT r.status FROM stock_reservations r JOIN orders o ON o.id = r.order_id "
                + "WHERE o.checkout_session_id = ?", String.class, p.sessionId())).containsOnly("EXPIRED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM system_logs WHERE action_type = 'ORDER_STATUS_CHANGED' "
                + "AND details LIKE '%PAYMENT_TIMEOUT%' AND user_role = 'SYSTEM' AND reference_id IN "
                + "(SELECT CAST(id AS varchar) FROM orders WHERE checkout_session_id = ?)", Integer.class,
                p.sessionId())).isEqualTo(1);

        sweeper.run();   // chạy lại không nhả lần hai
        assertThat(data.reserved(p.sku().variantId())).isZero();
    }

    @Test
    void confirmedReservationMeansPaymentArrivedAndSessionIsKept() throws Exception {
        Placed p = placeOnline(5, 1);
        // IPN (C3) chốt thanh toán ngay trước khi cron chạy
        jdbc.update("UPDATE stock_reservations SET status = 'CONFIRMED' WHERE order_id IN "
                + "(SELECT id FROM orders WHERE checkout_session_id = ?)", p.sessionId());
        jdbc.update("UPDATE product_variants SET stock_count = stock_count - 1, reserved_count = reserved_count - 1 "
                + "WHERE id = ?", p.sku().variantId());
        clock.advance(Duration.ofMinutes(16));
        sweeper.run();
        assertThat(sessionStatus(p.sessionId())).isEqualTo("PAYMENT_PENDING");
        assertThat(orderStatuses(p.sessionId())).containsOnly("PENDING_PAYMENT");
        assertThat(data.stock(p.sku().variantId())).isEqualTo(4);
        assertThat(data.reserved(p.sku().variantId())).isZero();
    }

    @Test
    void codSessionsAreNeverSwept() throws Exception {
        long u = buyer();
        long addr = data.address(u, 79, "26740", true);
        Sku s = data.sku(data.shop(), 100_000, 5);
        long id = Long.parseLong(json(place(u, addr, "COD", addLine(u, s.variantId(), 1)).andReturn(), "$.data.id"));
        clock.advance(Duration.ofHours(1));
        sweeper.run();
        assertThat(sessionStatus(id)).isEqualTo("COMPLETED");
        assertThat(orderStatuses(id)).containsOnly("PENDING_PAYMENT");
        assertThat(data.stock(s.variantId())).isEqualTo(4);
    }

    @Test
    void parallelSweepersReleaseOnlyOnce() throws Exception {
        Placed a = placeOnline(10, 3);
        Placed b = placeOnline(10, 2);
        clock.advance(Duration.ofMinutes(16));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        Future<Integer> f1 = pool.submit(() -> { go.await(); return sweeper.run(); });
        Future<Integer> f2 = pool.submit(() -> { go.await(); return sweeper.run(); });
        go.countDown();
        f1.get();
        f2.get();
        pool.shutdown();
        assertThat(sessionStatus(a.sessionId())).isEqualTo("EXPIRED");
        assertThat(sessionStatus(b.sessionId())).isEqualTo("EXPIRED");
        assertThat(data.reserved(a.sku().variantId())).isZero();
        assertThat(data.reserved(b.sku().variantId())).isZero();
        assertThat(data.stock(a.sku().variantId())).isEqualTo(10);
    }

    @Test
    void oneBrokenSessionDoesNotBlockOthers() throws Exception {
        Placed broken = placeOnline(5, 2);
        Placed ok = placeOnline(5, 1);
        data.setStock(broken.sku().variantId(), 5, 0);   // dữ liệu hỏng: reserved_count < lượt giữ → nhả lỗi
        clock.advance(Duration.ofMinutes(16));
        sweeper.run();
        assertThat(sessionStatus(ok.sessionId())).isEqualTo("EXPIRED");
        assertThat(sessionStatus(broken.sessionId())).isEqualTo("PAYMENT_PENDING");   // rollback nguyên phiên lỗi
        assertThat(orderStatuses(broken.sessionId())).containsOnly("PENDING_PAYMENT");
        // dọn để lượt quét của test khác không gặp lại phiên hỏng
        jdbc.update("UPDATE checkout_sessions SET status = 'FAILED' WHERE id = ?", broken.sessionId());
    }
}
