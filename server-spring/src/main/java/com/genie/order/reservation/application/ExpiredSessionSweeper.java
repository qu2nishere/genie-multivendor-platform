// Cron hủy phiên checkout quá hạn thanh toán & hoàn kho (UC-ORD-08; LLD 10; TS 10.3; plan C2 3.5, D13)
package com.genie.order.reservation.application;

import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditActor;
import com.genie.common.audit.AuditLogger;
import com.genie.common.domain.PaymentStatus;
import com.genie.common.security.ClientInfo;
import com.genie.order.checkout.application.CheckoutCompensation;
import com.genie.order.checkout.domain.CheckoutSession;
import com.genie.order.checkout.domain.CheckoutSessionRepository;
import com.genie.order.order.domain.Order;
import com.genie.order.order.domain.OrderRepository;
import com.genie.order.order.domain.OrderStatus;
import com.genie.order.reservation.domain.StockReservation;
import com.genie.order.reservation.domain.StockReservationRepository;
import com.genie.order.reservation.domain.StockReservationStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mỗi 60 giây hủy các phiên {@code INITIATED / PAYMENT_PENDING} đã quá {@code expires_at} (15 phút giữ hàng):
 * <ol>
 *   <li>Lấy lô {@value #BATCH_SIZE} id phiên quá hạn; MỖI PHIÊN một transaction, khóa phiên {@code FOR UPDATE SKIP LOCKED}
 *       (instance khác / IPN đang giữ → để lượt sau). Lỗi một phiên chỉ ghi log, không chặn phiên khác.</li>
 *   <li>Có lượt giữ chỗ đã {@code CONFIRMED} (IPN báo trả tiền ngay trước đó — C3) → không hủy phiên.</li>
 *   <li>Mỗi lượt giữ chỗ {@code ACTIVE → EXPIRED} bằng UPDATE có điều kiện, trúng mới nhả {@code reserved_count} — không
 *       trừ hai lần khi hai tiến trình chạy cùng lúc.</li>
 *   <li>Phiên → {@code EXPIRED}; đơn {@code PENDING_PAYMENT → AUTO_CANCELLED} ({@code payment_status = EXPIRED}); nhật ký
 *       {@code ORDER_STATUS_CHANGED} ({@code reason = PAYMENT_TIMEOUT}, người làm = hệ thống); cổng
 *       {@link CheckoutCompensation} hoàn voucher / Điểm (C6).</li>
 * </ol>
 * Phiên COD đã {@code COMPLETED} lúc đặt nên không bao giờ bị quét.
 */
@Component
public class ExpiredSessionSweeper {

    private static final Logger log = LoggerFactory.getLogger(ExpiredSessionSweeper.class);
    static final int BATCH_SIZE = 50;
    static final int MAX_PER_RUN = 500;
    public static final String REASON = "PAYMENT_TIMEOUT";

    private final CheckoutSessionRepository sessions;
    private final OrderRepository orders;
    private final StockReservationRepository reservations;
    private final ReservationService reservationService;
    private final CheckoutCompensation compensation;
    private final AuditLogger audit;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ExpiredSessionSweeper(CheckoutSessionRepository sessions, OrderRepository orders,
                                 StockReservationRepository reservations, ReservationService reservationService,
                                 CheckoutCompensation compensation, AuditLogger audit,
                                 PlatformTransactionManager transactionManager, Clock clock) {
        this.sessions = sessions;
        this.orders = orders;
        this.reservations = reservations;
        this.reservationService = reservationService;
        this.compensation = compensation;
        this.audit = audit;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "PT60S", initialDelayString = "PT30S")
    public void scheduled() {
        int done = run();
        if (done > 0) {
            log.info("Đã hủy {} phiên quá hạn thanh toán", done);
        }
    }

    /** Hủy mọi phiên quá hạn (tối đa {@value #MAX_PER_RUN} / lượt); trả số phiên đã hủy. */
    public int run() {
        int done = 0;
        int seen = 0;
        OffsetDateTime now = OffsetDateTime.now(clock);
        while (seen < MAX_PER_RUN) {
            List<Long> ids = sessions.findExpiredIds(now, BATCH_SIZE);
            if (ids.isEmpty()) {
                break;
            }
            int expiredInBatch = 0;
            for (Long id : ids) {
                seen++;
                try {
                    Boolean expired = tx.execute(status -> expire(id, now));
                    if (Boolean.TRUE.equals(expired)) {
                        done++;
                        expiredInBatch++;
                    }
                } catch (RuntimeException e) {
                    log.error("Hủy phiên checkout {} quá hạn lỗi, bỏ qua lượt này: {}", id, e.toString());
                }
            }
            if (ids.size() < BATCH_SIZE || expiredInBatch == 0) {
                break;   // hết phiên tới hạn, hoặc cả lô đang bị khóa / bỏ qua — để lượt sau
            }
        }
        return done;
    }

    /** Hủy một phiên; {@code false} = phiên bị khóa / đã đổi trạng thái / đã có giữ chỗ được chốt. */
    boolean expire(long sessionId, OffsetDateTime now) {
        CheckoutSession session = sessions.lockIfExpired(sessionId, now).orElse(null);
        if (session == null) {
            return false;
        }
        List<Order> list = orders.findByCheckoutSessionIdOrderByIdAsc(sessionId);
        List<Long> orderIds = list.stream().map(Order::getId).toList();
        List<StockReservation> held = orderIds.isEmpty() ? List.of()
                : reservations.findByOrderIdInOrderByProductVariantIdAsc(orderIds);
        if (held.stream().anyMatch(r -> r.getStatus() == StockReservationStatus.CONFIRMED)) {
            return false;   // IPN đã chốt thanh toán — không hủy
        }
        for (StockReservation r : held) {
            reservationService.releaseIfActive(r, StockReservationStatus.EXPIRED, now);
        }
        session.expire();
        sessions.save(session);
        for (Order o : list) {
            if (o.getStatus() != OrderStatus.PENDING_PAYMENT) {
                continue;
            }
            o.cancel(OrderStatus.AUTO_CANCELLED);
            o.setPaymentStatus(PaymentStatus.EXPIRED);
            orders.save(o);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("order_code", o.getOrderCode());
            details.put("from", OrderStatus.PENDING_PAYMENT.name());
            details.put("to", OrderStatus.AUTO_CANCELLED.name());
            details.put("reason", REASON);
            details.put("checkout_session_id", sessionId);
            audit.record(AuditAction.ORDER_STATUS_CHANGED, AuditActor.SYSTEM, String.valueOf(o.getId()), details,
                    ClientInfo.system(), true);
        }
        compensation.onExpired(sessionId, session.getUserId(), orderIds);
        return true;
    }
}
