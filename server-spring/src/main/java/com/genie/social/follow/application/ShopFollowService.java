package com.genie.social.follow.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.NotFoundException;
import com.genie.social.follow.domain.ShopFollowRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Theo dõi / hủy theo dõi shop — UC-SOC-06 (API_SPEC 3.11; plan theo dõi gian hàng F02). Idempotent và KHÔNG đếm sai (N3):
 * chỉ cộng {@code follower_count} khi dòng thật sự được chèn, chỉ trừ khi thật sự xóa — cùng một transaction.
 */
@Service
public class ShopFollowService {

    private final ShopFollowRepository follows;
    private final ShopFollowDirectory shops;
    private final Clock clock;

    public ShopFollowService(ShopFollowRepository follows, ShopFollowDirectory shops, Clock clock) {
        this.follows = follows;
        this.shops = shops;
        this.clock = clock;
    }

    @Transactional
    public void follow(long userId, long shopId) {
        ShopFollowDirectory.FollowableShop shop = shops.findFollowable(shopId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy shop " + shopId));
        if (shop.vendorUserId() == userId) {
            throw new BusinessException(ErrorCode.CANNOT_FOLLOW_OWN_SHOP);
        }
        if (follows.insertIfAbsent(userId, shop.vendorUserId(), OffsetDateTime.now(clock)) == 1) {
            shops.adjustFollowerCount(shop.vendorUserId(), +1);
        }
    }

    /** Shop không tồn tại / đã bị khóa vẫn hủy được; không có gì để hủy → vẫn thành công. */
    @Transactional
    public void unfollow(long userId, long shopId) {
        shops.vendorUserIdOf(shopId).ifPresent(vendorUserId -> {
            if (follows.deleteByCustomerIdAndVendorId(userId, vendorUserId) == 1) {
                shops.adjustFollowerCount(vendorUserId, -1);
            }
        });
    }
}
