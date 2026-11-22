package com.genie.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * ETag cho mọi {@code GET /api/v1/public/**} (API_SPEC 0.12, plan 2026-11-17 C01.03):
 * {@code ETag} = băm thân phản hồi; request gửi lại {@code If-None-Match} trùng → {@code 304} không thân.
 * Thời gian cache ({@code Cache-Control: public, max-age=…}) do từng controller đặt theo bảng API_SPEC 0.12.
 */
@Configuration(proxyBeanMethods = false)
public class PublicHttpCacheConfig {

    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> publicEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> reg = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        reg.addUrlPatterns("/api/v1/public/*");
        reg.setName("publicEtagFilter");
        return reg;
    }
}
