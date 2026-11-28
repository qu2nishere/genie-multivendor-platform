// Ai đang có kết nối WebSocket (API_SPEC 13; plan 2026-11-26 thông báo D4, N02.06; chat D11, D13)
package com.genie.common.realtime;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * Bọc {@link SimpUserRegistry} của simple broker: tên người dùng của phiên STOMP là {@code userId}
 * ({@code StompAuthInterceptor.StompUser}). TLCN chạy một instance nên registry trong bộ nhớ là đủ (DATN nhiều instance
 * thì thay bằng registry dùng chung). Dùng để: đẩy thông báo sàn tới người đang trực tuyến thuộc đối tượng, quyết định
 * tạo thông báo chat khi người nhận offline, tin "vắng mặt" của shop, chấm "đang hoạt động".
 */
@Component
public class OnlineUsers {

    private final SimpUserRegistry registry;

    public OnlineUsers(SimpUserRegistry registry) {
        this.registry = registry;
    }

    /** Người dùng có ít nhất một phiên WebSocket đang mở. */
    public boolean isOnline(long userId) {
        SimpUser u = registry.getUser(String.valueOf(userId));
        return u != null && u.hasSessions();
    }

    /** Những id trong {@code userIds} đang trực tuyến (giữ thứ tự đầu vào). */
    public Set<Long> onlineAmong(Collection<Long> userIds) {
        Set<Long> out = new LinkedHashSet<>();
        for (Long id : userIds) {
            if (id != null && isOnline(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Mọi người dùng đang trực tuyến (bỏ qua phiên không mang {@code userId} dạng số). */
    public Set<Long> allOnline() {
        Set<Long> out = new LinkedHashSet<>();
        for (SimpUser u : registry.getUsers()) {
            if (!u.hasSessions()) {
                continue;
            }
            try {
                out.add(Long.parseLong(u.getName()));
            } catch (NumberFormatException ignored) {
                // không phải phiên của người dùng đã xác thực
            }
        }
        return out;
    }
}
