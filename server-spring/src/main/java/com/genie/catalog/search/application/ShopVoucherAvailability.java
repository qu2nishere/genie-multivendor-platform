package com.genie.catalog.search.application;

import java.util.List;
import java.util.Optional;

/**
 * Cổng đảo ngược cho bộ lọc "Shop có voucher" ({@code has_shop_voucher}, API_SPEC 5.3; plan 2026-11-17 D10).
 * Module {@code promotion} cài khi có voucher shop; tới lúc đó cài đặt mặc định trả {@link Optional#empty()}
 * = chưa hỗ trợ → bộ lọc bị bỏ qua và {@code facets.has_shop_voucher_supported = false}.
 */
public interface ShopVoucherAvailability {

    /** {@code users.id} của các shop đang có ít nhất một voucher còn hiệu lực; rỗng = chưa hỗ trợ. */
    Optional<List<Long>> vendorIdsWithActiveVoucher();
}
