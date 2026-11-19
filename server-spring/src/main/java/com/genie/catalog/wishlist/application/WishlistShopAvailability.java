package com.genie.catalog.wishlist.application;

import java.util.Collection;
import java.util.Set;

/**
 * Cổng đảo ngược: shop nào đang tạm nghỉ (plan wishlist D3, W03.01) — trạng thái nghỉ thuộc module {@code vendor}, module
 * {@code catalog} không phụ thuộc ngược. Module {@code vendor} cài đặt khi có {@code ShopFacade} (plan Mở shop & KYC); chưa có
 * cài đặt thì dùng {@link WishlistConfig#noVacation()} — coi như không shop nào nghỉ.
 */
public interface WishlistShopAvailability {

    /** Tập {@code vendor_id} (= users.id của chủ shop) đang nghỉ trong số các id đưa vào. */
    Set<Long> vendorIdsOnVacation(Collection<Long> vendorIds);
}
