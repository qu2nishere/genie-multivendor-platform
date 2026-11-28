// Ma trận quyền voucher (plan 2026-11-26 voucher V04.05)
package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.domain.UserRole;
import com.genie.support.TestDataBuilder;
import org.junit.jupiter.api.Test;

class VoucherSecurityIT extends VoucherTestSupport {

    @Test
    void platformVouchersNeedManageVoucher() throws Exception {
        long admin = jdbc.queryForObject("SELECT id FROM users WHERE email = 'yen.dh@genie.example'", Long.class);
        assertThat(get_("/api/v1/admin/vouchers", auth(admin, UserRole.ADMIN)).status()).isEqualTo(200);
        assertThat(get_("/api/v1/admin/vouchers", auth(staff("MANAGE_VOUCHER"), UserRole.STAFF)).status()).isEqualTo(200);
        String moderator = auth(staff("REVIEW_KYC", "MODERATE_REVIEW"), UserRole.STAFF);
        assertThat(get_("/api/v1/admin/vouchers", moderator).status()).isEqualTo(403);
        assertThat(post_("/api/v1/admin/vouchers", moderator, "{}").status()).isEqualTo(403);
        assertThat(get_("/api/v1/admin/vouchers", auth(customer(), UserRole.CUSTOMER)).status()).isEqualTo(403);
        assertThat(get_("/api/v1/admin/vouchers", null).status()).isEqualTo(401);
    }

    @Test
    void sellerVouchersNeedAVerifiedShop() throws Exception {
        TestDataBuilder.Shop pending = data.shop().kyc("PENDING").create();
        Res r = get_("/api/v1/seller/vouchers", auth(pending.userId(), UserRole.VENDOR));
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.code()).isEqualTo("GNE-VND-4031");
        assertThat(get_("/api/v1/seller/vouchers", auth(customer(), UserRole.CUSTOMER)).status()).isEqualTo(403);
        assertThat(get_("/api/v1/seller/vouchers", null).status()).isEqualTo(401);
        TestDataBuilder.Shop ok = data.shop().create();
        assertThat(get_("/api/v1/seller/vouchers", auth(ok.userId(), UserRole.VENDOR)).status()).isEqualTo(200);
    }
}
