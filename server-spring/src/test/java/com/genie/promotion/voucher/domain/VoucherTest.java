// plan 2026-11-26 voucher V02.06 — trạng thái, luật sửa, cấu hình nhà tài trợ
package com.genie.promotion.voucher.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.common.domain.FundingParty;
import com.genie.common.domain.VoucherType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class VoucherTest {

    static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T07:00:00Z");

    static VoucherTerms terms(String code, OffsetDateTime from, OffsetDateTime to) {
        return new VoucherTerms(code, "Giảm 15%", DiscountType.PERCENTAGE, BigDecimal.valueOf(15), BigDecimal.valueOf(50_000),
                BigDecimal.valueOf(200_000), 100, 1, null, from, to, VoucherScope.ALL, null);
    }

    static Voucher shop(OffsetDateTime from, OffsetDateTime to) {
        return Voucher.createShop(9, terms("SHOP15", from, to));
    }

    @Test
    void statusAtEveryBoundary() {
        Voucher v = shop(NOW.plusHours(1), NOW.plusDays(5));
        assertThat(v.status(NOW)).isEqualTo(VoucherStatus.SCHEDULED);
        assertThat(v.status(NOW.plusHours(1))).isEqualTo(VoucherStatus.ACTIVE);       // đúng giờ bắt đầu
        assertThat(v.status(NOW.plusDays(5).minusSeconds(1))).isEqualTo(VoucherStatus.ACTIVE);
        assertThat(v.status(NOW.plusDays(5))).isEqualTo(VoucherStatus.ENDED);         // đúng giờ kết thúc

        v.setUsedCount(100);                                                         // hết lượt
        assertThat(v.status(NOW.plusDays(1))).isEqualTo(VoucherStatus.ENDED);
        v.setUsedCount(99);
        assertThat(v.status(NOW.plusDays(1))).isEqualTo(VoucherStatus.ACTIVE);

        v.disable();
        assertThat(v.status(NOW.plusDays(1))).isEqualTo(VoucherStatus.DISABLED);
        assertThat(v.status(NOW)).isEqualTo(VoucherStatus.DISABLED);                  // huỷ lịch
    }

    @Test
    void budgetExhaustedEndsEvenAfterConsumeTurnedItOff() {
        Voucher v = shop(NOW.minusDays(1), NOW.plusDays(5));
        v.setBudget(BigDecimal.valueOf(100_000));
        v.consume(BigDecimal.valueOf(60_000));
        assertThat(v.status(NOW)).isEqualTo(VoucherStatus.ACTIVE);
        v.consume(BigDecimal.valueOf(40_000));                                       // chạm ngân sách → tự tắt
        assertThat(v.getIsActive()).isFalse();
        assertThat(v.budgetExhausted()).isTrue();
        assertThat(v.status(NOW)).isEqualTo(VoucherStatus.ENDED);                    // không phải DISABLED
    }

    @Test
    void scheduledWithoutSaversIsFullyEditable() {
        Voucher v = shop(NOW.plusDays(1), NOW.plusDays(5));
        Voucher draft = Voucher.createShop(9, new VoucherTerms("SHOP20", "Giảm 30.000₫", DiscountType.FIXED_AMOUNT,
                BigDecimal.valueOf(30_000), null, BigDecimal.valueOf(300_000), 50, 2, null, NOW.plusDays(2),
                NOW.plusDays(3), VoucherScope.ALL, null));
        assertThat(v.fullyEditable(false, NOW)).isTrue();
        assertThat(v.lockedChanges(draft, false, NOW)).isEmpty();
        v.applyEdit(draft, true);
        assertThat(v.getCode()).isEqualTo("SHOP20");
        assertThat(v.getDiscountType()).isEqualTo(DiscountType.FIXED_AMOUNT);
        assertThat(v.getUsageLimit()).isEqualTo(50);
        assertThat(v.getValidFrom()).isEqualTo(NOW.plusDays(2));
    }

    @Test
    void runningOrSavedVoucherOnlyExtendsOrGrows() {
        Voucher running = shop(NOW.minusDays(1), NOW.plusDays(5));
        Voucher extend = shop(NOW.minusDays(1), NOW.plusDays(9));
        extend.setUsageLimit(500);
        extend.setTitle("Tên mới");
        assertThat(running.lockedChanges(extend, false, NOW)).isEmpty();
        running.applyEdit(extend, false);
        assertThat(running.getValidTo()).isEqualTo(NOW.plusDays(9));
        assertThat(running.getUsageLimit()).isEqualTo(500);

        Voucher cheaper = shop(NOW.minusDays(1), NOW.plusDays(3));                   // rút ngắn hạn
        cheaper.setDiscountValue(BigDecimal.valueOf(20));
        cheaper.setUsageLimit(10);
        assertThat(running.lockedChanges(cheaper, false, NOW))
                .containsExactlyInAnyOrder("discount_value", "valid_to", "usage_limit");

        Voucher savedScheduled = shop(NOW.plusDays(1), NOW.plusDays(5));             // sắp diễn ra nhưng đã có người lưu
        Voucher recode = Voucher.createShop(9, terms("OTHER1", NOW.plusDays(1), NOW.plusDays(5)));
        assertThat(savedScheduled.lockedChanges(recode, true, NOW)).containsExactly("code");
    }

    @Test
    void budgetMayGrowOrBeRemovedButNotShrinkOrBeAdded() {
        Voucher v = shop(NOW.minusDays(1), NOW.plusDays(5));
        v.setBudget(BigDecimal.valueOf(1_000_000));
        Voucher more = shop(NOW.minusDays(1), NOW.plusDays(5));
        more.setBudget(BigDecimal.valueOf(2_000_000));
        Voucher less = shop(NOW.minusDays(1), NOW.plusDays(5));
        less.setBudget(BigDecimal.valueOf(500_000));
        Voucher none = shop(NOW.minusDays(1), NOW.plusDays(5));
        assertThat(v.lockedChanges(more, false, NOW)).isEmpty();
        assertThat(v.lockedChanges(none, false, NOW)).isEmpty();
        assertThat(v.lockedChanges(less, false, NOW)).containsExactly("budget");
        Voucher unlimited = shop(NOW.minusDays(1), NOW.plusDays(5));
        assertThat(unlimited.lockedChanges(more, false, NOW)).containsExactly("budget");
    }

    @Test
    void endedOrDisabledCannotBeEdited() {
        Voucher ended = shop(NOW.minusDays(5), NOW.minusDays(1));
        assertThat(ended.lockedChanges(ended, false, NOW)).containsExactly("status");
        Voucher off = shop(NOW.plusDays(1), NOW.plusDays(5));
        off.disable();
        off.disable();   // idempotent
        assertThat(off.lockedChanges(off, false, NOW)).containsExactly("status");
    }

    @Test
    void deletableOnlyWhenEndedOrDisabledWithoutUsages() {
        Voucher ended = shop(NOW.minusDays(5), NOW.minusDays(1));
        assertThat(ended.isDeletable(false, NOW)).isTrue();
        assertThat(ended.isDeletable(true, NOW)).isFalse();
        Voucher running = shop(NOW.minusDays(1), NOW.plusDays(1));
        assertThat(running.isDeletable(false, NOW)).isFalse();
        running.disable();
        assertThat(running.isDeletable(false, NOW)).isTrue();
    }

    @Test
    void factoriesSetTypeSponsorAndPriority() {
        Voucher s = shop(NOW, NOW.plusDays(1));
        assertThat(s.getType()).isEqualTo(VoucherType.SHOP);
        assertThat(s.getSponsorType()).isEqualTo(FundingParty.VENDOR);
        assertThat(s.getPriority()).isEqualTo(1);
        assertThat(s.getVendorId()).isEqualTo(9);

        Voucher p = Voucher.createPlatform(terms("GENIE50", NOW, NOW.plusDays(1)), FundingParty.PLATFORM, null);
        assertThat(p.getType()).isEqualTo(VoucherType.PLATFORM);
        assertThat(p.getPriority()).isEqualTo(2);
        assertThat(p.getVendorId()).isNull();

        VoucherTerms ship = new VoucherTerms("SHIP30", "Freeship", DiscountType.FREESHIP, null, BigDecimal.valueOf(30_000),
                BigDecimal.valueOf(99_000), 100, 1, null, NOW, NOW.plusDays(1), VoucherScope.ALL, null);
        Voucher f = Voucher.createPlatform(ship, FundingParty.CO_SPONSOR, BigDecimal.valueOf(60));
        assertThat(f.getType()).isEqualTo(VoucherType.FREESHIP);
        assertThat(f.getPriority()).isEqualTo(3);
        assertThat(f.getDiscountValue()).isEqualByComparingTo("30000");               // FREESHIP lưu trần vào discount_value
        assertThat(f.getPlatformSharePercent()).isEqualByComparingTo("60");

        VoucherTerms shipAll = new VoucherTerms("SHIPALL", "Freeship", DiscountType.FREESHIP, null, null,
                BigDecimal.ZERO, 100, 1, null, NOW, NOW.plusDays(1), VoucherScope.ALL, null);
        assertThat(Voucher.createShop(9, shipAll).getDiscountValue()).isEqualByComparingTo("0");
    }

    @Test
    void validateConfigSponsorCombinations() {
        VoucherTerms t = terms("GENIE50", NOW, NOW.plusDays(1));
        assertThat(Voucher.createPlatform(t, FundingParty.CO_SPONSOR, BigDecimal.ONE).getSponsorType())
                .isEqualTo(FundingParty.CO_SPONSOR);
        assertThatThrownBy(() -> Voucher.createPlatform(t, FundingParty.CO_SPONSOR, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Voucher.createPlatform(t, FundingParty.CO_SPONSOR, BigDecimal.valueOf(100)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Voucher.createPlatform(t, FundingParty.VENDOR, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Voucher.createPlatform(t, FundingParty.PLATFORM, BigDecimal.TEN).getPlatformSharePercent()).isNull();
        assertThatThrownBy(() -> Voucher.createShop(9, terms("SHOP15", NOW, NOW)))   // kết thúc = bắt đầu
                .isInstanceOf(IllegalArgumentException.class);
        Voucher s = shop(NOW, NOW.plusDays(1));
        s.setSponsorType(FundingParty.PLATFORM);
        assertThatThrownBy(s::validateConfig).isInstanceOf(IllegalArgumentException.class);
    }
}
