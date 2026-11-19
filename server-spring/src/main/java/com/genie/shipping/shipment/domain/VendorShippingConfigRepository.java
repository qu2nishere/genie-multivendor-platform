package com.genie.shipping.shipment.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorShippingConfigRepository extends JpaRepository<VendorShippingConfig, Long> {

    /** Cấu hình của shop theo {@code users.id} của chủ shop (UNIQUE vendor_id). */
    Optional<VendorShippingConfig> findByVendorId(Long vendorId);
}
