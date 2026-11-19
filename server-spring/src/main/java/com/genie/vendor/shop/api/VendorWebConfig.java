package com.genie.vendor.shop.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Gắn cổng KYC cho {@code /api/v1/seller/**} (API_SPEC 0.9; plan Mở shop & KYC K02.06). */
@Configuration
public class VendorWebConfig implements WebMvcConfigurer {

    private final KycGateInterceptor kycGate;

    public VendorWebConfig(KycGateInterceptor kycGate) {
        this.kycGate = kycGate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(kycGate).addPathPatterns("/api/v1/seller/**");
    }
}
