package com.genie.promotion.voucher.domain;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    /** Mã voucher là duy nhất toàn hệ thống ({@code vouchers.code UNIQUE}). */
    Optional<Voucher> findByCode(String code);

    List<Voucher> findByCodeIn(Collection<String> codes);

    /**
     * {@code users.id} chủ shop đang có ít nhất một voucher SHOP dùng được — bộ lọc "Shop có voucher" (API_SPEC 5.3,
     * UC-CAT-05; plan 2026-11-24 D5). Cùng điều kiện thời gian / lượt / ngân sách với {@link Voucher#isApplicable},
     * không xét đơn tối thiểu; voucher PLATFORM / FREESHIP không tính.
     */
    @Query(value = "SELECT DISTINCT vendor_id FROM vouchers WHERE type = 'SHOP' AND is_active AND vendor_id IS NOT NULL "
            + "AND valid_from <= :now AND valid_to > :now AND used_count < usage_limit "
            + "AND (budget IS NULL OR total_discount_used < budget) ORDER BY vendor_id", nativeQuery = true)
    List<Long> findVendorIdsWithActiveShopVoucher(@Param("now") OffsetDateTime now);
}
