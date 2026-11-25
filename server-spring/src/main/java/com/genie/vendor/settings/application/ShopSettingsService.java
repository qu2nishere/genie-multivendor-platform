package com.genie.vendor.settings.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import com.genie.vendor.shop.application.SellerContext;
import com.genie.vendor.shop.application.ShopFacade;
import com.genie.vendor.shop.application.ShopNamePolicy;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cài đặt gian hàng — UC-VND-21 (API_SPEC 10.11; plan Mở shop & KYC K09.03–K09.06). Mở cả khi shop chưa VERIFIED. Một lời
 * gọi {@code GET} trả đủ các khối; {@code PATCH} gửi khối nào cập nhật khối đó. Khối {@code auto_reply} (UC-VND-06) và
 * {@code vacation} (UC-VND-19, chỉ đọc) chỉ lưu / trả dữ liệu (D14).
 */
@Service
public class ShopSettingsService {

    private final VendorProfileRepository profiles;
    private final SellerContext sellers;
    private final ShopNamePolicy names;
    private final ShopFacade shops;
    private final UploadService uploads;
    private final ShopShippingSettings shipping;
    private final PayoutAccountService payouts;
    private final Clock clock;

    public ShopSettingsService(VendorProfileRepository profiles, SellerContext sellers, ShopNamePolicy names, ShopFacade shops,
                               UploadService uploads, ShopShippingSettings shipping, PayoutAccountService payouts, Clock clock) {
        this.profiles = profiles;
        this.sellers = sellers;
        this.names = names;
        this.shops = shops;
        this.uploads = uploads;
        this.shipping = shipping;
        this.payouts = payouts;
        this.clock = clock;
    }

    public record Profile(String shopName, String description, String logoUrl, String coverUrl,
                          OffsetDateTime nextRenameAllowedAt) {
    }

    public record Region(Integer id, String name) {
    }

    public record Ward(String code, String name) {
    }

    /** Khối {@code pickup_address} (API 10.11) — địa giới 2 cấp Tỉnh → Phường, không còn quận (plan D16). */
    public record PickupAddress(String phoneNumber, Region province, Ward ward, String addressLine, Integer ghnShopId) {
    }

    public record AutoReply(Boolean enabled, String greeting, String awayMessage) {
    }

    public record Vacation(OffsetDateTime endsAt, String message) {
    }

    public record Settings(Profile profile, ShopShippingSettings.Settings shipping, PickupAddress pickupAddress,
                           PayoutAccountService.PayoutAccountView payoutAccount, AutoReply autoReply,
                           boolean preOrderEnabled, Vacation vacation) {
    }

    /** Khối profile của PATCH — trường null = giữ nguyên; {@code description = ""} để xóa mô tả. */
    public record ProfilePatch(String shopName, String description, String logoObjectKey, String coverObjectKey) {
    }

    public record Patch(ProfilePatch profile, ShopShippingSettings.Settings shipping, AutoReply autoReply,
                        Boolean preOrderEnabled) {
    }

    @Transactional(readOnly = true)
    public Settings get(long userId) {
        VendorProfile p = profiles.findById(sellers.current(userId).vendorProfileId()).orElseThrow();
        return view(p);
    }

    @Transactional
    public Settings patch(long userId, Patch patch) {
        VendorProfile p = profiles.findByIdForUpdate(sellers.current(userId).vendorProfileId()).orElseThrow();
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (patch.profile() != null) {
            applyProfile(userId, p, patch.profile(), now);
        }
        if (patch.shipping() != null) {
            ShopShippingSettings.Settings s = patch.shipping();
            if (s.carriers() == null || s.carriers().values().stream().noneMatch(Boolean.TRUE::equals)) {
                throw new BusinessException(ErrorCode.CARRIER_REQUIRED);
            }
            BigDecimal t = s.freeShippingThreshold();
            if (s.freeShippingEnabled() && (t == null || t.signum() < 0)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ngưỡng miễn phí vận chuyển phải ≥ 0.",
                        Map.of("field", "shipping.free_shipping_threshold"));
            }
            shipping.save(p.getUserId(), s);
        }
        if (patch.autoReply() != null) {
            AutoReply a = patch.autoReply();
            try {
                p.configureAutoReply(Boolean.TRUE.equals(a.enabled()), a.greeting(), a.awayMessage());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.AUTO_REPLY_EMPTY);
            }
        }
        if (patch.preOrderEnabled() != null) {
            p.setPreOrderEnabled(patch.preOrderEnabled());
        }
        return view(p);
    }

    private void applyProfile(long userId, VendorProfile p, ProfilePatch in, OffsetDateTime now) {
        if (in.shopName() != null && !ShopNamePolicy.normalize(in.shopName()).equals(p.getShopName())) {
            OffsetDateTime next = p.nextRenameAllowedAt(now);
            if (next != null) {
                throw new BusinessException(ErrorCode.SHOP_RENAME_TOO_SOON, Map.of("next_rename_allowed_at", next.toString()));
            }
            p.renameShop(names.validate(in.shopName(), p.getId()), now);
        }
        String description = in.description() == null ? p.getShopDescription()
                : in.description().isBlank() ? null : in.description().trim();
        if (description != null && description.length() > 2000) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mô tả tối đa 2000 ký tự.",
                    Map.of("field", "profile.description"));
        }
        List<Runnable> afterCommit = new ArrayList<>();
        String logo = replaceImage(userId, p, UploadPurpose.SHOP_LOGO, "logo", in.logoObjectKey(), p.getLogoUrl(), afterCommit);
        String cover = replaceImage(userId, p, UploadPurpose.SHOP_COVER, "cover", in.coverObjectKey(), p.getCoverUrl(),
                afterCommit);
        p.updateBranding(description, logo, cover);
        if (!afterCommit.isEmpty() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    afterCommit.forEach(Runnable::run);
                }
            });
        }
    }

    /** Ảnh mới: chuyển {@code tmp/} → {@code vendor/{shopId}/{logo|cover}/…}, xóa ảnh cũ của chính kho sau commit. */
    private String replaceImage(long userId, VendorProfile p, UploadPurpose purpose, String slot, String tmpKey, String current,
                                List<Runnable> afterCommit) {
        if (tmpKey == null || tmpKey.isBlank()) {
            return current;
        }
        UploadService.StoredFile f = uploads.promoteTo(userId, purpose, tmpKey, "vendor/" + p.getId() + "/" + slot + "/");
        uploads.keyOfPublicUrl(purpose, current).ifPresent(old -> afterCommit.add(() -> uploads.deleteQuietly(purpose, old)));
        return f.publicUrl();
    }

    /** Kho lấy hàng đang lưu; chưa cấu hình → {@code null}. */
    static PickupAddress pickupOf(VendorProfile p) {
        return p.getPickupAddressLine() == null ? null
                : new PickupAddress(p.getPickupPhone(), new Region(p.getPickupProvinceId(), p.getPickupProvince()),
                new Ward(p.getPickupWardCode(), p.getPickupWard()), p.getPickupAddressLine(), p.getGhnShopId());
    }

    private Settings view(VendorProfile p) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        PickupAddress pickup = pickupOf(p);
        Vacation vacation = p.isOnVacation(now) || (p.getVacationStartsAt() != null && p.getVacationStartsAt().isAfter(now))
                ? new Vacation(p.getVacationEndsAt(), p.getVacationMessage()) : null;
        return new Settings(
                new Profile(p.getShopName(), p.getShopDescription(), shops.mediaUrl(p.getLogoUrl()), shops.mediaUrl(p.getCoverUrl()),
                        p.nextRenameAllowedAt(now)),
                shipping.get(p.getUserId()).orElseGet(() -> new ShopShippingSettings.Settings(
                        Map.of("GHN", true, "GHTK", true, "VIETTEL_POST", false, "JNT_EXPRESS", false, "GENIE_EXPRESS", false),
                        false, null)),
                pickup, payouts.view(p),
                new AutoReply(Boolean.TRUE.equals(p.getAutoReplyEnabled()), p.getAutoReplyGreeting(), p.getAutoReplyOffline()),
                Boolean.TRUE.equals(p.getPreOrderEnabled()), vacation);
    }
}
