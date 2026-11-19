package com.genie.vendor.kyc.application;

/**
 * Cổng đảo ngược: báo nhân viên có hồ sơ KYC mới cần xem (AdminNotification loại {@code USER_APPROVAL}; plan Mở shop & KYC
 * K04.09). Module {@code notification} cài đặt khi được hiện thực; chưa có cài đặt → chỉ ghi log.
 */
public interface KycAdminNotifier {

    void kycSubmitted(long vendorProfileId, String shopName);
}
