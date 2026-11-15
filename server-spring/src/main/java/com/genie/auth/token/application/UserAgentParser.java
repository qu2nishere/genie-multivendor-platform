package com.genie.auth.token.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Suy tên thiết bị / loại thiết bị từ User-Agent cho màn "Thiết bị đăng nhập" (API_SPEC 3.6) — đủ dùng cho các trình
 * duyệt phổ biến, không thêm thư viện. Ví dụ: {@code Windows PC · Chrome 126}, {@code iPhone · Safari 17}.
 */
public final class UserAgentParser {

    public enum DeviceType { DESKTOP, MOBILE, TABLET }

    public record Device(String name, DeviceType type) {
    }

    private static final Pattern EDGE = Pattern.compile("Edg(?:e|A|iOS)?/(\\d+)");
    private static final Pattern OPERA = Pattern.compile("(?:OPR|Opera)/(\\d+)");
    private static final Pattern FIREFOX = Pattern.compile("(?:Firefox|FxiOS)/(\\d+)");
    private static final Pattern CHROME = Pattern.compile("(?:Chrome|CriOS)/(\\d+)");
    private static final Pattern SAFARI = Pattern.compile("Version/(\\d+).*Safari/");

    private UserAgentParser() {
    }

    public static Device parse(String userAgent) {
        String ua = userAgent == null ? "" : userAgent;
        DeviceType type = deviceType(ua);
        String platform = platform(ua, type);
        String browser = browser(ua);
        String name = browser == null ? platform : platform + " · " + browser;
        return new Device(name, type);
    }

    private static DeviceType deviceType(String ua) {
        if (ua.contains("iPad") || ua.contains("Tablet") || (ua.contains("Android") && !ua.contains("Mobile"))) {
            return DeviceType.TABLET;
        }
        if (ua.contains("iPhone") || ua.contains("Mobile") || ua.contains("Android")) {
            return DeviceType.MOBILE;
        }
        return DeviceType.DESKTOP;
    }

    private static String platform(String ua, DeviceType type) {
        if (ua.contains("iPhone")) {
            return "iPhone";
        }
        if (ua.contains("iPad")) {
            return "iPad";
        }
        if (ua.contains("Android")) {
            return type == DeviceType.TABLET ? "Máy tính bảng Android" : "Điện thoại Android";
        }
        if (ua.contains("Windows")) {
            return "Windows PC";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "Mac";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "Thiết bị không xác định";
    }

    private static String browser(String ua) {
        String v;
        if ((v = version(EDGE, ua)) != null) {
            return "Edge " + v;
        }
        if ((v = version(OPERA, ua)) != null) {
            return "Opera " + v;
        }
        if ((v = version(FIREFOX, ua)) != null) {
            return "Firefox " + v;
        }
        if ((v = version(CHROME, ua)) != null) {
            return "Chrome " + v;
        }
        if ((v = version(SAFARI, ua)) != null) {
            return "Safari " + v;
        }
        return null;
    }

    private static String version(Pattern p, String ua) {
        Matcher m = p.matcher(ua);
        return m.find() ? m.group(1) : null;
    }
}
