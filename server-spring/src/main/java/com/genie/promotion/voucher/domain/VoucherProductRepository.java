package com.genie.promotion.voucher.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherProductRepository extends JpaRepository<VoucherProduct, VoucherProductId> {
}
