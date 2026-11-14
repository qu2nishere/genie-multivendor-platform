package com.genie.common.domain;

/**
 * Phương thức thanh toán — PaymentTransaction.paymentMethod, Order.paymentMethod (ENTITY.md Phụ lục A, nhóm 6).
 * VIETQR = chuyển khoản, xác nhận bằng webhook SePay.
 */
public enum PaymentMethod {
    VNPAY, VIETQR, COD   // mở rộng: MOMO, ZALOPAY, INTERNAL_WALLET, BNPL
}
