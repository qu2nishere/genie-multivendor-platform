// Tài khoản nội bộ (STAFF / ADMIN) không được mua hàng (plan B1 D16 — chốt lại 2026-11-20, hướng B; API_SPEC 0.1, 6).
package com.genie.common.security;

import com.genie.common.api.ErrorResponseFactory;
import com.genie.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Chặn tài khoản nội bộ ở mọi đường dẫn mua hàng — giỏ hàng {@code /me/cart/**} và đặt hàng / thanh toán
 * {@code /me/checkout/**}, {@code /me/checkout-sessions/**} (API_SPEC 6.1–6.13) → {@code 403 GNE-ORD-4031}.
 * Đặt ở tầng bảo mật theo đường dẫn (sau {@link JwtAuthFilter}) để luật tự áp dụng cho mọi endpoint mua hàng, kể cả
 * endpoint module {@code order} hiện thực sau này. Nhân viên mua sắm bằng tài khoản khách hàng cá nhân (email riêng).
 */
public class BuyerOnlyFilter extends OncePerRequestFilter {

    /** Tiền tố đường dẫn mua hàng ({@code /me/checkout} phủ cả {@code /me/checkout-sessions}). */
    static final List<String> PURCHASE_PREFIXES = List.of("/api/v1/me/cart", "/api/v1/me/checkout");

    private final ErrorResponseFactory errors;

    public BuyerOnlyFilter(ErrorResponseFactory errors) {
        this.errors = errors;
    }

    static boolean isPurchasePath(String path) {
        if (path == null) return false;
        for (String prefix : PURCHASE_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/") || path.startsWith(prefix + "-")) return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !isPurchasePath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user && user.isStaffOrAdmin()) {
            errors.write(request, response, ErrorCode.INTERNAL_ACCOUNT_CANNOT_BUY, null, null);
            return;
        }
        chain.doFilter(request, response);
    }
}
