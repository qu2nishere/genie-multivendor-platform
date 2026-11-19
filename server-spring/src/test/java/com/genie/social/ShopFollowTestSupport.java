package com.genie.social;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Tiện ích chung cho test theo dõi shop (plan theo dõi gian hàng F02, F03). */
abstract class ShopFollowTestSupport extends IntegrationTestBase {

    protected static final String URL = "/api/v1/me/following";

    @Autowired
    protected JwtService jwtService;

    protected TestDataBuilder data;

    @BeforeEach
    void builder() {
        data = new TestDataBuilder(jdbc);
    }

    /** Shop VERIFIED mới: trả {vendorUserId, shopId}. */
    protected long[] shop() {
        long vendor = data.vendor();
        return new long[] {vendor, data.shopIdOf(vendor)};
    }

    protected long customer() {
        return createCustomer(uniqueEmail(), PASSWORD).getId();
    }

    protected String auth(long userId, UserRole role) {
        return "Bearer " + jwtService.issue(userId, role, 1L).token();
    }

    protected String auth(long userId) {
        return auth(userId, UserRole.CUSTOMER);
    }

    protected int followers(long vendorUserId) {
        return jdbc.queryForObject("SELECT follower_count FROM vendor_profiles WHERE user_id = ?", Integer.class, vendorUserId);
    }

    protected int rows(long vendorUserId) {
        return jdbc.queryForObject("SELECT count(*) FROM shop_follows WHERE vendor_id = ?", Integer.class, vendorUserId);
    }
}
