// Kiểm & chuẩn hóa thân tạo / sửa voucher — hàm thuần (plan 2026-11-26 voucher D5, D6, D7, D8, D9, D10; API_SPEC 10.7, 11.14)
package com.genie.promotion.voucher.application;

import com.genie.common.domain.FundingParty;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import com.genie.promotion.voucher.domain.DiscountType;
import com.genie.promotion.voucher.domain.VoucherScope;
import com.genie.promotion.voucher.domain.VoucherTerms;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Gom MỌI lỗi một lần ({@code 422 GNE-VAL-4220} + {@code errors[]} theo trường). Kiểm phụ thuộc CSDL (ngành tồn tại, sản
 * phẩm của shop, trùng mã) nằm ở service — service cộng thêm lỗi vào {@link Result#violations()} rồi gọi {@link #throwIfAny}.
 */
public final class VoucherInputValidator {

    public enum Kind { SHOP, PLATFORM }

    static final Pattern CODE = Pattern.compile("^[A-Z0-9]{6,12}$");
    static final BigDecimal MIN_MONEY = BigDecimal.valueOf(1_000);
    static final BigDecimal MAX_FIXED = BigDecimal.valueOf(50_000_000);
    static final BigDecimal MAX_MIN_ORDER = BigDecimal.valueOf(1_000_000_000);
    static final int MAX_USAGE_LIMIT = 1_000_000;
    static final int MAX_PER_USER = 100;
    static final int MAX_CATEGORIES = 20;
    static final int MAX_PRODUCTS = 100;
    static final int MAX_EXCLUDED = 50;
    static final Duration MAX_DURATION = Duration.ofDays(180);
    static final Duration START_GRACE = Duration.ofMinutes(5);

    /** {@code draft} = {@code null} khi có lỗi. */
    public record Result(VoucherDraft draft, List<Violation> violations) {
    }

    private VoucherInputValidator() {
    }

    /** Chuẩn hóa mã: cắt khoảng trắng, chữ HOA. */
    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * @param creating tạo mới (kiểm {@code valid_from} không ở quá khứ); sửa thì bỏ qua — luật khóa trường ở entity.
     */
    public static Result validate(VoucherInput in, Kind kind, OffsetDateTime now, boolean creating) {
        List<Violation> v = new ArrayList<>();
        String code = normalizeCode(in.code());
        if (code == null || !CODE.matcher(code).matches()) {
            v.add(err("code", "INVALID_FORMAT", in.code(), "Mã gồm 6–12 ký tự chữ in hoa và số."));
        }

        String title = null;
        if (kind == Kind.PLATFORM) {
            title = in.title() == null ? null : in.title().strip();
            if (title == null || title.length() < 5 || title.length() > 100) {
                v.add(err("title", "LENGTH", in.title(), "Tên chương trình 5–100 ký tự."));
            }
        }

        DiscountType type = parse(DiscountType.class, in.discountType());
        if (type == null) {
            v.add(err("discount_type", "INVALID", in.discountType(), "Loại giảm giá không hợp lệ."));
        }
        BigDecimal value = in.discountValue();
        BigDecimal max = in.maxDiscount();
        BigDecimal minOrder = in.minOrderValue();
        if (minOrder == null || minOrder.signum() < 0 || minOrder.compareTo(MAX_MIN_ORDER) > 0 || !whole(minOrder)) {
            v.add(err("min_order_value", "OUT_OF_RANGE", minOrder, "Đơn tối thiểu từ 0 đến 1.000.000.000₫."));
        }
        BigDecimal maxPerUse = null;   // mức giảm tối đa một lượt — để so với ngân sách
        if (type == DiscountType.PERCENTAGE) {
            if (value == null || value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0
                    || value.scale() > 2 && value.stripTrailingZeros().scale() > 2) {
                v.add(err("discount_value", "OUT_OF_RANGE", value, "Phần trăm giảm từ 1 đến 100."));
            }
            if (max == null || max.compareTo(MIN_MONEY) < 0 || !whole(max)) {
                v.add(err("max_discount", "REQUIRED", max, "Giảm theo % phải có mức giảm tối đa từ 1.000₫."));
            }
            maxPerUse = max;
        } else if (type == DiscountType.FIXED_AMOUNT) {
            if (value == null || value.compareTo(MIN_MONEY) < 0 || value.compareTo(MAX_FIXED) > 0 || !whole(value)) {
                v.add(err("discount_value", "OUT_OF_RANGE", value, "Số tiền giảm từ 1.000₫ đến 50.000.000₫."));
            } else if (minOrder != null && minOrder.compareTo(value) < 0) {
                v.add(err("min_order_value", "BELOW_DISCOUNT", minOrder, "Đơn tối thiểu phải không nhỏ hơn số tiền giảm."));
            }
            max = null;
            maxPerUse = value;
        } else if (type == DiscountType.FREESHIP) {
            value = null;
            if (max != null && (max.compareTo(MIN_MONEY) < 0 || !whole(max))) {
                v.add(err("max_discount", "OUT_OF_RANGE", max, "Trần hỗ trợ phí ship từ 1.000₫ (bỏ trống = hỗ trợ toàn bộ)."));
            }
            maxPerUse = max;
        }

        Integer limit = in.usageLimit();
        if (limit == null || limit < 1 || limit > MAX_USAGE_LIMIT) {
            v.add(err("usage_limit", "OUT_OF_RANGE", limit, "Số lượt phát hành từ 1 đến 1.000.000."));
        }
        Integer perUser = in.usagePerUser();
        int perUserMax = limit == null || limit < 1 ? MAX_PER_USER : Math.min(limit, MAX_PER_USER);
        if (perUser == null || perUser < 1 || perUser > perUserMax) {
            v.add(err("usage_per_user", "OUT_OF_RANGE", perUser, "Lượt mỗi khách từ 1 đến " + perUserMax + "."));
        }
        BigDecimal budget = in.budget();
        if (budget != null && (budget.signum() <= 0 || !whole(budget)
                || (maxPerUse != null && budget.compareTo(maxPerUse) < 0))) {
            v.add(err("budget", "TOO_SMALL", budget, "Ngân sách phải không nhỏ hơn mức giảm tối đa của một lượt."));
        }

        OffsetDateTime from = in.validFrom();
        OffsetDateTime to = in.validTo();
        if (from == null) {
            v.add(err("valid_from", "REQUIRED", null, "Chọn thời điểm bắt đầu."));
        } else if (creating && from.isBefore(now.minus(START_GRACE))) {
            v.add(err("valid_from", "IN_PAST", from, "Thời điểm bắt đầu không được ở quá khứ."));
        }
        if (to == null) {
            v.add(err("valid_to", "REQUIRED", null, "Chọn thời điểm kết thúc."));
        } else if (from != null && !to.isAfter(from)) {
            v.add(err("valid_to", "BEFORE_START", to, "Thời điểm kết thúc phải sau thời điểm bắt đầu."));
        } else if (from != null && Duration.between(from, to).compareTo(MAX_DURATION) > 0) {
            v.add(err("valid_to", "TOO_LONG", to, "Voucher chạy tối đa 180 ngày."));
        }

        VoucherScope scope = parse(VoucherScope.class, in.scope() == null ? "ALL" : in.scope());
        if (scope == null || (kind == Kind.PLATFORM && scope == VoucherScope.PRODUCT)) {
            v.add(err("scope", "INVALID", in.scope(), kind == Kind.PLATFORM ? "Phạm vi: ALL hoặc CATEGORY."
                    : "Phạm vi: ALL, CATEGORY hoặc PRODUCT."));
        }
        Set<Long> cats = ids(in.categoryIds());
        Set<Long> prods = ids(in.productIds());
        Set<Long> exCats = ids(in.excludedCategoryIds());
        Set<Long> exProds = ids(in.excludedProductIds());
        if (scope == VoucherScope.CATEGORY && (cats.isEmpty() || cats.size() > MAX_CATEGORIES)) {
            v.add(err("category_ids", "REQUIRED", in.categoryIds(), "Chọn 1–20 ngành hàng."));
        }
        if (scope == VoucherScope.PRODUCT && (prods.isEmpty() || prods.size() > MAX_PRODUCTS)) {
            v.add(err("product_ids", "REQUIRED", in.productIds(), "Chọn 1–100 sản phẩm."));
        }
        if (scope != VoucherScope.CATEGORY) cats = Set.of();
        if (scope != VoucherScope.PRODUCT) prods = Set.of();
        if (scope == VoucherScope.PRODUCT) {
            exCats = Set.of();
            exProds = Set.of();
        }
        if (exCats.size() > MAX_EXCLUDED) v.add(err("excluded_category_ids", "TOO_MANY", exCats.size(), "Tối đa 50 ngành loại trừ."));
        if (exProds.size() > MAX_EXCLUDED) v.add(err("excluded_product_ids", "TOO_MANY", exProds.size(), "Tối đa 50 sản phẩm loại trừ."));
        if (!java.util.Collections.disjoint(cats, exCats)) {
            v.add(err("excluded_category_ids", "OVERLAP", in.excludedCategoryIds(), "Ngành vừa áp dụng vừa loại trừ."));
        }

        FundingParty sponsor = FundingParty.VENDOR;
        BigDecimal share = null;
        if (kind == Kind.PLATFORM) {
            sponsor = parse(FundingParty.class, in.fundedBy() == null ? "PLATFORM" : in.fundedBy());
            if (sponsor == null || sponsor == FundingParty.VENDOR) {
                v.add(err("funded_by", "INVALID", in.fundedBy(), "Nhà tài trợ: PLATFORM hoặc CO_SPONSOR."));
            } else if (sponsor == FundingParty.CO_SPONSOR) {
                share = in.platformSharePercent();
                if (share == null || share.compareTo(BigDecimal.ONE) < 0 || share.compareTo(BigDecimal.valueOf(99)) > 0) {
                    v.add(err("platform_share_percent", "OUT_OF_RANGE", share, "Phần sàn chịu từ 1 đến 99%."));
                }
            }
        }

        if (!v.isEmpty()) {
            return new Result(null, v);
        }
        if (kind == Kind.SHOP) {
            title = VoucherTitles.of(type, value, max, minOrder);
        }
        String body = in.notificationBody() == null || in.notificationBody().isBlank() ? null : in.notificationBody().strip();
        VoucherTerms terms = new VoucherTerms(code, title, type, value, max, minOrder, limit, perUser, budget, from, to,
                scope, kind == Kind.PLATFORM ? body : null);
        return new Result(new VoucherDraft(terms, cats, prods, exCats, exProds, sponsor, share), v);
    }

    public static void throwIfAny(List<Violation> violations) {
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(violations);
        }
    }

    public static Violation err(String field, String code, Object rejected, String message) {
        return new Violation(field, code, rejected, message);
    }

    private static boolean whole(BigDecimal n) {
        return n.stripTrailingZeros().scale() <= 0;
    }

    private static Set<Long> ids(List<Long> list) {
        Set<Long> out = new LinkedHashSet<>();
        if (list != null) {
            for (Long id : list) {
                if (id != null && id > 0) out.add(id);
            }
        }
        return out;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
