// Kho Voucher của người mua (API_SPEC 3.14; UC-PRO-03; plan 2026-11-26 voucher V05.03, D14, D16)
package com.genie.promotion.voucher.application;

import com.genie.common.api.PageResponse;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.promotion.voucher.domain.Voucher;
import com.genie.promotion.voucher.domain.VoucherRepository;
import com.genie.promotion.voucher.domain.VoucherStatusSql;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lưu bằng mã hoặc id: voucher đang chạy / sắp diễn ra, không phải quà đổi Điểm, shop còn hoạt động — ngược lại
 * {@code 404 GNE-VCH-4041} (không lộ voucher ẩn). {@code INSERT … ON CONFLICT DO NOTHING}: bấm hai lần chỉ một dòng.
 */
@Service
public class VoucherWalletService {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 50;
    static final List<String> TYPES = List.of("PLATFORM", "SHOP", "FREESHIP");

    private final NamedParameterJdbcTemplate jdbc;
    private final VoucherRepository vouchers;
    private final VoucherReadModel readModel;
    private final VoucherViews views;
    private final PublicVoucherService publicVouchers;
    private final Clock clock;

    public VoucherWalletService(NamedParameterJdbcTemplate jdbc, VoucherRepository vouchers, VoucherReadModel readModel,
                                VoucherViews views, PublicVoucherService publicVouchers, Clock clock) {
        this.jdbc = jdbc;
        this.vouchers = vouchers;
        this.readModel = readModel;
        this.views = views;
        this.publicVouchers = publicVouchers;
        this.clock = clock;
    }

    /** {@code POST /me/vouchers} — {@code code} hoặc {@code voucherId}. */
    @Transactional
    public VoucherView save(long me, String code, Long voucherId) {
        if ((code == null || code.isBlank()) == (voucherId == null)) {
            throw InvalidRequestException.of("code", "ONE_OF", code, "Nhập mã voucher hoặc chọn voucher.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        Optional<Voucher> found = voucherId != null ? vouchers.findById(voucherId)
                : vouchers.findByCode(code.trim().toUpperCase(Locale.ROOT));
        Voucher v = found.filter(x -> publicVouchers.publiclyVisible(x, now))
                .orElseThrow(() -> new BusinessException(ErrorCode.VOUCHER_NOT_FOUND));
        VoucherReadModel.Usage used = readModel.usagesOf(me, List.of(v.getId())).get(v.getId());
        if (used != null && used.count() >= v.getUsagePerUser()) {
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_SAVED, "Bạn đã dùng hết lượt của voucher này.",
                    Map.of("reason", "USAGE_LIMIT_REACHED"));
        }
        int inserted = jdbc.update("""
                INSERT INTO user_vouchers (user_id, voucher_id, created_at, is_used) VALUES (:uid, :vid, :now, FALSE)
                ON CONFLICT (user_id, voucher_id) DO NOTHING""",
                new MapSqlParameterSource("uid", me).addValue("vid", v.getId()).addValue("now", VoucherReadModel.ts(now)));
        if (inserted == 0) {
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_SAVED);
        }
        return views.of(v, now, me);
    }

    /** {@code DELETE /me/vouchers/{id}} — idempotent. */
    @Transactional
    public void remove(long me, long voucherId) {
        jdbc.update("DELETE FROM user_vouchers WHERE user_id = :uid AND voucher_id = :vid",
                new MapSqlParameterSource("uid", me).addValue("vid", voucherId));
    }

    public record WalletPage(List<VoucherView> items, PageResponse.Pagination pagination, Map<String, Object> summary) {
    }

    /** {@code GET /me/vouchers?state&type&page&size} (D16). */
    @Transactional(readOnly = true)
    public WalletPage page(long me, String state, String type, Integer page, Integer size) {
        String st = state == null || state.isBlank() ? "AVAILABLE" : state.trim().toUpperCase(Locale.ROOT);
        if (!List.of("AVAILABLE", "USED", "EXPIRED").contains(st)) {
            throw InvalidRequestException.of("state", "INVALID", state, "state ∈ AVAILABLE, USED, EXPIRED.");
        }
        String tp = type == null || type.isBlank() || type.equalsIgnoreCase("ALL") ? null : type.trim().toUpperCase(Locale.ROOT);
        if (tp != null && !TYPES.contains(tp)) {
            throw InvalidRequestException.of("type", "INVALID", type, "type ∈ PLATFORM, SHOP, FREESHIP.");
        }
        int pg = page == null ? 1 : page;
        int sz = size == null ? DEFAULT_SIZE : size;
        if (pg < 1 || sz < 1 || sz > MAX_SIZE) {
            throw InvalidRequestException.of("size", "OUT_OF_RANGE", size, "page từ 1, size 1–50.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        MapSqlParameterSource p = new MapSqlParameterSource("uid", me).addValue("now", VoucherReadModel.ts(now))
                .addValue("limit", sz).addValue("offset", (long) (pg - 1) * sz);
        String used = "((SELECT count(*) FROM voucher_usages u WHERE u.voucher_id = v.id AND u.user_id = :uid) >= v.usage_per_user)";
        String live = "(" + VoucherStatusSql.ACTIVE + " OR " + VoucherStatusSql.SCHEDULED + ")";
        String stateSql = switch (st) {
            case "AVAILABLE" -> "NOT " + used + " AND " + live;
            case "USED" -> used;
            default -> "NOT " + used + " AND NOT " + live;
        };
        String base = "FROM user_vouchers uv JOIN vouchers v ON v.id = uv.voucher_id WHERE uv.user_id = :uid AND " + stateSql;
        String order = st.equals("AVAILABLE")
                ? "CASE WHEN " + VoucherStatusSql.ACTIVE + " THEN 0 ELSE 1 END, v.valid_to ASC, v.id DESC"
                : "v.valid_to DESC, v.id DESC";

        Map<String, Object> counts = new LinkedHashMap<>();
        jdbc.query("SELECT count(*) AS total, count(*) FILTER (WHERE v.type = 'PLATFORM') AS platform, "
                + "count(*) FILTER (WHERE v.type = 'SHOP') AS shop, "
                + "count(*) FILTER (WHERE v.type = 'FREESHIP' OR v.discount_type = 'FREESHIP') AS freeship " + base, p, rs -> {
            counts.put("ALL", rs.getLong("total"));
            counts.put("PLATFORM", rs.getLong("platform"));
            counts.put("SHOP", rs.getLong("shop"));
            counts.put("FREESHIP", rs.getLong("freeship"));
        });
        String typeSql = tp == null ? "" : tp.equals("FREESHIP") ? " AND (v.type = 'FREESHIP' OR v.discount_type = 'FREESHIP')"
                : " AND v.type = '" + tp + "'";
        List<Long> ids = jdbc.queryForList("SELECT v.id " + base + typeSql + " ORDER BY " + order
                + " LIMIT :limit OFFSET :offset", p, Long.class);
        long total = ((Number) counts.getOrDefault(tp == null ? "ALL" : tp, 0L)).longValue();
        Map<Long, Voucher> byId = vouchers.findAllById(ids).stream()
                .collect(Collectors.toMap(Voucher::getId, Function.identity()));
        List<Voucher> list = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return new WalletPage(views.of(list, now, me), PageResponse.Pagination.of(pg, sz, total),
                Map.of("counts", counts));
    }
}
