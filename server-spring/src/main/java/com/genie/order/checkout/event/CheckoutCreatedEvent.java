package com.genie.order.checkout.event;

import java.util.List;

/**
 * Đã tạo phiên checkout và các đơn con (UC-ORD-07 bước 6) — phát trong transaction, người nghe dùng
 * {@code @TransactionalEventListener(AFTER_COMMIT)} (email xác nhận đơn — chuỗi C11, thông báo người bán…).
 *
 * @param paymentMethod {@code VNPAY / VIETQR / COD}
 */
public record CheckoutCreatedEvent(long sessionId, long userId, List<Long> orderIds, String paymentMethod,
                                   long grandTotal) {
}
