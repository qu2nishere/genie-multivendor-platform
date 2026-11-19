package com.genie.admin.banner.application;

import com.genie.admin.banner.event.BannerChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Xóa cache banner ngay sau khi thay đổi đã commit (plan banner N7, B04.04) — "Tắt ngay" có hiệu lực tức thì thay vì
 * chờ hết TTL 60 giây. Chạy đồng bộ (thao tác Redis nhỏ) để lần đọc kế tiếp chắc chắn thấy dữ liệu mới.
 */
@Component
public class BannerCacheEvictListener {

    private final BannerCache cache;

    public BannerCacheEvictListener(BannerCache cache) {
        this.cache = cache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBannerChanged(BannerChangedEvent event) {
        cache.evictAll();
    }
}
