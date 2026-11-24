// Module promotion cài cổng "Shop có voucher" của cụm tìm kiếm (API_SPEC 5.3; plan 2026-11-17 D10; plan 2026-11-24 D5–D6)
package com.genie.promotion.voucher.application;

import com.fasterxml.jackson.databind.JavaType;
import com.genie.catalog.search.application.ShopVoucherAvailability;
import com.genie.common.cache.JsonCache;
import com.genie.promotion.voucher.domain.VoucherRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ShopVoucherAvailability} cho bộ lọc {@code has_shop_voucher}: danh sách {@code users.id} chủ shop đang có
 * voucher SHOP dùng được (quy tắc ở {@link VoucherRepository#findVendorIdsWithActiveShopVoucher}).
 * <ul>
 *   <li>Cache Redis {@value #CACHE_KEY} {@link #TTL} giây qua {@link JsonCache} — tìm kiếm gọi cổng mỗi request (2 lần khi
 *       có facet). Redis lỗi → đọc thẳng CSDL. Cụm B5 (tạo / sửa / tắt voucher shop) gọi {@link #evictCache()} sau commit.</li>
 *   <li>Luôn trả {@code Optional.of(...)} (kể cả danh sách rỗng) → {@code has_shop_voucher_supported = true}; rỗng
 *       nghĩa là không shop nào có voucher, lọc ra 0 sản phẩm.</li>
 * </ul>
 * Bean này có mặt thì cài đặt mặc định trong {@code SearchPortsConfig} ({@code @ConditionalOnMissingBean}) tự tắt.
 */
@Component
public class PromotionSearchPorts implements ShopVoucherAvailability {

    static final String CACHE_KEY = "promotion:shop_voucher_vendors";
    static final Duration TTL = Duration.ofSeconds(60);

    private final VoucherRepository vouchers;
    private final JsonCache cache;
    private final JavaType idsType;
    private final Clock clock;

    public PromotionSearchPorts(VoucherRepository vouchers, JsonCache cache, Clock clock) {
        this.vouchers = vouchers;
        this.cache = cache;
        this.idsType = cache.listOf(Long.class);
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<Long>> vendorIdsWithActiveVoucher() {
        List<Long> ids = cache.getOrLoad(CACHE_KEY, idsType, TTL,
                () -> vouchers.findVendorIdsWithActiveShopVoucher(OffsetDateTime.now(clock)));
        return Optional.of(ids == null ? List.of() : ids);
    }

    /** Xóa cache sau khi voucher shop thay đổi (cụm B5 gọi sau commit). */
    public void evictCache() {
        cache.evict(CACHE_KEY);
    }
}
