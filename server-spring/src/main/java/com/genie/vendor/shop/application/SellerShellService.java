package com.genie.vendor.shop.application;

import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@code GET /seller/me} — khung Seller Center: shop, trạng thái KYC, badge (API_SPEC 10.1; plan Mở shop & KYC K03.07). */
@Service
public class SellerShellService {

    private final VendorProfileRepository profiles;
    private final ShopFacade shops;
    private final SellerContext sellers;
    private final List<SellerBadgeContributor> badges;

    public SellerShellService(VendorProfileRepository profiles, ShopFacade shops, SellerContext sellers,
                              List<SellerBadgeContributor> badges) {
        this.profiles = profiles;
        this.shops = shops;
        this.sellers = sellers;
        this.badges = badges;
    }

    public record SellerShell(ShopFacade.ShopMiniView shop, String kycStatus, Map<String, Long> badges) {
    }

    @Transactional(readOnly = true)
    public SellerShell shell(long userId) {
        SellerContext.Seller seller = sellers.current(userId);
        VendorProfile p = profiles.findById(seller.vendorProfileId()).orElseThrow();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("orders_to_confirm", 0L);
        counts.put("unread_chats", 0L);
        for (SellerBadgeContributor c : badges) {
            counts.putAll(c.badgesOf(userId));
        }
        return new SellerShell(shops.view(p).mini(), p.getKycStatus().name(), counts);
    }
}
