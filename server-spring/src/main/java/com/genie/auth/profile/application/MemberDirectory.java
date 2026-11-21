package com.genie.auth.profile.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh bạ người dùng sàn cho màn quản trị {@code /admin/users} (UC-ADM-03 — plan B1 mục 3.6, R05): chỉ tài khoản
 * {@code CUSTOMER} / {@code VENDOR} chưa xóa (nhân viên quản lý ở 11.18). Đọc thẳng {@code users} +
 * {@code customer_profiles} bằng SQL (read-model, tìm không dấu qua {@code immutable_unaccent}).
 */
@Service
public class MemberDirectory {

    /** Tab màn hình: Tất cả / Khách hàng / Shop / Đang khóa. */
    public enum Tab { ALL, CUSTOMER, VENDOR, LOCKED }

    /** Một dòng người dùng — trạng thái gốc của {@code users}, module {@code admin} tự ánh xạ sang trạng thái API. */
    public record Member(long id, String email, String role, String status, OffsetDateTime lockedUntil,
                         OffsetDateTime suspendedUntil, String suspensionReason, String fullName, String phoneNumber) {
    }

    /** {@code counts}: số người theo từng tab (không áp từ khóa — giống màn mock). */
    public record MemberPage(List<Member> items, long total, Map<String, Long> counts) {
    }

    private static final String FROM = " FROM users u LEFT JOIN customer_profiles cp ON cp.user_id = u.id"
            + " WHERE u.role IN ('CUSTOMER', 'VENDOR') AND u.status NOT IN ('ANONYMIZED', 'DELETED')";
    private static final String LOCKED = "(u.status IN ('LOCKED', 'SUSPENDED') OR u.locked_until > :now)";
    private static final String COLUMNS = "SELECT u.id, u.email, u.role, u.status, u.locked_until, u.suspended_until,"
            + " u.suspension_reason, cp.full_name, cp.phone_number";

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public MemberDirectory(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * Tìm theo họ tên (không dấu), email, số điện thoại (chỉ khi từ khóa là dãy ≥ 3 chữ số) hoặc
     * {@code extraUserIds} (chủ shop khớp tên shop — module {@code admin} lấy qua cổng). Mới tạo trước.
     */
    @Transactional(readOnly = true)
    public MemberPage search(Tab tab, String q, Collection<Long> extraUserIds, int page, int size) {
        MapSqlParameterSource p = new MapSqlParameterSource("now", OffsetDateTime.now(clock));
        StringBuilder where = new StringBuilder(FROM);
        switch (tab) {
            case CUSTOMER -> where.append(" AND u.role = 'CUSTOMER'");
            case VENDOR -> where.append(" AND u.role = 'VENDOR'");
            case LOCKED -> where.append(" AND ").append(LOCKED);
            default -> { }
        }
        String needle = q == null ? "" : q.trim();
        if (!needle.isEmpty()) {
            p.addValue("q", needle);
            where.append(" AND (position(immutable_unaccent(lower(:q)) in immutable_unaccent(lower(coalesce(cp.full_name, '')))) > 0")
                    .append(" OR position(lower(:q) in lower(u.email)) > 0");
            String digits = needle.replaceAll("[\\s.+()-]", "");
            if (digits.matches("\\d{3,}")) {
                p.addValue("digits", digits);
                where.append(" OR position(:digits in regexp_replace(coalesce(cp.phone_number, ''), '\\D', '', 'g')) > 0");
            }
            if (extraUserIds != null && !extraUserIds.isEmpty()) {
                p.addValue("extra", extraUserIds);
                where.append(" OR u.id IN (:extra)");
            }
            where.append(')');
        }
        Long total = jdbc.queryForObject("SELECT count(*)" + where, p, Long.class);
        p.addValue("limit", size).addValue("offset", (long) (page - 1) * size);
        List<Member> items = jdbc.query(COLUMNS + where + " ORDER BY u.created_at DESC, u.id DESC LIMIT :limit OFFSET :offset",
                p, MemberDirectory::member);
        return new MemberPage(items, total == null ? 0 : total, counts());
    }

    /** Người dùng sàn theo id (không trả nhân viên / tài khoản đã xóa — đích như vậy coi như không tồn tại). */
    @Transactional(readOnly = true)
    public Optional<Member> find(long userId) {
        return jdbc.query(COLUMNS + FROM + " AND u.id = :id", new MapSqlParameterSource("id", userId),
                MemberDirectory::member).stream().findFirst();
    }

    private Map<String, Long> counts() {
        return jdbc.queryForObject("SELECT count(*) AS all_count,"
                        + " count(*) FILTER (WHERE u.role = 'CUSTOMER') AS customer_count,"
                        + " count(*) FILTER (WHERE u.role = 'VENDOR') AS vendor_count,"
                        + " count(*) FILTER (WHERE " + LOCKED + ") AS locked_count" + FROM,
                new MapSqlParameterSource("now", OffsetDateTime.now(clock)), (rs, i) -> {
                    Map<String, Long> m = new LinkedHashMap<>();
                    m.put(Tab.ALL.name(), rs.getLong("all_count"));
                    m.put(Tab.CUSTOMER.name(), rs.getLong("customer_count"));
                    m.put(Tab.VENDOR.name(), rs.getLong("vendor_count"));
                    m.put(Tab.LOCKED.name(), rs.getLong("locked_count"));
                    return m;
                });
    }

    private static Member member(ResultSet rs, int row) throws SQLException {
        return new Member(rs.getLong("id"), rs.getString("email"), rs.getString("role"), rs.getString("status"),
                rs.getObject("locked_until", OffsetDateTime.class), rs.getObject("suspended_until", OffsetDateTime.class),
                rs.getString("suspension_reason"), rs.getString("full_name"), rs.getString("phone_number"));
    }
}
