package com.genie.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Lấy {@link ClientInfo} từ request (plan AUTH A01.05).
 * IP: chỉ tin {@code X-Forwarded-For} khi kết nối đến từ proxy tin cậy ({@code genie.security.trusted-proxies});
 * duyệt XFF từ phải sang trái, lấy IP đầu tiên không phải proxy tin cậy (Nginx dùng {@code proxy_add_x_forwarded_for}
 * nối IP thật vào cuối — client tự gửi XFF giả chỉ nằm ở bên trái).
 */
@Component
public class ClientInfoResolver implements HandlerMethodArgumentResolver {

    private final List<IpAddressMatcher> trusted;

    public ClientInfoResolver(SecurityProperties props) {
        this.trusted = props.trustedProxies().stream().map(String::trim).filter(s -> !s.isEmpty())
                .map(IpAddressMatcher::new).toList();
    }

    public ClientInfo resolve(HttpServletRequest request) {
        return new ClientInfo(clientIp(request), truncate(request.getHeader("User-Agent"), ClientInfo.MAX_USER_AGENT));
    }

    String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (remote == null || !isTrusted(remote)) {
            return remote;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remote;
        }
        String[] parts = xff.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String ip = parts[i].trim();
            if (!ip.isEmpty() && !isTrusted(ip)) {
                return truncate(ip, 45);
            }
        }
        return truncate(parts[0].trim(), 45);
    }

    private boolean isTrusted(String ip) {
        for (IpAddressMatcher m : trusted) {
            try {
                if (m.matches(ip)) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // chuỗi không phải IP hợp lệ → coi như không tin cậy
            }
        }
        return false;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ===== HandlerMethodArgumentResolver: cho phép khai tham số ClientInfo trong controller =====

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ClientInfo.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return request == null ? ClientInfo.system() : resolve(request);
    }
}
