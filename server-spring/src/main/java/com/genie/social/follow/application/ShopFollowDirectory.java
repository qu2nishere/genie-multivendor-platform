package com.genie.social.follow.application;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Cổng đảo ngược: tra shop và đổi bộ đếm người theo dõi (plan theo dõi gian hàng F01.02, D5; TS 4.4). Dữ liệu shop thuộc module
 * {@code vendor}; {@code social} chỉ biết cổng này — module {@code vendor} cài đặt ({@code ShopFollowDirectoryImpl}), nên không có
 * vòng phụ thuộc {@code social ↔ vendor}.
 * <p>{@code shopId} = {@code vendor_profiles.id} (trên API); {@code vendorUserId} = {@code users.id} của chủ shop (lưu ở
 * {@code shop_follows.vendor_id}) — D1.
 */
public interface ShopFollowDirectory {

    /** Shop theo dõi được: {@code kyc_status = VERIFIED} và tài khoản chủ shop {@code ACTIVE} (D4). */
    Optional<FollowableShop> findFollowable(long shopId);

    /** {@code users.id} của chủ shop — KHÔNG lọc trạng thái (dùng khi hủy theo dõi shop đã bị khóa). */
    Optional<Long> vendorUserIdOf(long shopId);

    /** Thông tin hiển thị của các shop CÒN theo dõi được, theo {@code vendorUserId} (một truy vấn cho cả trang). */
    Map<Long, FollowedShopView> findVisibleByVendorUserIds(Collection<Long> vendorUserIds);

    /** {@code follower_count += delta} nguyên tử, không âm (N4). */
    void adjustFollowerCount(long vendorUserId, int delta);

    record FollowableShop(long shopId, long vendorUserId) {
    }

    /** {@code $ShopMini} + chỉ số cho màn "Shop đang theo dõi" (API_SPEC 3.11). */
    record FollowedShopView(long shopId, String name, String logoUrl, boolean isMall, double ratingAvg,
                            int followerCount) {
    }
}
