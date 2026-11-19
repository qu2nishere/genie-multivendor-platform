package com.genie.vendor.kyc.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycResultRepository extends JpaRepository<KycResult, Long> {

    /** Kết quả thẩm định của một shop (UNIQUE vendor_profile_id — 1 dòng / shop). */
    Optional<KycResult> findByVendorProfileId(Long vendorProfileId);
}
