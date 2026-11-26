package com.genie.order.cart;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.OrderTestData;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Tiện ích chung cho test chuỗi C2 (plan C2 O02.05). */
public abstract class CartTestSupport extends IntegrationTestBase {

    @Autowired
    protected JwtService jwtService;

    protected OrderTestData data;

    @BeforeEach
    void orderData() {
        data = new OrderTestData(jdbc);
    }

    protected long customer() {
        return createCustomer(uniqueEmail(), PASSWORD).getId();
    }

    protected String auth(long userId) {
        return "Bearer " + jwtService.issue(userId, UserRole.CUSTOMER, 1L).token();
    }

    protected static String addBody(long variantId, int qty) {
        return "{\"variant_id\":" + variantId + ",\"quantity\":" + qty + "}";
    }
}
