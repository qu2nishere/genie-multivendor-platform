package com.genie.order.checkout.application;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@code $CheckoutSession} (API_SPEC 6.11) + khối {@code payment} khi vừa đặt (6.10). {@code reservationExpiresAt} =
 * cột {@code checkout_sessions.expires_at}.
 */
public record CheckoutView(long id, String code, String status, String paymentMethod, OffsetDateTime createdAt,
                           OffsetDateTime reservationExpiresAt, List<OrderLine> orders, Summary summary,
                           PaymentInitiator.Payment payment) {

    public record Shop(long id, String name) {
    }

    public record OrderLine(long id, String code, Shop shop, long grandTotal) {
    }

    public record Summary(long subtotal, long shippingTotal, long discountTotal, long coinDiscount, long grandTotal) {
    }

    public CheckoutView withPayment(PaymentInitiator.Payment p) {
        return new CheckoutView(id, code, status, paymentMethod, createdAt, reservationExpiresAt, orders, summary, p);
    }
}
