package com.genie.support.refund.domain;

/**
 * Enum RefundStatus — ENTITY.md Phụ lục A. ← RefundRequest.status (9 trạng thái)
 */
public enum RefundStatus {                                                                     // ← RefundRequest.status (9 trạng thái)
    REQUESTED, AWAITING_EVIDENCE, SHOP_REJECTED, DISPUTED, AWAITING_RETURN_SHIPMENT, RETURN_FAILED, APPROVED, REJECTED, REFUNDED
}
