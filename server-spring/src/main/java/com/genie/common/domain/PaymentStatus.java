package com.genie.common.domain;

/**
 * Trạng thái thanh toán — PaymentTransaction.status, Order.paymentStatus (ENTITY.md Phụ lục A, nhóm 6).
 */
public enum PaymentStatus {
    PENDING, REQUIRES_ACTION, SUCCESS, FAILED, EXPIRED, REFUNDED, PARTIALLY_REFUNDED, CANCELLED
}
