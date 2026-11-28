// Xóa cache sau commit, rollback thì không (plan 2026-11-26 voucher N5, V05.08)
package com.genie.promotion.voucher;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.domain.UserRole;
import com.genie.promotion.voucher.application.PromotionSearchPorts;
import com.genie.support.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VoucherCacheEvictionIT extends VoucherTestSupport {

    @Autowired
    PromotionSearchPorts searchPorts;

    private void warm(TestDataBuilder.Shop s) throws Exception {
        searchPorts.vendorIdsWithActiveVoucher();
        get_("/api/v1/public/vouchers?shop_id=" + s.shopId(), null);
        get_("/api/v1/public/vouchers?featured=true", null);
        assertThat(redis.hasKey("cache:promotion:shop_voucher_vendors")).isTrue();
        assertThat(redis.keys("cache:promotion:public_vouchers:*")).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shopVoucherChangesEvictBothCachesAfterCommit() throws Exception {
        TestDataBuilder.Shop s = data.shop().create();
        String vendor = auth(s.userId(), UserRole.VENDOR);
        warm(s);
        String c = code();
        long id = post_("/api/v1/seller/vouchers", vendor, percentBody(c, now(), now().plusDays(3), null)).data().get("id").asLong();
        assertThat(redis.hasKey("cache:promotion:shop_voucher_vendors")).isFalse();
        assertThat(redis.keys("cache:promotion:public_vouchers:*")).isEmpty();

        warm(s);
        assertThat(put_("/api/v1/seller/vouchers/" + id, vendor, percentBody(c, now(), now().plusDays(6), null)).status())
                .isEqualTo(200);
        assertThat(redis.hasKey("cache:promotion:shop_voucher_vendors")).isFalse();

        warm(s);
        assertThat(patch_("/api/v1/seller/vouchers/" + id, vendor, "{\"is_active\":false}").status()).isEqualTo(200);
        assertThat(redis.keys("cache:promotion:public_vouchers:*")).isEmpty();
    }

    @Test
    void failedWritesDoNotEvict() throws Exception {
        TestDataBuilder.Shop s = data.shop().create();
        String vendor = auth(s.userId(), UserRole.VENDOR);
        String c = code();
        long id = post_("/api/v1/seller/vouchers", vendor, percentBody(c, now(), now().plusDays(3), null)).data().get("id").asLong();
        warm(s);
        // đổi mức giảm khi đang chạy → 409 trong transaction (rollback) → cache giữ nguyên
        Res locked = put_("/api/v1/seller/vouchers/" + id, vendor,
                percentBody(c, now(), now().plusDays(3), null).replace("\"discount_value\":15", "\"discount_value\":30"));
        assertThat(locked.status()).isEqualTo(409);
        assertThat(redis.hasKey("cache:promotion:shop_voucher_vendors")).isTrue();
        assertThat(redis.keys("cache:promotion:public_vouchers:*")).hasSizeGreaterThanOrEqualTo(2);
        // trùng mã (lỗi khi ghi) cũng không xóa
        assertThat(post_("/api/v1/seller/vouchers", vendor, percentBody(c, now(), now().plusDays(3), null)).status())
                .isEqualTo(409);
        assertThat(redis.hasKey("cache:promotion:shop_voucher_vendors")).isTrue();
    }
}
