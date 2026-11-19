package com.genie.vendor.kyc.event;

/**
 * Người bán vừa nộp / nộp lại hồ sơ KYC (UC-VND-02; plan Mở shop & KYC K04.07). Người nghe ({@code AFTER_COMMIT}):
 * gửi hồ sơ cho AI Worker (UC-VND-03, K05.06) và báo nhân viên có hồ sơ mới (K04.09).
 *
 * @param vendorProfileId {@code vendor_profiles.id}
 * @param vendorUserId    {@code users.id} của chủ shop
 */
public record KycSubmittedEvent(long vendorProfileId, long vendorUserId) {
}
