package com.genie.auth.login.application;

import com.genie.auth.token.application.SessionService;
import com.genie.auth.token.application.UserAgentParser;
import com.genie.auth.token.domain.RefreshToken;
import com.genie.common.security.ClientInfo;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Nhận diện "thiết bị lạ" (UC-AUTH-02 luồng 8a, plan AUTH A07.03): trong 90 ngày gần nhất chưa có lần đăng nhập nào
 * cùng loại thiết bị + trình duyệt (từ User-Agent) <b>và</b> cùng dải IP /24 (IPv6: 4 nhóm đầu).
 * Lần đăng nhập đầu tiên của tài khoản (chưa có lịch sử) không tính là thiết bị lạ.
 */
@Component
public class NewDeviceDetector {

    private static final Duration HISTORY = Duration.ofDays(90);

    private final SessionService sessions;

    public NewDeviceDetector(SessionService sessions) {
        this.sessions = sessions;
    }

    /** Gọi TRƯỚC khi mở phiên mới. */
    public boolean isNewDevice(long userId, ClientInfo client) {
        List<RefreshToken> history = sessions.recentLogins(userId, HISTORY);
        if (history.isEmpty()) {
            return false;
        }
        String device = UserAgentParser.parse(client == null ? null : client.userAgent()).name();
        String network = network(client == null ? null : client.ip());
        for (RefreshToken t : history) {
            if (UserAgentParser.parse(t.getDeviceInfo()).name().equals(device) && network(t.getIpAddress()).equals(network)) {
                return false;
            }
        }
        return true;
    }

    static String network(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        if (ip.contains(":")) {
            String[] groups = ip.split(":");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(4, groups.length); i++) {
                sb.append(groups[i]).append(':');
            }
            return sb.toString();
        }
        int last = ip.lastIndexOf('.');
        return last > 0 ? ip.substring(0, last) : ip;
    }
}
