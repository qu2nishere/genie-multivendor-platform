package com.genie.admin.banner.application;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Liên kết đích của banner chỉ được trỏ trong miền sàn — chống chuyển hướng mở (UC-ADM-12 ngoại lệ; plan banner N4,
 * B02.05). Hàm thuần: nhận chuỗi, trả hợp lệ / không. Kiểm cả chuỗi gốc lẫn chuỗi đã giải mã %xx (chặn {@code /%2F%2Fevil}).
 */
public final class BannerTargetValidator {

    public static final int MAX_LENGTH = 500;

    private BannerTargetValidator() {
    }

    public static boolean isAllowed(String target) {
        if (target == null || target.isEmpty() || target.length() > MAX_LENGTH) {
            return false;
        }
        if (!safePath(target, false)) {
            return false;
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(target.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return safePath(decoded, true);   // %20 trong query là hợp lệ; vẫn chặn ký tự điều khiển
    }

    private static boolean safePath(String s, boolean decoded) {
        if (!s.startsWith("/") || s.startsWith("//") || s.indexOf('\\') >= 0 || s.contains("://")) {
            return false;
        }
        if (s.regionMatches(true, 0, "/api/", 0, 5)) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isISOControl(c) || (!decoded && Character.isWhitespace(c))) {
                return false;
            }
        }
        return true;
    }
}
