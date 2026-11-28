// Cache danh sách voucher công khai (API_SPEC 5.13; plan 2026-11-26 voucher D15, N5)
package com.genie.promotion.voucher.application;

import com.fasterxml.jackson.databind.JavaType;
import com.genie.common.cache.JsonCache;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Redis 60 giây qua {@link JsonCache} (Redis lỗi → đọc CSDL): {@code promotion:public_vouchers:featured},
 * {@code promotion:public_vouchers:shop:{shopId}}. Lưu danh sách id (thứ tự hiển thị) — {@code $Voucher} dựng lại mỗi lần
 * vì {@code state} / {@code is_saved} phụ thuộc thời điểm và người gọi. Voucher nào đổi → xóa cả nhóm.
 */
@Component
public class PublicVoucherCache {

    static final String PREFIX = "promotion:public_vouchers:";
    static final Duration TTL = Duration.ofSeconds(60);

    private final JsonCache cache;
    private final JavaType idList;

    public PublicVoucherCache(JsonCache cache) {
        this.cache = cache;
        this.idList = cache.listOf(Long.class);
    }

    public List<Long> featured(Supplier<List<Long>> loader) {
        return cache.getOrLoad(PREFIX + "featured", idList, TTL, loader);
    }

    public List<Long> shop(long shopId, Supplier<List<Long>> loader) {
        return cache.getOrLoad(PREFIX + "shop:" + shopId, idList, TTL, loader);
    }

    public void evictAll() {
        cache.evictPrefix(PREFIX);
    }
}
