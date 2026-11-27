// Cài cổng ChatShopDirectory của module social (plan 2026-11-26 chat M02.03, N2, D5, D11, D16)
package com.genie.vendor.shop.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.social.chat.application.ChatShopDirectory;
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
 * Đọc {@code vendor_profiles} (+ trạng thái chủ shop qua {@link UserFacade}) cho chat. Logo dựng qua
 * {@link ShopFacade#mediaUrl} — cùng luật URL với mọi nơi khác. Shop "hoạt động" = {@code VERIFIED} + chủ {@code ACTIVE}.
 * Cấu hình tự trả lời đọc từ khối {@code auto_reply} của {@code PATCH /seller/settings} (UC-VND-06 — B4).
 */
@Component
public class ChatShopDirectoryImpl implements ChatShopDirectory {

    private final VendorProfileRepository profiles;
    private final UserFacade users;
    private final ShopFacade shops;

    public ChatShopDirectoryImpl(VendorProfileRepository profiles, UserFacade users, ShopFacade shops) {
        this.profiles = profiles;
        this.users = users;
        this.shops = shops;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatShop> find(long shopId) {
        return profiles.findById(shopId).map(p -> toShop(p, users.isActive(p.getUserId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ChatShop> byVendorUsers(Collection<Long> vendorUserIds) {
        if (vendorUserIds.isEmpty()) {
            return Map.of();
        }
        List<VendorProfile> rows = profiles.findByUserIdIn(vendorUserIds);
        Set<Long> active = users.activeAmong(rows.stream().map(VendorProfile::getUserId).toList());
        Map<Long, ChatShop> out = new HashMap<>();
        for (VendorProfile p : rows) {
            out.put(p.getUserId(), toShop(p, active.contains(p.getUserId())));
        }
        return out;
    }

    private ChatShop toShop(VendorProfile p, boolean ownerActive) {
        return new ChatShop(p.getId(), p.getUserId(), p.getShopName(), shops.mediaUrl(p.getLogoUrl()),
                Boolean.TRUE.equals(p.getIsMall()), p.getKycStatus() == KycStatus.VERIFIED && ownerActive,
                Boolean.TRUE.equals(p.getAutoReplyEnabled()), p.getAutoReplyGreeting(), p.getAutoReplyOffline(),
                p.getVacationStartsAt(), p.getVacationEndsAt(), p.getVacationMessage(), p.getChatResponseRate());
    }
}
