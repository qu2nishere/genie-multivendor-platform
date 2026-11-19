package com.genie.vendor.shop.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.vendor.shop.domain.KycStatus;
import com.genie.vendor.shop.domain.VendorProfile;
import com.genie.vendor.shop.domain.VendorProfileRepository;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra shop của NGƯỜI ĐANG GỌI (plan Mở shop & KYC K02.05, N10): mọi API {@code /seller/**} lấy shop theo {@code userId} trong
 * token — không bao giờ nhận {@code shop_id} từ client (chống IDOR, TS 9). Tài khoản không có shop → {@code 403 GNE-SEC-4030}.
 */
@Component
public class SellerContext {

    private final VendorProfileRepository profiles;

    public SellerContext(VendorProfileRepository profiles) {
        this.profiles = profiles;
    }

    /** Shop của người gọi — chỉ các khóa cần cho cổng KYC / phân quyền. */
    public record Seller(long vendorProfileId, long vendorUserId, KycStatus kycStatus) {

        public boolean isVerified() {
            return kycStatus == KycStatus.VERIFIED;
        }
    }

    @Transactional(readOnly = true)
    public Seller current(long userId) {
        VendorProfile p = profiles.findByUserId(userId).orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        return new Seller(p.getId(), p.getUserId(), p.getKycStatus());
    }

    /** Như {@link #current} nhưng bắt buộc shop đã {@code VERIFIED} → khác thì {@code 403 GNE-VND-4031 {kyc_status}}. */
    @Transactional(readOnly = true)
    public Seller requireVerified(long userId) {
        Seller seller = current(userId);
        if (!seller.isVerified()) {
            throw notVerified(seller.kycStatus());
        }
        return seller;
    }

    public static BusinessException notVerified(KycStatus status) {
        return new BusinessException(ErrorCode.SHOP_NOT_VERIFIED, Map.of("kyc_status", status.name()));
    }
}
