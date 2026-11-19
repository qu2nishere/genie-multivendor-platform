package com.genie.vendor.shop.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.common.storage.ObjectStorage;
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
 * D5, F01.04). Khi plan Mở shop & KYC có {@code ShopFacade} thì lớp này gọi lại nó thay vì truy vấn repository trực tiếp.
 * Shop theo dõi được: {@code VERIFIED} + chủ shop {@code ACTIVE} (D4).
 */
@Component
public class ShopFollowDirectoryImpl implements ShopFollowDirectory {

    /** Bucket ảnh logo shop (LUU_TRU 5) — cột lưu khóa đối tượng thì ghép URL công khai. */
    static final String SHOP_MEDIA = "content-assets";

    private final VendorProfileRepository profiles;
    private final UserFacade users;
    private final ObjectStorage storage;

    public ShopFollowDirectoryImpl(VendorProfileRepository profiles, UserFacade users, ObjectStorage storage) {
        this.profiles = profiles;
        this.users = users;
        this.storage = storage;
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
            out.put(p.getUserId(), new FollowedShopView(p.getId(), p.getShopName(), logo(p.getLogoUrl()),
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

    private String logo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.startsWith("http://") || value.startsWith("https://") ? value : storage.publicUrl(SHOP_MEDIA, value);
    }
}
