package com.genie.promotion.voucher.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    /** Voucher trong Kho Voucher của một người ({@code UNIQUE (user_id, voucher_id)}). */
    Optional<UserVoucher> findByUserIdAndVoucherId(Long userId, Long voucherId);
}
