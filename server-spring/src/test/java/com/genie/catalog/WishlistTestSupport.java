package com.genie.catalog;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/** Tiện ích chung cho test sản phẩm yêu thích (plan wishlist W02, W03). */
abstract class WishlistTestSupport extends IntegrationTestBase {

    @Autowired
    protected JwtService jwtService;

    protected TestDataBuilder data;

    @BeforeEach
    void builder() {
        data = new TestDataBuilder(jdbc);
    }

    protected long customer() {
        return createCustomer(uniqueEmail(), PASSWORD).getId();
    }

    protected String auth(long userId) {
        return "Bearer " + jwtService.issue(userId, UserRole.CUSTOMER, 1L).token();
    }

    /** Sản phẩm đang bán có 1 SKU còn tồn + ảnh chính. */
    protected long onSaleProduct(long vendor, String name, long price) {
        long p = data.product(vendor, name, "PUBLISHED", price);
        data.variant(p, price, 10);
        data.primaryImage(p, "seed/product/" + p + ".jpg");
        return p;
    }

    protected int rows(long userId) {
        return jdbc.queryForObject("SELECT count(*) FROM user_wishlists WHERE user_id = ?", Integer.class, userId);
    }
}
