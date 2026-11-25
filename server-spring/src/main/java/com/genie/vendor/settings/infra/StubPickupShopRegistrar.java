package com.genie.vendor.settings.infra;

import com.genie.vendor.settings.application.PickupShopRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Giả lập đăng ký kho GHN (plan Mở shop & KYC D12, D16) — mặc định ở máy dev / demo khi không có tài khoản GHN dùng được
 * cho {@code shop/register}. Lần đầu trả số giả ổn định {@code 880000 + vendorProfileId}; đã có Shop ID thì giữ nguyên
 * (như GHN cập nhật địa chỉ của cùng cửa hàng).
 */
@Component
@ConditionalOnProperty(prefix = "genie.shipping.ghn", name = "pickup-mode", havingValue = "stub", matchIfMissing = true)
public class StubPickupShopRegistrar implements PickupShopRegistrar {

    private static final Logger log = LoggerFactory.getLogger(StubPickupShopRegistrar.class);
    static final int BASE_ID = 880_000;

    public StubPickupShopRegistrar() {
        log.info("Kho lấy hàng GHN: dùng bản giả lập (GHN_PICKUP_MODE=stub).");
    }

    @Override
    public int sync(Integer currentGhnShopId, PickupInfo info) {
        if (currentGhnShopId != null) {
            return currentGhnShopId;
        }
        return Math.toIntExact(BASE_ID + info.vendorProfileId());
    }
}
