package com.genie.order.checkout.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bản mặc định của các cổng checkout mà chuỗi sau sẽ cài (plan C2 nguyên tắc 2, R3): giảm giá (C6), thanh toán online
 * (C3), hoàn voucher / Điểm khi phiên hết hạn (C6). Module nào cài bean cùng kiểu thì bản dưới đây không được tạo.
 */
@Configuration(proxyBeanMethods = false)
public class CheckoutPorts {

    /** C2 không áp voucher / Điểm (D2). */
    @Bean
    @ConditionalOnMissingBean(CheckoutDiscountPolicy.class)
    CheckoutDiscountPolicy noCheckoutDiscounts() {
        return (userId, shops, selection) -> CheckoutDiscountPolicy.Discounts.NONE;
    }

    /** Chưa có cổng thanh toán thật (C3 — D3): khối payment không có liên kết / mã QR. */
    @Bean
    @ConditionalOnMissingBean(PaymentInitiator.class)
    PaymentInitiator stubPaymentInitiator() {
        return request -> new PaymentInitiator.Payment(request.method().name(), null, null);
    }
}
