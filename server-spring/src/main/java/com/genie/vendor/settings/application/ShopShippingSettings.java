package com.genie.vendor.settings.application;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Cổng đảo ngược: cấu hình hãng vận chuyển của shop (bảng {@code vendor_shipping_config} thuộc module {@code shipping}) cho
 * màn Cài đặt (API 10.11; plan Mở shop & KYC K09.01). Module {@code shipping} cài đặt ({@code VendorShippingSettingsAdapter}).
 */
public interface ShopShippingSettings {

    /**
     * @param carriers              {@code GHN, GHTK, VIETTEL_POST, JNT_EXPRESS, GENIE_EXPRESS} → bật / tắt
     * @param freeShippingEnabled   có miễn phí vận chuyển theo ngưỡng
     * @param freeShippingThreshold ngưỡng đơn tối thiểu (VND); null khi tắt
     */
    record Settings(Map<String, Boolean> carriers, boolean freeShippingEnabled, BigDecimal freeShippingThreshold) {
    }

    Optional<Settings> get(long vendorUserId);

    /** Lưu (tạo nếu chưa có); trả cấu hình sau khi lưu. */
    Settings save(long vendorUserId, Settings settings);

    /** Tạo cấu hình mặc định (GHN, GHTK bật) nếu chưa có — idempotent. */
    void createDefault(long vendorUserId);
}
