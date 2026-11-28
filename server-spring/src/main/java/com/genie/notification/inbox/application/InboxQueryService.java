// GET /me/notifications — hộp thư gộp thông báo cá nhân, thông báo sàn, thông báo nhân viên (API_SPEC 3.12; plan 2026-11-26 thông báo D7, D8, N04.05)
package com.genie.notification.inbox.application;

import com.genie.common.api.CursorPage;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.html.HtmlSanitizer;
import com.genie.notification.admin.application.AdminInboxRules;
import com.genie.notification.admin.domain.AdminNotificationType;
import com.genie.notification.inbox.domain.NotificationCategory;
import com.genie.notification.inbox.domain.NotificationType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mỗi nguồn lấy tối đa {@code limit + 1} dòng nằm sau con trỏ (đã sắp {@code thời điểm DESC, id DESC}), trộn theo khóa
 * toàn phần {@link InboxCursor} rồi cắt {@code limit} — không trùng / không sót giữa các trang. Thời điểm của {@code pn_}
 * là lúc phát ({@code dispatched_at}).
 */
@Service
public class InboxQueryService {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;
    static final int PREVIEW_CHARS = 160;

    private final NamedParameterJdbcTemplate jdbc;
    private final InboxReadModel readModel;

    public InboxQueryService(NamedParameterJdbcTemplate jdbc, InboxReadModel readModel) {
        this.jdbc = jdbc;
        this.readModel = readModel;
    }

    /** Trang hộp thư + {@code summary.unread_count}. */
    public record InboxPage(CursorPage<NotificationView> page, long unreadCount) {
    }

    private record Row(NotificationView view, long at, int source, long id) {
    }

    @Transactional(readOnly = true)
    public InboxPage page(long userId, NotificationCategory category, String cursor, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw InvalidRequestException.of("limit", "OUT_OF_RANGE", limit, "limit phải từ 1 đến 50.");
        }
        InboxCursor after = cursor == null || cursor.isBlank() ? null : InboxCursor.decode(cursor);
        Viewer v = viewer(userId);
        int fetch = size + 1;
        List<Row> rows = new ArrayList<>(personal(v, category, after, fetch));
        if (NotificationCategory.includesSystemSources(category)) {
            if (v.receivesPlatform()) {
                rows.addAll(platform(v, after, fetch));
            }
            Set<AdminNotificationType> adminTypes = v.adminTypes();
            if (!adminTypes.isEmpty()) {
                rows.addAll(admin(adminTypes, after, fetch));
            }
        }
        rows.sort(Comparator.comparingLong(Row::at).thenComparingInt(Row::source).thenComparingLong(Row::id).reversed());
        boolean hasMore = rows.size() > size;
        List<Row> pageRows = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Row last = pageRows.get(pageRows.size() - 1);
            next = new InboxCursor(last.at(), last.source(), last.id()).encode();
        }
        List<NotificationView> items = pageRows.stream().map(Row::view).toList();
        return new InboxPage(new CursorPage<>(items, new CursorPage.Pagination(size, hasMore, next)),
                readModel.unreadCount(v));
    }

    Viewer viewer(long userId) {
        return readModel.viewer(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    // ===== Nguồn 1 — thông báo cá nhân =====
    private List<Row> personal(Viewer v, NotificationCategory category, InboxCursor after, int fetch) {
        MapSqlParameterSource p = new MapSqlParameterSource("uid", v.userId()).addValue("n", fetch);
        StringBuilder sql = new StringBuilder("SELECT id, type, title, body, image_url, reference_type, reference_id,"
                + " is_read, created_at FROM notifications WHERE user_id = :uid");
        if (category != null) {
            sql.append(" AND type IN (:types)");
            p.addValue("types", category.types().stream().map(Enum::name).toList());
        }
        sql.append(cursorClause("created_at", "id", InboxCursor.PERSONAL, after, p));
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT :n");
        return jdbc.query(sql.toString(), p, (rs, i) -> {
            NotificationType type = NotificationType.valueOf(rs.getString("type"));
            OffsetDateTime at = time(rs, "created_at");
            long id = rs.getLong("id");
            return new Row(new NotificationView(NotificationView.PERSONAL_PREFIX + id, NotificationCategory.of(type),
                    type.name(), rs.getString("title"), rs.getString("body"), rs.getString("image_url"), null,
                    NotificationViews.vn(at), rs.getBoolean("is_read"),
                    NotificationViews.linkOf(rs.getString("reference_type"), rs.getString("reference_id"))),
                    micros(at), InboxCursor.PERSONAL, id);
        });
    }

    // ===== Nguồn 2 — thông báo sàn (fan-out on read) =====
    private List<Row> platform(Viewer v, InboxCursor after, int fetch) {
        MapSqlParameterSource p = readModel.platformParams(v).addValue("n", fetch);
        String sql = "SELECT p.id, p.title, p.content, p.image_url, p.dispatched_at, (r.user_id IS NOT NULL) AS is_read"
                + " FROM platform_notifications p LEFT JOIN user_platform_notification_reads r"
                + " ON r.platform_notification_id = p.id AND r.user_id = :uid WHERE " + InboxReadModel.PLATFORM_VISIBLE
                + cursorClause("p.dispatched_at", "p.id", InboxCursor.PLATFORM, after, p)
                + " ORDER BY p.dispatched_at DESC, p.id DESC LIMIT :n";
        return jdbc.query(sql, p, (rs, i) -> {
            long id = rs.getLong("id");
            OffsetDateTime at = time(rs, "dispatched_at");
            return new Row(platformView(id, rs.getString("title"), rs.getString("content"), rs.getString("image_url"),
                    at, rs.getBoolean("is_read")), micros(at), InboxCursor.PLATFORM, id);
        });
    }

    /** Phần tử 3.12 của một thông báo sàn — dùng chung cho hộp thư và gói tin realtime. */
    public static NotificationView platformView(long id, String title, String contentHtml, String imageUrl,
                                                OffsetDateTime dispatchedAt, boolean read) {
        return new NotificationView(NotificationView.PLATFORM_PREFIX + id, NotificationCategory.SYSTEM,
                NotificationView.PLATFORM_ANNOUNCEMENT, title, HtmlSanitizer.plainPreview(contentHtml, PREVIEW_CHARS),
                imageUrl, contentHtml, NotificationViews.vn(dispatchedAt), read,
                NotificationLink.of(NotificationLink.PLATFORM_NOTIFICATION, id));
    }

    // ===== Nguồn 3 — thông báo nhân viên (hộp thư chung) =====
    private List<Row> admin(Set<AdminNotificationType> types, InboxCursor after, int fetch) {
        MapSqlParameterSource p = new MapSqlParameterSource("types", types.stream().map(Enum::name).toList())
                .addValue("n", fetch);
        String sql = "SELECT id, type, title, content, image_url, is_read, created_at FROM admin_notifications"
                + " WHERE type IN (:types)" + cursorClause("created_at", "id", InboxCursor.ADMIN, after, p)
                + " ORDER BY created_at DESC, id DESC LIMIT :n";
        return jdbc.query(sql, p, (rs, i) -> {
            long id = rs.getLong("id");
            AdminNotificationType type = AdminNotificationType.valueOf(rs.getString("type"));
            OffsetDateTime at = time(rs, "created_at");
            return new Row(new NotificationView(NotificationView.ADMIN_PREFIX + id, NotificationCategory.SYSTEM,
                    type.name(), rs.getString("title"), rs.getString("content"), rs.getString("image_url"), null,
                    NotificationViews.vn(at), rs.getBoolean("is_read"), AdminInboxRules.linkOf(type)),
                    micros(at), InboxCursor.ADMIN, id);
        });
    }

    /**
     * Điều kiện "nằm sau con trỏ" cho nguồn hạng {@code source} (khóa {@code (at, source, id)} giảm dần): nguồn hạng nhỏ
     * hơn → {@code at <= c.at}; cùng hạng → {@code at < c.at OR (at = c.at AND id < c.id)}; hạng lớn hơn → {@code at < c.at}.
     */
    static String cursorClause(String atCol, String idCol, int source, InboxCursor c, MapSqlParameterSource p) {
        if (c == null) {
            return "";
        }
        p.addValue("cAt", Timestamp.from(fromMicros(c.at()))).addValue("cId", c.id());
        if (source < c.source()) {
            return " AND " + atCol + " <= :cAt";
        }
        if (source == c.source()) {
            return " AND (" + atCol + " < :cAt OR (" + atCol + " = :cAt AND " + idCol + " < :cId))";
        }
        return " AND " + atCol + " < :cAt";
    }

    static long micros(OffsetDateTime t) {
        Instant i = t.toInstant();
        return ChronoUnit.MICROS.between(Instant.EPOCH, i);
    }

    static Instant fromMicros(long micros) {
        return Instant.EPOCH.plus(micros, ChronoUnit.MICROS);
    }

    private static OffsetDateTime time(ResultSet rs, String col) throws SQLException {
        return rs.getTimestamp(col).toInstant().atOffset(ZoneOffset.UTC);
    }
}
