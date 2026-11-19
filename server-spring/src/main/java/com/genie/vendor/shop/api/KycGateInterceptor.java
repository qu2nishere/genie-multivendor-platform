package com.genie.vendor.shop.api;

import com.genie.common.security.AuthUser;
import com.genie.vendor.shop.application.SellerContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Cổng KYC của Seller Center (API_SPEC 0.9; plan Mở shop & KYC N9, K02.06): mọi {@code /api/v1/seller/**} trả
 * {@code 403 GNE-VND-4031 {kyc_status}} khi shop chưa {@code VERIFIED}, trừ các đường dẫn trong {@link #OPEN}. Spring Security
 * đã chặn người không phải VENDOR trước khi tới đây; tài khoản VENDOR không có shop → {@code 403 GNE-SEC-4030}.
 */
@Component
public class KycGateInterceptor implements HandlerInterceptor {

    /** Phương thức + mẫu đường dẫn được mở khi chưa VERIFIED — đúng danh sách API_SPEC 0.9 (+ 10.1b tìm nhanh trả rỗng). */
    record Open(HttpMethod method, String pattern) {
    }

    static final List<Open> OPEN = List.of(
            new Open(HttpMethod.GET, "/api/v1/seller/me"),                  // khung Seller Center (10.1)
            new Open(null, "/api/v1/seller/kyc/**"),                        // hồ sơ KYC (10.12)
            new Open(null, "/api/v1/seller/kyc"),
            new Open(HttpMethod.GET, "/api/v1/seller/settings"),            // cài đặt (10.11)
            new Open(HttpMethod.PATCH, "/api/v1/seller/settings"),
            new Open(HttpMethod.PUT, "/api/v1/seller/pickup-address"),      // UC-VND-05 tự kiểm VERIFIED ở service
            new Open(null, "/api/v1/seller/payout-account/**"),             // UC-VND-07 tự kiểm VERIFIED ở service
            new Open(null, "/api/v1/seller/payout-account"),
            new Open(null, "/api/v1/seller/vacation/**"),                   // nhóm chế độ nghỉ (UC-VND-19)
            new Open(null, "/api/v1/seller/vacation"),
            new Open(HttpMethod.GET, "/api/v1/seller/search"));             // 10.1b — trả rỗng khi chưa VERIFIED

    private static final AntPathMatcher PATHS = new AntPathMatcher();

    private final SellerContext sellers;

    public KycGateInterceptor(SellerContext sellers) {
        this.sellers = sellers;
    }

    static boolean isOpen(String method, String path) {
        for (Open o : OPEN) {
            if ((o.method() == null || o.method().matches(method)) && PATHS.match(o.pattern(), path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (isOpen(request.getMethod(), path)) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser me)) {
            return true;   // Spring Security đã trả 401 / 403 trước đó
        }
        sellers.requireVerified(me.id());
        return true;
    }
}
