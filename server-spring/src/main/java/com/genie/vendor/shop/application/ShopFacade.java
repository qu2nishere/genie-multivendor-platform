package com.genie.vendor.shop.application;

import com.genie.common.storage.ObjectStorage;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cửa công khai của module {@code vendor} cho module khác (README plan 2026-11-16 mục 3; plan Mở shop & KYC N2, K02.07).
 * Chỉ trả record, không trả entity.
 * <p><b>Hai loại id:</b> {@code shopId} = {@code vendor_profiles.id} (id trên API — D1); {@code vendorUserId} = {@code users.id}
 * của chủ shop (cột {@code vendor_id} ở bảng nghiệp vụ: sản phẩm, đơn, theo dõi…). Đổi qua {@link #vendorUserIdOf} /
 * {@link #shopIdOf}.
 */
@Component
public class ShopFacade {

    /** Bucket của ảnh shop lưu dạng khóa tương đối (dữ liệu mẫu); ảnh tải lên qua SHOP_LOGO / SHOP_COVER lưu URL đầy đủ. */
    static final String LEGACY_SHOP_MEDIA = "content-assets";

    private final VendorProfileRepository profiles;
    private final ObjectStorage storage;

    public ShopFacade(VendorProfileRepository profiles, ObjectStorage storage) {
        this.profiles = profiles;
        this.storage = storage;
    }

    /** {@code $ShopMini} (API_SPEC 1): id, tên, logo, huy hiệu Chính hãng. */
    public record ShopMiniView(long id, String name, String logoUrl, boolean isMall) {
    }

    /** Thông tin shop cho module khác — kèm id chủ shop và trạng thái KYC. */
    public record ShopView(long id, long vendorUserId, String name, String logoUrl, boolean isMall, String kycStatus) {

        public ShopMiniView mini() {
            return new ShopMiniView(id, name, logoUrl, isMall);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ShopMiniView> findMini(long shopId) {
        return profiles.findById(shopId).map(p -> view(p).mini());
    }

    @Transactional(readOnly = true)
    public Optional<ShopView> findByVendorUserId(long vendorUserId) {
        return profiles.findByUserId(vendorUserId).map(this::view);
    }

    @Transactional(readOnly = true)
    public Optional<Long> vendorUserIdOf(long shopId) {
        return profiles.findById(shopId).map(VendorProfile::getUserId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> shopIdOf(long vendorUserId) {
        return profiles.findByUserId(vendorUserId).map(VendorProfile::getId);
    }

    @Transactional(readOnly = true)
    public boolean isVerified(long vendorUserId) {
        return profiles.findByUserId(vendorUserId).map(p -> p.getKycStatus() == KycStatus.VERIFIED).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isOnVacation(long vendorUserId, OffsetDateTime now) {
        return profiles.findByUserId(vendorUserId).map(p -> p.isOnVacation(now)).orElse(false);
    }

    /** Hoa hồng áp dụng: thỏa thuận riêng → ngành hàng → 5% (UC-PAY-05). Không có shop → theo ngành / mặc định. */
    @Transactional(readOnly = true)
    public BigDecimal effectiveCommissionRate(long vendorUserId, BigDecimal categoryRate) {
        return profiles.findByUserId(vendorUserId)
                .map(p -> p.effectiveCommissionRate(categoryRate))
                .orElseGet(() -> categoryRate != null ? categoryRate : new BigDecimal("0.0500"));
    }

    /** Shop đã xác minh theo tên — ô chọn shop của Admin (API 11.16; K10.04). */
    @Transactional(readOnly = true)
    public List<ShopMiniView> searchVerified(String q, int limit) {
        String term = q == null ? "" : q.trim();
        return profiles.searchVerified(term, PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream().map(p -> view(p).mini()).toList();
    }

    ShopView view(VendorProfile p) {
        return new ShopView(p.getId(), p.getUserId(), p.getShopName(), mediaUrl(p.getLogoUrl()),
                Boolean.TRUE.equals(p.getIsMall()), p.getKycStatus().name());
    }

    /** URL ảnh shop: giá trị đã là URL (ảnh tải lên) giữ nguyên; khóa tương đối (dữ liệu mẫu) ghép URL công khai. */
    public String mediaUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.startsWith("http://") || value.startsWith("https://")
                ? value : storage.publicUrl(LEGACY_SHOP_MEDIA, value);
    }
}
