// Danh sách quản lý voucher + thẻ thống kê (API_SPEC 10.7, 11.14; plan 2026-11-26 voucher V03.03, V04.02, D2, D17)
package com.genie.promotion.voucher.application;

import com.genie.common.api.PageResponse;
import com.genie.common.error.InvalidRequestException;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherRepository;
import com.genie.promotion.voucher.domain.VoucherStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherManagementQuery {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int DEFAULT_SIZE = 10;
    static final int MAX_SIZE = 50;

    private final VoucherRepository vouchers;
    private final VoucherReadModel readModel;
    private final VoucherManagementService management;
    private final Clock clock;

    public VoucherManagementQuery(VoucherRepository vouchers, VoucherReadModel readModel,
                                  VoucherManagementService management, Clock clock) {
        this.vouchers = vouchers;
        this.readModel = readModel;
        this.management = management;
        this.clock = clock;
    }

    public record Page(List<VoucherAdminViews.Row> rows, PageResponse.Pagination pagination, Map<String, Object> summary) {
    }

    /** 10.7 — voucher của shop người gọi. */
    @Transactional(readOnly = true)
    public Page shopPage(long me, String status, Integer page, Integer size) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Page p = page(me, parseStatus(status), page, size, now);
        ZonedDateTime vn = now.atZoneSameInstant(VN);
        OffsetDateTime monthStart = vn.withDayOfMonth(1).toLocalDate().atStartOfDay(VN).toOffsetDateTime();
        VoucherReadModel.ShopMonthStats stats = readModel.shopMonthStats(me, monthStart, monthStart.plusMonths(1));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("active_count", readModel.count(me, VoucherStatus.ACTIVE, now));
        summary.put("usage_this_month", stats.usage());
        summary.put("revenue_from_promotions_this_month", stats.revenue());
        return new Page(p.rows(), p.pagination(), summary);
    }

    /** 11.14 — voucher sàn + freeship. */
    @Transactional(readOnly = true)
    public Page platformPage(String status, Integer page, Integer size) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Page p = page(null, parseStatus(status), page, size, now);
        VoucherReadModel.PlatformStats stats = readModel.platformStats(now);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("active_count", readModel.count(null, VoucherStatus.ACTIVE, now));
        summary.put("total_usage", stats.totalUsage());
        summary.put("total_discount_used", stats.totalDiscountUsed());
        summary.put("scheduled_count", readModel.count(null, VoucherStatus.SCHEDULED, now));
        summary.put("remaining_budget", stats.remainingBudget());
        return new Page(p.rows(), p.pagination(), summary);
    }

    private Page page(Long vendorUserId, VoucherStatus status, Integer page, Integer size, OffsetDateTime now) {
        int pg = page == null ? 1 : page;
        int sz = size == null ? DEFAULT_SIZE : size;
        if (pg < 1) throw InvalidRequestException.of("page", "OUT_OF_RANGE", page, "page bắt đầu từ 1.");
        if (sz < 1 || sz > MAX_SIZE) throw InvalidRequestException.of("size", "OUT_OF_RANGE", size, "size từ 1 đến 50.");
        VoucherReadModel.IdPage ids = readModel.managementPage(vendorUserId, status, now, pg - 1, sz);
        Map<Long, Voucher> byId = vouchers.findAllById(ids.ids()).stream()
                .collect(Collectors.toMap(Voucher::getId, Function.identity()));
        Set<String> coinCodes = management.coinRewardCodes();
        List<VoucherAdminViews.Row> rows = ids.ids().stream().map(byId::get).filter(java.util.Objects::nonNull)
                .map(v -> VoucherAdminViews.row(v, now, coinCodes.contains(v.getCode().toUpperCase()))).toList();
        return new Page(rows, PageResponse.Pagination.of(pg, sz, ids.total()), null);
    }

    static VoucherStatus parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return null;
        try {
            return VoucherStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidRequestException.of("status", "INVALID", status, "status ∈ ACTIVE, SCHEDULED, ENDED, DISABLED.");
        }
    }
}
