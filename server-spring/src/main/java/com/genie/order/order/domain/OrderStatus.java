package com.genie.order.order.domain;

/**
 * Enum OrderStatus — ENTITY.md Phụ lục A. ← Order.status (19 trạng thái — giữ đủ vì GHN cũng có giao thất bại / hoàn hàng)
 */
public enum OrderStatus {                                                                      // ← Order.status (19 trạng thái — giữ đủ vì GHN cũng có giao thất bại / hoàn hàng)
    PENDING_PAYMENT,   // Online: chờ thanh toán | COD: chờ shop xác nhận (đã có đơn)
    PAID,              // Online: đã TT | COD: sàn đã nhận tiền thu hộ từ GHN (fund escrow HELD)
    CONFIRMED, PREPARING, READY_FOR_PICKUP,
    DELIVERING, DELIVERED, DELIVERY_FAILED, RETURNING, RETURNED_TO_VENDOR,
    COMPLETED, RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED,
    PARTIALLY_REFUNDED, // đã hoàn một phần, phần còn lại vẫn giao dịch bình thường
    REFUNDED, CANCELLED, CANCELLED_BY_VENDOR, AUTO_CANCELLED
}
