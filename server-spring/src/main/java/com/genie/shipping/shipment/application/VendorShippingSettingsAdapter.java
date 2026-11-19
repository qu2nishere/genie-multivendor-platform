package com.genie.shipping.shipment.application;

import com.genie.shipping.shipment.domain.VendorShippingConfig;
import com.genie.shipping.shipment.domain.VendorShippingConfigRepository;
import com.genie.vendor.settings.application.ShopShippingSettings;
import com.genie.vendor.shop.application.ShopBootstrapContributor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module {@code shipping} cài cổng {@link ShopShippingSettings} của {@code vendor} trên {@code vendor_shipping_config}
 * (plan Mở shop & KYC K09.01–K09.02) và tạo cấu hình mặc định ngay khi mở shop ({@link ShopBootstrapContributor} — K03.06).
 * {@code free_shipping_enabled} không có cột riêng: bật = có ngưỡng.
 */
@Component
public class VendorShippingSettingsAdapter implements ShopShippingSettings, ShopBootstrapContributor {

    private final VendorShippingConfigRepository configs;

    public VendorShippingSettingsAdapter(VendorShippingConfigRepository configs) {
        this.configs = configs;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Settings> get(long vendorUserId) {
        return configs.findByVendorId(vendorUserId).map(VendorShippingSettingsAdapter::view);
    }

    @Override
    @Transactional
    public Settings save(long vendorUserId, Settings s) {
        VendorShippingConfig c = configs.findByVendorId(vendorUserId)
                .orElseGet(() -> VendorShippingConfig.builder().vendorId(vendorUserId).build());
        Map<String, Boolean> in = s.carriers() == null ? Map.of() : s.carriers();
        c.setGhnEnabled(in.getOrDefault("GHN", c.getGhnEnabled()));
        c.setGhtkEnabled(in.getOrDefault("GHTK", c.getGhtkEnabled()));
        c.setVtpEnabled(in.getOrDefault("VIETTEL_POST", c.getVtpEnabled()));
        c.setJtEnabled(in.getOrDefault("JNT_EXPRESS", c.getJtEnabled()));
        c.setGenieExpressEnabled(in.getOrDefault("GENIE_EXPRESS", c.getGenieExpressEnabled()));
        c.setFreeshipThreshold(s.freeShippingEnabled() ? s.freeShippingThreshold() : null);
        return view(configs.save(c));
    }

    @Override
    @Transactional
    public void createDefault(long vendorUserId) {
        if (configs.findByVendorId(vendorUserId).isEmpty()) {
            configs.save(VendorShippingConfig.builder().vendorId(vendorUserId).build());
        }
    }

    @Override
    public void onShopRegistered(long shopId, long vendorUserId) {
        createDefault(vendorUserId);
    }

    private static Settings view(VendorShippingConfig c) {
        Map<String, Boolean> carriers = new LinkedHashMap<>();
        carriers.put("GHN", Boolean.TRUE.equals(c.getGhnEnabled()));
        carriers.put("GHTK", Boolean.TRUE.equals(c.getGhtkEnabled()));
        carriers.put("VIETTEL_POST", Boolean.TRUE.equals(c.getVtpEnabled()));
        carriers.put("JNT_EXPRESS", Boolean.TRUE.equals(c.getJtEnabled()));
        carriers.put("GENIE_EXPRESS", Boolean.TRUE.equals(c.getGenieExpressEnabled()));
        return new Settings(carriers, c.getFreeshipThreshold() != null, c.getFreeshipThreshold());
    }
}
