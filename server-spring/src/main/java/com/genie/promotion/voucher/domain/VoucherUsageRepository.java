package com.genie.promotion.voucher.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    /** Số lần một người đã dùng một voucher — so với {@code usage_per_user} ({@code UserVoucher.isUsed}, ENTITY 4.3). */
    long countByUserIdAndVoucherId(Long userId, Long voucherId);
}
