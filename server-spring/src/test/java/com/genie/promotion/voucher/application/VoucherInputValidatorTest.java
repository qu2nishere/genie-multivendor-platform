// plan 2026-11-26 voucher V02.06 — mỗi luật D5–D10 một ca biên + tiêu đề tự sinh (D6)
package com.genie.promotion.voucher.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.common.domain.FundingParty;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.promotion.voucher.application.VoucherInputValidator.Kind;
import com.genie.promotion.voucher.application.VoucherInputValidator.Result;
import com.genie.promotion.voucher.domain.DiscountType;
import com.genie.promotion.voucher.domain.VoucherScope;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VoucherInputValidatorTest {

    static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-08T07:00:00Z");

    static VoucherInput percent() {
        return new VoucherInput(null, "summer26", "PERCENTAGE", BigDecimal.valueOf(15), BigDecimal.valueOf(50_000),
                BigDecimal.valueOf(200_000), 500, 1, null, NOW.plusHours(1), NOW.plusDays(10), "ALL", List.of(),
                List.of(), List.of(), List.of(), null, null, null);
    }

    static VoucherInput with(VoucherInput b, String field, Object value) {
        return new VoucherInput(
                field.equals("title") ? (String) value : b.title(),
                field.equals("code") ? (String) value : b.code(),
                field.equals("discount_type") ? (String) value : b.discountType(),
                field.equals("discount_value") ? (BigDecimal) value : b.discountValue(),
                field.equals("max_discount") ? (BigDecimal) value : b.maxDiscount(),
                field.equals("min_order_value") ? (BigDecimal) value : b.minOrderValue(),
                field.equals("usage_limit") ? (Integer) value : b.usageLimit(),
                field.equals("usage_per_user") ? (Integer) value : b.usagePerUser(),
                field.equals("budget") ? (BigDecimal) value : b.budget(),
                field.equals("valid_from") ? (OffsetDateTime) value : b.validFrom(),
                field.equals("valid_to") ? (OffsetDateTime) value : b.validTo(),
                field.equals("scope") ? (String) value : b.scope(),
                field.equals("category_ids") ? castList(value) : b.categoryIds(),
                field.equals("product_ids") ? castList(value) : b.productIds(),
                field.equals("excluded_category_ids") ? castList(value) : b.excludedCategoryIds(),
                field.equals("excluded_product_ids") ? castList(value) : b.excludedProductIds(),
                field.equals("funded_by") ? (String) value : b.fundedBy(),
                field.equals("platform_share_percent") ? (BigDecimal) value : b.platformSharePercent(),
                b.notificationBody());
    }

    @SuppressWarnings("unchecked")
    static List<Long> castList(Object o) {
        return (List<Long>) o;
    }

    static List<String> fields(Result r) {
        return r.violations().stream().map(Violation::field).toList();
    }

    @Test
    void validShopPercentVoucherGetsUppercaseCodeAndGeneratedTitle() {
        Result r = VoucherInputValidator.validate(percent(), Kind.SHOP, NOW, true);
        assertThat(r.violations()).isEmpty();
        assertThat(r.draft().terms().code()).isEqualTo("SUMMER26");
        assertThat(r.draft().terms().title()).isEqualTo("Giảm 15% tối đa 50.000₫ đơn từ 200.000₫");
        assertThat(r.draft().sponsor()).isEqualTo(FundingParty.VENDOR);
    }

    @Test
    void codeFormat() {
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "code", "AB12"), Kind.SHOP, NOW, true)))
                .containsExactly("code");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "code", "ABCDEFGHIJKLM"), Kind.SHOP, NOW, true)))
                .containsExactly("code");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "code", "SALE-50"), Kind.SHOP, NOW, true)))
                .containsExactly("code");
        assertThat(VoucherInputValidator.validate(with(percent(), "code", " abc123 "), Kind.SHOP, NOW, true).violations())
                .isEmpty();
    }

    @Test
    void percentageNeedsCapAndOneToHundred() {
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "discount_value", BigDecimal.valueOf(101)), Kind.SHOP, NOW, true)))
                .containsExactly("discount_value");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "discount_value", BigDecimal.ZERO), Kind.SHOP, NOW, true)))
                .containsExactly("discount_value");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "max_discount", null), Kind.SHOP, NOW, true)))
                .containsExactly("max_discount");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "max_discount", BigDecimal.valueOf(999)), Kind.SHOP, NOW, true)))
                .containsExactly("max_discount");
        assertThat(VoucherInputValidator.validate(with(percent(), "discount_value", new BigDecimal("7.5")), Kind.SHOP, NOW, true)
                .draft().terms().title()).startsWith("Giảm 7,5%");
    }

    @Test
    void fixedAmountBoundsAndMinOrder() {
        VoucherInput fixed = with(with(percent(), "discount_type", "FIXED_AMOUNT"), "discount_value", BigDecimal.valueOf(30_000));
        Result ok = VoucherInputValidator.validate(with(fixed, "min_order_value", BigDecimal.valueOf(399_000)), Kind.SHOP, NOW, true);
        assertThat(ok.violations()).isEmpty();
        assertThat(ok.draft().terms().maxDiscountCap()).isNull();
        assertThat(ok.draft().terms().title()).isEqualTo("Giảm 30.000₫ đơn từ 399.000₫");
        assertThat(fields(VoucherInputValidator.validate(with(fixed, "min_order_value", BigDecimal.valueOf(29_999)), Kind.SHOP, NOW, true)))
                .containsExactly("min_order_value");
        assertThat(fields(VoucherInputValidator.validate(with(fixed, "discount_value", BigDecimal.valueOf(999)), Kind.SHOP, NOW, true)))
                .containsExactly("discount_value");
        assertThat(fields(VoucherInputValidator.validate(with(fixed, "discount_value", BigDecimal.valueOf(50_000_001)), Kind.SHOP, NOW, true)))
                .contains("discount_value");
        assertThat(fields(VoucherInputValidator.validate(with(fixed, "discount_value", new BigDecimal("30000.5")), Kind.SHOP, NOW, true)))
                .containsExactly("discount_value");
    }

    @Test
    void freeshipIgnoresValueAndCapIsOptional() {
        VoucherInput ship = with(with(percent(), "discount_type", "FREESHIP"), "max_discount", BigDecimal.valueOf(30_000));
        Result r = VoucherInputValidator.validate(with(ship, "min_order_value", BigDecimal.valueOf(99_000)), Kind.SHOP, NOW, true);
        assertThat(r.violations()).isEmpty();
        assertThat(r.draft().terms().discountType()).isEqualTo(DiscountType.FREESHIP);
        assertThat(r.draft().terms().discountValue()).isNull();
        assertThat(r.draft().terms().title()).isEqualTo("Freeship tối đa 30.000₫ đơn từ 99.000₫");
        assertThat(VoucherInputValidator.validate(with(ship, "max_discount", null), Kind.SHOP, NOW, true).violations()).isEmpty();
    }

    @Test
    void usageLimitsAndBudget() {
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "usage_limit", 0), Kind.SHOP, NOW, true)))
                .contains("usage_limit");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "usage_limit", 1_000_001), Kind.SHOP, NOW, true)))
                .contains("usage_limit");
        assertThat(fields(VoucherInputValidator.validate(with(with(percent(), "usage_limit", 3), "usage_per_user", 4), Kind.SHOP, NOW, true)))
                .containsExactly("usage_per_user");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "usage_per_user", 101), Kind.SHOP, NOW, true)))
                .containsExactly("usage_per_user");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "budget", BigDecimal.valueOf(49_999)), Kind.SHOP, NOW, true)))
                .containsExactly("budget");
        assertThat(VoucherInputValidator.validate(with(percent(), "budget", BigDecimal.valueOf(50_000)), Kind.SHOP, NOW, true)
                .violations()).isEmpty();
    }

    @Test
    void timeWindow() {
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "valid_from", NOW.minusMinutes(6)), Kind.SHOP, NOW, true)))
                .containsExactly("valid_from");
        assertThat(VoucherInputValidator.validate(with(percent(), "valid_from", NOW.minusMinutes(4)), Kind.SHOP, NOW, true)
                .violations()).isEmpty();
        assertThat(VoucherInputValidator.validate(with(percent(), "valid_from", NOW.minusDays(3)), Kind.SHOP, NOW, false)
                .violations()).isEmpty();   // sửa: không kiểm quá khứ
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "valid_to", NOW.plusHours(1)), Kind.SHOP, NOW, true)))
                .containsExactly("valid_to");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "valid_to", NOW.plusDays(182)), Kind.SHOP, NOW, true)))
                .containsExactly("valid_to");
    }

    @Test
    void scopeSetsAndExclusions() {
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "scope", "CATEGORY"), Kind.SHOP, NOW, true)))
                .containsExactly("category_ids");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "scope", "PRODUCT"), Kind.SHOP, NOW, true)))
                .containsExactly("product_ids");
        Result prod = VoucherInputValidator.validate(with(with(with(percent(), "scope", "PRODUCT"), "product_ids", List.of(5L, 5L, 6L)),
                "excluded_category_ids", List.of(3L)), Kind.SHOP, NOW, true);
        assertThat(prod.draft().productIds()).containsExactly(5L, 6L);
        assertThat(prod.draft().excludedCategoryIds()).isEmpty();   // loại trừ bỏ qua khi PRODUCT
        Result cat = VoucherInputValidator.validate(with(with(with(percent(), "scope", "CATEGORY"), "category_ids", List.of(1L)),
                "product_ids", List.of(9L)), Kind.SHOP, NOW, true);
        assertThat(cat.draft().terms().scope()).isEqualTo(VoucherScope.CATEGORY);
        assertThat(cat.draft().productIds()).isEmpty();
        assertThat(fields(VoucherInputValidator.validate(with(with(with(percent(), "scope", "CATEGORY"), "category_ids", List.of(1L)),
                "excluded_category_ids", List.of(1L)), Kind.SHOP, NOW, true))).containsExactly("excluded_category_ids");
        assertThat(fields(VoucherInputValidator.validate(with(percent(), "scope", "PRODUCT"), Kind.PLATFORM, NOW, true)))
                .contains("scope");
    }

    @Test
    void platformTitleAndSponsor() {
        VoucherInput p = with(percent(), "title", "Siêu Sale Giữa Tháng");
        Result ok = VoucherInputValidator.validate(p, Kind.PLATFORM, NOW, true);
        assertThat(ok.violations()).isEmpty();
        assertThat(ok.draft().sponsor()).isEqualTo(FundingParty.PLATFORM);
        assertThat(ok.draft().terms().title()).isEqualTo("Siêu Sale Giữa Tháng");
        assertThat(fields(VoucherInputValidator.validate(with(p, "title", "Sale"), Kind.PLATFORM, NOW, true)))
                .containsExactly("title");
        assertThat(fields(VoucherInputValidator.validate(with(p, "funded_by", "CO_SPONSOR"), Kind.PLATFORM, NOW, true)))
                .containsExactly("platform_share_percent");
        Result co = VoucherInputValidator.validate(with(with(p, "funded_by", "CO_SPONSOR"), "platform_share_percent",
                BigDecimal.valueOf(50)), Kind.PLATFORM, NOW, true);
        assertThat(co.draft().platformSharePercent()).isEqualByComparingTo("50");
        assertThat(fields(VoucherInputValidator.validate(with(p, "funded_by", "VENDOR"), Kind.PLATFORM, NOW, true)))
                .containsExactly("funded_by");
    }

    @Test
    void allErrorsAreReportedAtOnce() {
        VoucherInput bad = new VoucherInput(null, "x", "WHAT", null, null, null, null, null, null, null, null, "ALL",
                null, null, null, null, null, null, null);
        assertThat(fields(VoucherInputValidator.validate(bad, Kind.SHOP, NOW, true)))
                .contains("code", "discount_type", "min_order_value", "usage_limit", "usage_per_user", "valid_from", "valid_to");
    }

    @Test
    void titles() {
        assertThat(VoucherTitles.of(DiscountType.FREESHIP, null, null, BigDecimal.ZERO)).isEqualTo("Freeship");
        assertThat(VoucherTitles.of(DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(1_500_000), null, null))
                .isEqualTo("Giảm 1.500.000₫");
    }
}
