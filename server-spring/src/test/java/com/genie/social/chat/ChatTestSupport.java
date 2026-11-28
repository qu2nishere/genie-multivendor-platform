// Tiện ích chung cho IT chat (plan 2026-11-26 chat M02–M08)
package com.genie.social.chat;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ChatTestSupport extends IntegrationTestBase {

    @Autowired
    protected JwtService jwtService;

    protected TestDataBuilder data;

    @BeforeEach
    void chatSupport() {
        data = new TestDataBuilder(jdbc);
    }

    protected long customer() {
        return createCustomer(uniqueEmail(), PASSWORD).getId();
    }

    /** Shop {@code VERIFIED} (chủ ACTIVE) — {@code userId} = {@code conversations.vendor_id}, {@code shopId} trên API. */
    protected TestDataBuilder.Shop shop() {
        return data.shop().create();
    }

    protected String auth(long userId, UserRole role) {
        return "Bearer " + jwtService.issue(userId, role, 1L).token();
    }

    protected String customerAuth(long userId) {
        return auth(userId, UserRole.CUSTOMER);
    }

    protected String vendorAuth(long vendorUserId) {
        return auth(vendorUserId, UserRole.VENDOR);
    }

    /** Sản phẩm PUBLISHED một biến thể của {@code shop}; trả {@code [productId, variantId]}. */
    protected long[] product(TestDataBuilder.Shop shop, long price) {
        long productId = data.product(shop.userId(), "SP " + TestDataBuilder.uniqueWord(), "PUBLISHED", price);
        long variantId = data.variant(productId, price, 10);
        return new long[] {productId, variantId};
    }

    /** Đơn COD giữa khách và shop (một dòng {@code variantId}) lúc {@code createdAtIso}. */
    protected long order(long customerId, long vendorUserId, long variantId, long total, String createdAtIso) {
        String code = "TR-T" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        long id = jdbc.queryForObject("""
                INSERT INTO orders (order_code, customer_id, vendor_id, shipping_address_snapshot_json, sub_total,
                    shipping_fee, grand_total, funding_status, status, created_at, updated_at)
                VALUES (?, ?, ?, '{}'::jsonb, ?, 0, ?, 'UNFUNDED', 'PENDING_PAYMENT', CAST(? AS TIMESTAMPTZ), now())
                RETURNING id""", Long.class, code, customerId, vendorUserId, BigDecimal.valueOf(total),
                BigDecimal.valueOf(total), createdAtIso);
        jdbc.update("""
                INSERT INTO order_items (order_id, product_variant_id, quantity, unit_price, line_total, gross_amount,
                    commission_rate, commission_amount, vendor_receivable)
                VALUES (?, ?, 1, ?, ?, ?, 0.05, 0, ?)""", id, variantId, BigDecimal.valueOf(total),
                BigDecimal.valueOf(total), BigDecimal.valueOf(total), BigDecimal.valueOf(total));
        return id;
    }
}
