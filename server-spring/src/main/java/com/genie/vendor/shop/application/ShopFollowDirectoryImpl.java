package com.genie.vendor.shop.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.social.follow.application.ShopFollowDirectory;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài cổng {@link ShopFollowDirectory} của module {@code social} bằng dữ liệu {@code vendor_profiles} (plan theo dõi gian hàng
 * D5, F01.04). Ảnh logo dựng qua {@link ShopFacade#mediaUrl} (plan Mở shop & KYC K10.01) — cùng luật URL với mọi nơi khác.
 * Shop theo dõi được: {@code VERIFIED} + chủ shop {@code ACTIVE} (D4).
 */
@Component
public class ShopFollowDirectoryImpl implements ShopFollowDirectory {

    private final VendorProfileRepository profiles;
    private final UserFacade users;
    private final ShopFacade shops;

    public ShopFollowDirectoryImpl(VendorProfileRepository profiles, UserFacade users, ShopFacade shops) {
        this.profiles = profiles;
        this.users = users;
        this.shops = shops;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FollowableShop> findFollowable(long shopId) {
        return profiles.findById(shopId)
                .filter(p -> p.getKycStatus() == KycStatus.VERIFIED)
                .filter(p -> users.isActive(p.getUserId()))
                .map(p -> new FollowableShop(p.getId(), p.getUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> vendorUserIdOf(long shopId) {
        return profiles.findById(shopId).map(VendorProfile::getUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, FollowedShopView> findVisibleByVendorUserIds(Collection<Long> vendorUserIds) {
        if (vendorUserIds.isEmpty()) {
            return Map.of();
        }
        List<VendorProfile> rows = profiles.findByUserIdIn(vendorUserIds);
        Set<Long> active = users.activeAmong(rows.stream().map(VendorProfile::getUserId).toList());
        Map<Long, FollowedShopView> out = new HashMap<>();
        for (VendorProfile p : rows) {
            if (p.getKycStatus() != KycStatus.VERIFIED || !active.contains(p.getUserId())) {
                continue;   // shop bị khóa / chưa xác minh: không hiện trong danh sách (D4)
            }
            out.put(p.getUserId(), new FollowedShopView(p.getId(), p.getShopName(), shops.mediaUrl(p.getLogoUrl()),
                    Boolean.TRUE.equals(p.getIsMall()), p.getRatingAvg() == null ? 0 : p.getRatingAvg().doubleValue(),
                    p.getFollowerCount() == null ? 0 : p.getFollowerCount()));
        }
        return out;
    }

    @Override
    @Transactional
    public void adjustFollowerCount(long vendorUserId, int delta) {
        profiles.adjustFollowerCount(vendorUserId, delta);
    }
}
