package com.genie.social.follow.application;

import com.genie.social.follow.domain.ShopFollowRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của "theo dõi shop" cho module khác (TS 4.4; plan theo dõi gian hàng D6, D7, F03.04) — chưa có nơi gọi:
 * {@code notification} (báo voucher / Flash Sale mới của shop tới người theo dõi — UC-SOC-06 bước 5) và {@code auth}
 * ({@code GET /me} → {@code counters.following_shops} qua {@code AccountCounterContributor}).
 */
@Component
public class ShopFollowFacade {

    private final ShopFollowRepository follows;

    public ShopFollowFacade(ShopFollowRepository follows) {
        this.follows = follows;
    }

    /** {@code users.id} của những người đang theo dõi shop (theo {@code users.id} chủ shop). */
    @Transactional(readOnly = true)
    public List<Long> followerIdsOf(long vendorUserId) {
        return follows.findCustomerIdsByVendorId(vendorUserId);
    }

    /** Số shop một người đang theo dõi (kể cả shop đã bị khóa — bộ đếm thô). */
    @Transactional(readOnly = true)
    public long countFollowing(long userId) {
        return follows.countByCustomerId(userId);
    }
}
