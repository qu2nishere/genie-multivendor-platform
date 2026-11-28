package com.genie.vendor.shop.application;

import com.genie.auth.profile.application.AccountShopProvider;
import com.genie.catalog.product.application.SellerShopPort;
import com.genie.catalog.wishlist.application.WishlistShopAvailability;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module {@code vendor} cài cổng đảo ngược của module khác (plan Mở shop & KYC K10.01–K10.02):
 * <ul>
 *   <li>{@link WishlistShopAvailability} (catalog — plan wishlist D3): shop nào đang tạm nghỉ;</li>
 *   <li>{@link AccountShopProvider} (auth — {@code GET /me}, API 3.1): shop của tài khoản {@code {id, name, logo_url, kyc_status}}.</li>
 *   <li>{@link SellerShopPort} (catalog — đăng bán, plan C1 N3): kho lấy hàng, đặt trước, xóa cache trang shop.</li>
 * </ul>
 */
@Component
public class VendorShopPorts implements WishlistShopAvailability, AccountShopProvider, SellerShopPort {

    private final VendorProfileRepository profiles;
    private final ShopFacade shops;
    private final Clock clock;
    private final PublicShopService publicShops;

    public VendorShopPorts(VendorProfileRepository profiles, ShopFacade shops, Clock clock, PublicShopService publicShops) {
        this.profiles = profiles;
        this.shops = shops;
        this.clock = clock;
        this.publicShops = publicShops;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> vendorIdsOnVacation(Collection<Long> vendorIds) {
        if (vendorIds.isEmpty()) {
            return Set.of();
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        return profiles.findByUserIdIn(vendorIds).stream().filter(p -> p.isOnVacation(now)).map(VendorProfile::getUserId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopSummary> shopOf(long userId) {
        return profiles.findByUserId(userId).map(p -> new ShopSummary(p.getId(), p.getShopName(),
                shops.mediaUrl(p.getLogoUrl()), p.getKycStatus().name()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SellerShopPort.ShopFacts> factsOf(long vendorUserId) {
        return profiles.findByUserId(vendorUserId).map(p -> new SellerShopPort.ShopFacts(p.getId(), p.getShopName(),
                p.isPickupConfigured(), p.getPickupProvince(), Boolean.TRUE.equals(p.getPreOrderEnabled())));
    }

    @Override
    public void evictShopPage(long shopId) {
        publicShops.evict(shopId);
    }
}
