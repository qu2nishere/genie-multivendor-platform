package com.genie.promotion.voucher.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Xóa cache sau commit (plan 2026-11-26 voucher N5, V03.04): voucher SHOP → bộ lọc "Shop có voucher"
 * ({@code promotion:shop_voucher_vendors} — UC-CAT-05); mọi voucher → danh sách công khai 5.13. Rollback → không xóa.
 */
@Component
public class VoucherCacheEvictListener {

    private final PromotionSearchPorts searchPorts;
    private final PublicVoucherCache publicCache;

    public VoucherCacheEvictListener(PromotionSearchPorts searchPorts, PublicVoucherCache publicCache) {
        this.searchPorts = searchPorts;
        this.publicCache = publicCache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(VoucherChangedEvent e) {
        if (e.shopVoucher()) {
            searchPorts.evictCache();
        }
        publicCache.evictAll();
    }
}
