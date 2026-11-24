package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.CatalogITSupport;
import com.genie.catalog.search.application.ShopVoucherAvailability;
import com.genie.promotion.voucher.application.PromotionSearchPorts;
import com.genie.support.TestDataBuilder.Shop;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cổng "Shop có voucher" của module promotion (API_SPEC 5.3; plan 2026-11-24 H08.03–H08.04, D5–D6).
 * CSDL dùng chung → mỗi ca dựng shop riêng và chỉ khẳng định shop đó có / không có trong danh sách.
 */
class PromotionSearchPortsIT extends CatalogITSupport {

    @Autowired
    ShopVoucherAvailability port;

    @Autowired
    PromotionSearchPorts promotionPorts;

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    /** Đọc thẳng (bỏ cache) — mỗi ca tự xóa cache trước khi hỏi. */
    private List<Long> vendors() {
        promotionPorts.evictCache();
        return port.vendorIdsWithActiveVoucher().orElseThrow();
    }

    @Test
    void promotionModuleReplacesTheDefaultPort() {
        assertThat(port).isInstanceOf(PromotionSearchPorts.class);
        assertThat(port.vendorIdsWithActiveVoucher()).isPresent();   // has_shop_voucher_supported = true
    }

    @Test
    void activeShopVoucherWithUsesLeftCounts() {
        Shop shop = data.shop().create();
        data.voucher(shop, now()).create();
        assertThat(vendors()).contains(shop.userId());
    }

    @Test
    void validityWindowIsRespected() {
        Shop notStarted = data.shop().create();
        data.voucher(notStarted, now()).valid(now().plusMinutes(1), now().plusDays(5)).create();
        Shop ended = data.shop().create();
        data.voucher(ended, now()).valid(now().minusDays(5), now().minusMinutes(1)).create();
        Shop endsNow = data.shop().create();
        data.voucher(endsNow, now()).valid(now().minusDays(5), now()).create();          // valid_to > now (không bằng)
        Shop startsNow = data.shop().create();
        data.voucher(startsNow, now()).valid(now(), now().plusDays(5)).create();         // valid_from ≤ now

        List<Long> ids = vendors();
        assertThat(ids).doesNotContain(notStarted.userId(), ended.userId(), endsNow.userId());
        assertThat(ids).contains(startsNow.userId());
    }

    @Test
    void inactiveUsedUpOrOverBudgetVouchersDoNotCount() {
        Shop inactive = data.shop().create();
        data.voucher(inactive, now()).inactive().create();
        Shop usedUp = data.shop().create();
        data.voucher(usedUp, now()).uses(100, 100).create();
        Shop overBudget = data.shop().create();
        data.voucher(overBudget, now()).budget(1_000_000, 1_000_000).create();
        Shop budgetLeft = data.shop().create();
        data.voucher(budgetLeft, now()).budget(1_000_000, 999_999).create();
        Shop oneUseLeft = data.shop().create();
        data.voucher(oneUseLeft, now()).uses(99, 100).create();

        List<Long> ids = vendors();
        assertThat(ids).doesNotContain(inactive.userId(), usedUp.userId(), overBudget.userId());
        assertThat(ids).contains(budgetLeft.userId(), oneUseLeft.userId());
    }

    @Test
    void platformAndFreeshipVouchersAreNotShopVouchers() {
        Shop shop = data.shop().create();
        long platform = data.voucher(shop, now()).type("PLATFORM").create();
        long freeship = data.voucher(shop, now()).type("FREESHIP").create();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM vouchers WHERE id IN (?, ?) AND vendor_id IS NULL",
                Integer.class, platform, freeship)).isEqualTo(2);
        assertThat(vendors()).doesNotContain(shop.userId());
    }

    @Test
    void aShopWithOneBrokenAndOneGoodVoucherIsListedOnce() {
        Shop shop = data.shop().create();
        data.voucher(shop, now()).inactive().create();
        data.voucher(shop, now()).create();
        data.voucher(shop, now()).create();

        assertThat(vendors()).containsOnlyOnce(shop.userId());
    }

    @Test
    void listIsCachedForSixtySecondsAndEvictable() {
        Shop first = data.shop().create();
        data.voucher(first, now()).create();
        assertThat(vendors()).contains(first.userId());                     // nạp + lưu cache

        Shop later = data.shop().create();
        data.voucher(later, now()).create();
        assertThat(port.vendorIdsWithActiveVoucher().orElseThrow()).doesNotContain(later.userId());   // còn bản cache

        Long ttl = redis.getExpire("cache:promotion:shop_voucher_vendors");
        assertThat(ttl).isBetween(1L, 60L);

        promotionPorts.evictCache();
        assertThat(port.vendorIdsWithActiveVoucher().orElseThrow()).contains(later.userId());
    }
}
