package com.genie.auth.profile.application;

import java.util.Optional;

/**
 * Cổng "shop của tài khoản" cho {@code GET /me} (API_SPEC 3.1, plan AUTH D13, A13.01). Module {@code vendor} cài đặt
 * (đọc {@code vendor_profiles}); {@code auth} không phụ thuộc {@code vendor}. Chưa có cài đặt (module vendor chưa làm)
 * → {@code shop = null}, web ẩn mục "Chuyển sang Kênh người bán".
 */
public interface AccountShopProvider {

    /** Shop do người này sở hữu; chưa mở shop → rỗng. */
    Optional<ShopSummary> shopOf(long userId);

    /**
     * @param kycStatus {@code NOT_SUBMITTED | PENDING | AI_PROCESSING | MANUAL_REVIEW | VERIFIED | REJECTED |
     *                  SUPPLEMENT_REQUESTED} (cột {@code vendor_profiles.kyc_status})
     */
    record ShopSummary(long id, String name, String logoUrl, String kycStatus) {
    }
}
