// Đơn COD vừa đặt → báo người mua + từng shop (UC-ORD-07 bước 6, UC-NOT-01; plan 2026-11-26 thông báo D11e, N03.09)
package com.genie.order.checkout.application;

import com.genie.notification.inbox.application.NotificationCommand;
import com.genie.notification.inbox.application.NotificationFacade;
import com.genie.notification.inbox.application.NotificationLink;
import com.genie.notification.inbox.application.NotificationKind;
import com.genie.order.checkout.event.CheckoutCreatedEvent;
import com.genie.order.order.domain.Order;
import com.genie.order.order.domain.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Chỉ COD: đơn đã chốt ngay khi đặt. Đơn VNPAY / VIETQR còn chờ thanh toán → báo khi thanh toán thành công
 * (chuỗi C3, {@code PAYMENT_SUCCESS}). File mới của plan thông báo — không sửa {@code CheckoutService} (C2).
 */
@Component
public class CheckoutNotificationListener {

    private final OrderRepository orders;
    private final NotificationFacade notifications;

    public CheckoutNotificationListener(OrderRepository orders, NotificationFacade notifications) {
        this.orders = orders;
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckoutCreated(CheckoutCreatedEvent e) {
        if (!"COD".equals(e.paymentMethod())) {
            return;
        }
        List<Order> placed = orders.findByCheckoutSessionIdOrderByIdAsc(e.sessionId());
        if (placed.isEmpty()) {
            return;
        }
        List<NotificationCommand> cmds = new ArrayList<>();
        List<String> codes = placed.stream().map(Order::getOrderCode).toList();
        String buyerBody = placed.size() == 1
                ? "Đơn " + codes.get(0) + " đã được đặt (thanh toán khi nhận hàng). Shop sẽ xác nhận sớm."
                : placed.size() + " đơn " + String.join(", ", codes) + " đã được đặt (thanh toán khi nhận hàng).";
        cmds.add(new NotificationCommand(e.userId(), NotificationKind.ORDER_STATUS_CHANGED, "Đặt hàng thành công",
                buyerBody, null, NotificationLink.of(NotificationLink.ORDER, placed.get(0).getId()),
                Map.of("event", "ORDER_PLACED", "order_codes", codes, "payment_method", "COD")));
        for (Order o : placed) {
            cmds.add(new NotificationCommand(o.getVendorId(), NotificationKind.NEW_ORDER_FOR_VENDOR,
                    "Đơn hàng mới " + o.getOrderCode(),
                    "Bạn có đơn COD mới cần xác nhận.", null, NotificationLink.of(NotificationLink.ORDER, o.getId()),
                    Map.of("event", "NEW_ORDER", "order_code", o.getOrderCode())));
        }
        notifications.sendManyNow(cmds);
    }
}
