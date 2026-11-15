package com.genie.common.security;

import com.genie.common.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Đọc {@code Authorization: Bearer …} → kiểm chữ ký, hạn, denylist / mốc thu hồi → đặt {@link AuthUser} vào SecurityContext
 * (plan AUTH A04.03). Token sai / bị thu hồi: KHÔNG chặn ở đây — request đi tiếp như khách; nếu endpoint cần đăng nhập
 * thì {@code RestAuthEntryPoint} trả {@code 401 GNE-SEC-4010}.
 * <p>Authority: {@code ROLE_<role>} + vai trò được kế thừa (thay cho RoleHierarchy: VENDOR ⊃ CUSTOMER, ADMIN ⊃ STAFF —
 * ENTITY 1.1 a1) + mã quyền từ {@link AuthorityProvider}.
 * <p>Không khai báo là bean (để Spring Boot không tự đăng ký thành servlet filter lần hai) — {@code SecurityConfig} tạo.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Thuộc tính request đánh dấu lý do token không dùng được — entry point đọc để log. */
    public static final String ERROR_ATTRIBUTE = "genie.auth.error";

    private final JwtService jwtService;
    private final TokenRevocationStore revocations;
    private final List<AuthorityProvider> authorityProviders;

    public JwtAuthFilter(JwtService jwtService, TokenRevocationStore revocations, List<AuthorityProvider> authorityProviders) {
        this.jwtService = jwtService;
        this.revocations = revocations;
        this.authorityProviders = authorityProviders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            try {
                AuthUser user = jwtService.parse(token);
                if (revocations.isRevoked(user)) {
                    request.setAttribute(ERROR_ATTRIBUTE, "revoked");
                } else {
                    var auth = new UsernamePasswordAuthenticationToken(user, null, authoritiesOf(user));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtService.InvalidAccessTokenException e) {
                request.setAttribute(ERROR_ATTRIBUTE, "invalid");
            }
        }
        chain.doFilter(request, response);
    }

    List<GrantedAuthority> authoritiesOf(AuthUser user) {
        List<GrantedAuthority> result = new ArrayList<>();
        UserRole role = user.role();
        result.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (role == UserRole.VENDOR) {
            result.add(new SimpleGrantedAuthority("ROLE_" + UserRole.CUSTOMER.name()));
        }
        if (role == UserRole.ADMIN) {
            result.add(new SimpleGrantedAuthority("ROLE_" + UserRole.STAFF.name()));
        }
        if (role == UserRole.STAFF || role == UserRole.ADMIN) {
            for (AuthorityProvider provider : authorityProviders) {
                provider.permissionsOf(user.id(), role).forEach(p -> result.add(new SimpleGrantedAuthority(p)));
            }
        }
        return result;
    }
}
