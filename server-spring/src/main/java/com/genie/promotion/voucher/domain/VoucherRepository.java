package com.genie.promotion.voucher.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    /** Mã voucher là duy nhất toàn hệ thống ({@code vouchers.code UNIQUE}). */
    Optional<Voucher> findByCode(String code);

    List<Voucher> findByCodeIn(Collection<String> codes);
}
