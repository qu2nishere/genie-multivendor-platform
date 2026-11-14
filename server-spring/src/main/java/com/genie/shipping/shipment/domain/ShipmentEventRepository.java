package com.genie.shipping.shipment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
}
