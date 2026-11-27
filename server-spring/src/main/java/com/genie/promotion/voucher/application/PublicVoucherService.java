// GET /public/vouchers — vé "Lưu voucher" ở trang chủ / trang shop / trang chiến dịch (API_SPEC 5.13; UC-PRO-03; plan D12, D15)
package com.genie.promotion.voucher.application;

import com.genie.common.error.InvalidRequestException;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherRepository;
import com.genie.promotion.voucher.domain.VoucherStatus;
import com.genie.promotion.voucher.domain.VoucherStatusSql;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ voucher đang chạy / sắp diễn ra; không bao giờ trả quà đổi Điểm (D12) hay voucher của shop không còn hoạt động.
 * Danh sách id cache 60 giây ({@link PublicVoucherCache}); {@code state} / {@code is_saved} dựng lại theo thời điểm và người
 * gọi ({@link VoucherViews}). Voucher đổi trạng thái theo thời gian trong 60 giây đó bị lọc lại khi đọc.
 */
@Service
public class PublicVoucherService {

    static final int MAX_FEATURED = 12;
    static final int MAX_CODES = 20;
    private static final String LIVE = "(" + VoucherStatusSql.ACTIVE + " OR " + VoucherStatusSql.SCHEDULED + ")";

    private final NamedParameterJdbcTemplate jdbc;
    private final VoucherRepository vouchers;
    private final VoucherReadModel readModel;
    private final VoucherViews views;
    private final PublicVoucherCache cache;
    private final VoucherManagementService management;
    private final Clock clock;

    public PublicVoucherService(NamedParameterJdbcTemplate jdbc, VoucherRepository vouchers, VoucherReadModel readModel,
                                VoucherViews views, PublicVoucherCache cache, VoucherManagementService management,
                                Clock clock) {
        this.jdbc = jdbc;
        this.vouchers = vouchers;
        this.readModel = readModel;
        this.views = views;
        this.cache = cache;
        this.management = management;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VoucherView> list(Long shopId, String code, String codes, Boolean featured, Integer limit, Long viewerId) {
        int modes = (shopId != null ? 1 : 0) + (code != null && !code.isBlank() ? 1 : 0)
                + (codes != null && !codes.isBlank() ? 1 : 0) + (Boolean.TRUE.equals(featured) ? 1 : 0);
        if (modes != 1) {
            throw InvalidRequestException.of("query", "ONE_OF", null, "Cần đúng một trong shop_id, code, codes, featured.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<Long> ids;
        if (Boolean.TRUE.equals(featured)) {
            int n = limit == null ? 4 : limit;
            if (n < 1 || n > MAX_FEATURED) {
                throw InvalidRequestException.of("limit", "OUT_OF_RANGE", limit, "limit từ 1 đến 12.");
            }
            ids = cache.featured(() -> featuredIds(now)).stream().limit(n).toList();
        } else if (shopId != null) {
            ids = readModel.visibleShopOwner(shopId).map(owner -> cache.shop(shopId, () -> shopIds(owner, now)))
                    .orElse(List.of());
        } else {
            List<String> wanted = (code != null && !code.isBlank() ? List.of(code) : Arrays.asList(codes.split(",")))
                    .stream().map(c -> c.trim().toUpperCase(Locale.ROOT)).filter(c -> !c.isEmpty()).distinct().toList();
            if (wanted.isEmpty() || wanted.size() > MAX_CODES) {
                throw InvalidRequestException.of("codes", "OUT_OF_RANGE", codes, "Tối đa 20 mã.");
            }
            ids = byCodes(wanted, now);
        }
        return views.of(liveInOrder(ids, now), now, viewerId);
    }

    /** Voucher còn hiển thị công khai được (dùng lại ở Kho khi lưu theo id / mã). */
    boolean publiclyVisible(Voucher v, OffsetDateTime now) {
        VoucherStatus st = v.status(now);
        if (st != VoucherStatus.ACTIVE && st != VoucherStatus.SCHEDULED) return false;
        if (management.coinRewardCodes().contains(v.getCode().toUpperCase(Locale.ROOT))) return false;
        return v.getVendorId() == null || !readModel.visibleOwners(List.of(v.getVendorId())).isEmpty();
    }

    private List<Voucher> liveInOrder(List<Long> ids, OffsetDateTime now) {
        if (ids.isEmpty()) return List.of();
        Map<Long, Voucher> byId = vouchers.findAllById(ids).stream()
                .collect(Collectors.toMap(Voucher::getId, Function.identity()));
        Set<String> coin = management.coinRewardCodes();
        return ids.stream().map(byId::get).filter(Objects::nonNull)
                .filter(v -> v.status(now) == VoucherStatus.ACTIVE || v.status(now) == VoucherStatus.SCHEDULED)
                .filter(v -> !coin.contains(v.getCode().toUpperCase(Locale.ROOT))).toList();
    }

    private List<Long> featuredIds(OffsetDateTime now) {
        return jdbc.queryForList("SELECT v.id FROM vouchers v WHERE v.type IN ('PLATFORM', 'FREESHIP') AND " + LIVE
                + " AND upper(v.code) NOT IN (:coin) ORDER BY v.priority DESC, v.valid_to ASC, v.id DESC LIMIT :limit",
                params(now).addValue("limit", MAX_FEATURED), Long.class);
    }

    private List<Long> shopIds(long ownerId, OffsetDateTime now) {
        return jdbc.queryForList("SELECT v.id FROM vouchers v WHERE v.type = 'SHOP' AND v.vendor_id = :vid AND " + LIVE
                + " ORDER BY v.valid_from ASC, v.valid_to ASC, v.id DESC LIMIT 50", params(now).addValue("vid", ownerId),
                Long.class);
    }

    private List<Long> byCodes(List<String> codes, OffsetDateTime now) {
        List<Long> found = jdbc.queryForList("SELECT v.id FROM vouchers v WHERE upper(v.code) IN (:codes) AND " + LIVE
                + " AND upper(v.code) NOT IN (:coin) AND (v.vendor_id IS NULL OR EXISTS (SELECT 1 FROM vendor_profiles vp "
                + "JOIN users u ON u.id = vp.user_id WHERE vp.user_id = v.vendor_id AND vp.kyc_status = 'VERIFIED' "
                + "AND u.status = 'ACTIVE')) ORDER BY v.priority DESC, v.id", params(now).addValue("codes", codes), Long.class);
        return found;
    }

    private MapSqlParameterSource params(OffsetDateTime now) {
        Set<String> coin = management.coinRewardCodes();
        return new MapSqlParameterSource("now", VoucherReadModel.ts(now))
                .addValue("coin", coin.isEmpty() ? List.of("#") : List.copyOf(coin));
    }
}
