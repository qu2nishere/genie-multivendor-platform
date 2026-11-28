// Truy vấn đọc của hộp thư gộp 3 nguồn (API_SPEC 3.12; plan 2026-11-26 thông báo D1, D3, D7, D8)
package com.genie.notification.inbox.application;

import com.genie.common.domain.UserRole;
import com.genie.notification.admin.domain.AdminNotificationType;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Read-model SQL thẳng các bảng {@code users}, {@code staff_permissions} (không import class module {@code auth} /
 * {@code admin} — tiền lệ plan 2026-11-17 D6; giữ {@code notification} là module "lá", README mục 3).
 * <p>Điều kiện "thông báo sàn hiển thị cho người xem" (D1, D3) viết một lần ở {@link #PLATFORM_VISIBLE} và dùng chung
 * cho đếm, phân trang, đánh dấu đọc.
 */
@Component
public class InboxReadModel {

    /** Tham số {@code :uid}, {@code :role}, {@code :createdAt}; bảng thông báo sàn có bí danh {@code p}. */
    static final String PLATFORM_VISIBLE = "p.dispatched_at IS NOT NULL AND p.dispatched_at >= :createdAt"
            + " AND (p.target = 'ALL' OR p.target = :role"
            + " OR (p.target = 'SPECIFIC_USERS' AND p.recipient_user_ids_json @> jsonb_build_array(CAST(:uid AS BIGINT))))";

    private final NamedParameterJdbcTemplate jdbc;

    public InboxReadModel(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Người xem đang {@code ACTIVE}; không tồn tại / đã khóa → rỗng. */
    public Optional<Viewer> viewer(long userId) {
        List<Viewer> rows = jdbc.query("SELECT role, created_at FROM users WHERE id = :uid AND status = 'ACTIVE'",
                new MapSqlParameterSource("uid", userId),
                (rs, i) -> new Viewer(userId, UserRole.valueOf(rs.getString("role")),
                        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC), Set.of()));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Viewer v = rows.get(0);
        if (v.role() == UserRole.STAFF) {
            v = new Viewer(userId, v.role(), v.createdAt(), staffPermissions(userId));
        }
        return Optional.of(v);
    }

    Set<String> staffPermissions(long userId) {
        return new HashSet<>(jdbc.queryForList("SELECT permission FROM staff_permissions"
                + " WHERE staff_user_id = :uid AND revoked_at IS NULL", new MapSqlParameterSource("uid", userId),
                String.class));
    }

    MapSqlParameterSource platformParams(Viewer v) {
        return new MapSqlParameterSource("uid", v.userId())
                .addValue("role", v.role().name())
                .addValue("createdAt", Timestamp.from(v.createdAt().toInstant()));
    }

    /** Tổng chưa đọc của cả 3 nguồn ({@code summary.unread_count}, gói tin realtime). */
    public long unreadCount(Viewer v) {
        long personal = count("SELECT COUNT(*) FROM notifications WHERE user_id = :uid AND is_read = FALSE",
                new MapSqlParameterSource("uid", v.userId()));
        long platform = v.receivesPlatform() ? count("SELECT COUNT(*) FROM platform_notifications p WHERE "
                + PLATFORM_VISIBLE + " AND NOT EXISTS (SELECT 1 FROM user_platform_notification_reads r"
                + " WHERE r.platform_notification_id = p.id AND r.user_id = :uid)", platformParams(v)) : 0;
        long admin = 0;
        Set<AdminNotificationType> types = v.adminTypes();
        if (!types.isEmpty()) {
            admin = count("SELECT COUNT(*) FROM admin_notifications WHERE is_read = FALSE AND type IN (:types)",
                    new MapSqlParameterSource("types", types.stream().map(Enum::name).toList()));
        }
        return personal + platform + admin;
    }

    /** Người đang {@code ACTIVE} là nhân viên được thấy loại {@code type} trong số {@code candidates} (D7). */
    public Set<Long> staffAllowed(Set<Long> candidates, String requiredPermission) {
        if (candidates.isEmpty()) {
            return Set.of();
        }
        String sql = "SELECT u.id FROM users u WHERE u.id IN (:ids) AND u.status = 'ACTIVE'"
                + " AND (u.role = 'ADMIN' OR (u.role = 'STAFF' AND (CAST(:perm AS VARCHAR) IS NULL OR EXISTS ("
                + " SELECT 1 FROM staff_permissions s WHERE s.staff_user_id = u.id AND s.revoked_at IS NULL"
                + " AND s.permission = :perm))))";
        return new HashSet<>(jdbc.queryForList(sql, new MapSqlParameterSource("ids", candidates)
                .addValue("perm", requiredPermission), Long.class));
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long n = jdbc.queryForObject(sql, params, Long.class);
        return n == null ? 0 : n;
    }

    static OffsetDateTime toOffset(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }
}
