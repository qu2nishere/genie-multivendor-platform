package com.genie.vendor.shop.application;

/**
 * Cổng đảo ngược: việc cần làm ngay sau khi một shop được tạo (UC-VND-01 bước 4; plan Mở shop & KYC K03.06) — module
 * {@code shipping} tạo cấu hình hãng vận chuyển mặc định (GHN bật), module {@code payment} tạo ví shop (khi hiện thực).
 * Gọi SAU khi transaction mở shop đã commit, mỗi cài đặt trong transaction riêng; lỗi một cài đặt chỉ ghi log, không làm
 * hỏng việc mở shop (cài đặt phải idempotent để chạy bù được).
 */
public interface ShopBootstrapContributor {

    void onShopRegistered(long shopId, long vendorUserId);
}
