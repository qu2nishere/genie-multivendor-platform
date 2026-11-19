package com.genie.vendor.shop.event;

/**
 * Shop vừa được duyệt KYC — AI tự duyệt hoặc nhân viên duyệt (UC-VND-03, UC-VND-04; plan Mở shop & KYC K05.08, K06.05,
 * K10.03). Phát trong transaction đổi trạng thái, người nghe dùng {@code AFTER_COMMIT}.
 * <ul>
 *   <li>{@code catalog} (khi hiện thực) — mở chức năng đăng bán;</li>
 *   <li>{@code notification} (khi hiện thực) — thông báo in-app "Hồ sơ KYC đã được duyệt".</li>
 * </ul>
 *
 * @param shopId       {@code vendor_profiles.id}
 * @param vendorUserId {@code users.id} của chủ shop
 */
public record ShopVerifiedEvent(long shopId, long vendorUserId) {
}
