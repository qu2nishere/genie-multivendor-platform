package com.genie.shipping.shipment.domain;

/**
 * Enum ShipmentTrackingStatus — ENTITY.md Phụ lục A. ← ShipmentTracking.status (9 trạng thái)
 */
public enum ShipmentTrackingStatus {                                                           // ← ShipmentTracking.status (9 trạng thái)
    AWAITING_PICKUP, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED, RETURNING, RETURNED, LOST
}
