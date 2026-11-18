package com.genie.promotion;

import com.genie.auth.profile.domain.User;
import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;

/** Tiện ích chung cho test Điểm Genie (plan Điểm Genie P03–P06). Mỗi test một khách mới — dữ liệu không đụng nhau. */
public abstract class CoinTestSupport extends IntegrationTestBase {

    protected static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    protected JwtService jwtService;

    protected long newCustomer() {
        User u = createCustomer(uniqueEmail(), PASSWORD);
        return u.getId();
    }

    /** Token phát theo đồng hồ test HIỆN TẠI (tua đồng hồ nhiều ngày thì token cũ đã hết hạn 15 phút). */
    protected String auth(long userId) {
        return "Bearer " + jwtService.issue(userId, UserRole.CUSTOMER, 1L).token();
    }

    /** Đặt đồng hồ theo giờ Việt Nam, vd {@code at("2026-06-01T09:00")}. */
    protected void at(String vnLocal) {
        clock.set(LocalDateTime.parse(vnLocal).atZone(VN).toInstant());
    }

    protected int balance(long userId) {
        Integer b = jdbc.query("SELECT balance FROM user_coins WHERE user_id = ?", rs -> rs.next() ? rs.getInt(1) : 0,
                userId);
        return b == null ? 0 : b;
    }

    protected long ledgerSum(long userId) {
        Long s = jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM coin_transactions WHERE user_id = ?", Long.class,
                userId);
        return s == null ? 0 : s;
    }
}
