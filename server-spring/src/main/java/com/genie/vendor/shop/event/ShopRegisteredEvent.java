package com.genie.vendor.shop.event;

/**
 * Một tài khoản vừa mở shop (UC-VND-01; plan Mở shop & KYC K03.03, K10.03) — phát trong transaction tạo
 * {@code vendor_profiles}, người nghe dùng {@code AFTER_COMMIT}.
 * <ul>
 *   <li>{@code vendor} — tạo cấu hình vận chuyển mặc định / ví shop qua cổng {@code ShopBootstrapContributor};</li>
 *   <li>{@code notification} (khi hiện thực) — thông báo "Chào mừng người bán mới".</li>
 * </ul>
 *
 * @param shopId       {@code vendor_profiles.id}
 * @param vendorUserId {@code users.id} của chủ shop
 */
public record ShopRegisteredEvent(long shopId, long vendorUserId) {
}
