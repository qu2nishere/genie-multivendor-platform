// Đánh dấu đã đọc một / tất cả — API_SPEC 3.12 (plan 2026-11-26 thông báo D5, D7, D9, N04.06)
package com.genie.notification.inbox.application;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.realtime.UserChannel;
import com.genie.notification.admin.domain.AdminNotificationType;
import com.genie.notification.inbox.domain.NotificationCategory;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IDOR (N6): {@code ntf_} lọc {@code user_id}; {@code pn_} phải thuộc đối tượng nhận của người gọi; {@code an_} phải là loại
 * người gọi được xem. Không thỏa → {@code 404 GNE-SYS-4040} (không lộ thông báo có tồn tại). Đánh dấu lại cái đã đọc vẫn
 * thành công (idempotent). Sau commit gửi {@code NOTIFICATIONS_READ} cho chính người đó (đồng bộ tab / thiết bị khác).
 */
@Service
public class InboxReadService {

    private final NamedParameterJdbcTemplate jdbc;
    private final InboxReadModel readModel;
    private final UserChannel channel;
    private final Clock clock;

    public InboxReadService(NamedParameterJdbcTemplate jdbc, InboxReadModel readModel, UserChannel channel, Clock clock) {
        this.jdbc = jdbc;
        this.readModel = readModel;
        this.channel = channel;
        this.clock = clock;
    }

    @Transactional
    public void markRead(long userId, String prefixedId) {
        Viewer v = readModel.viewer(userId).orElseThrow(InboxReadService::notFound);
        Timestamp now = Timestamp.from(Instant.now(clock));
        if (prefixedId.startsWith(NotificationView.PERSONAL_PREFIX)) {
            long id = idOf(prefixedId, NotificationView.PERSONAL_PREFIX);
            MapSqlParameterSource p = new MapSqlParameterSource("id", id).addValue("uid", userId).addValue("now", now);
            int n = jdbc.update("UPDATE notifications SET is_read = TRUE, read_at = COALESCE(read_at, :now)"
                    + " WHERE id = :id AND user_id = :uid", p);
            if (n == 0) {
                throw notFound();
            }
        } else if (prefixedId.startsWith(NotificationView.PLATFORM_PREFIX)) {
            long id = idOf(prefixedId, NotificationView.PLATFORM_PREFIX);
            if (!v.receivesPlatform()) {
                throw notFound();
            }
            MapSqlParameterSource p = readModel.platformParams(v).addValue("id", id).addValue("now", now);
            int n = jdbc.update("INSERT INTO user_platform_notification_reads (user_id, platform_notification_id, read_at)"
                    + " SELECT :uid, p.id, :now FROM platform_notifications p WHERE p.id = :id AND "
                    + InboxReadModel.PLATFORM_VISIBLE + " ON CONFLICT DO NOTHING", p);
            if (n == 0 && !platformVisible(p)) {
                throw notFound();
            }
        } else if (prefixedId.startsWith(NotificationView.ADMIN_PREFIX)) {
            long id = idOf(prefixedId, NotificationView.ADMIN_PREFIX);
            Set<AdminNotificationType> types = v.adminTypes();
            if (types.isEmpty()) {
                throw notFound();
            }
            int n = jdbc.update("UPDATE admin_notifications SET is_read = TRUE, updated_at = :now"
                    + " WHERE id = :id AND type IN (:types)", new MapSqlParameterSource("id", id).addValue("now", now)
                    .addValue("types", types.stream().map(Enum::name).toList()));
            if (n == 0) {
                throw notFound();
            }
        } else {
            throw notFound();
        }
        pushRead(v, List.of(prefixedId), null);
    }

    @Transactional
    public void markAllRead(long userId, NotificationCategory category) {
        Viewer v = readModel.viewer(userId).orElseThrow(InboxReadService::notFound);
        Timestamp now = Timestamp.from(Instant.now(clock));
        MapSqlParameterSource p = new MapSqlParameterSource("uid", userId).addValue("now", now);
        String typeFilter = "";
        if (category != null) {
            typeFilter = " AND type IN (:types)";
            p.addValue("types", category.types().stream().map(Enum::name).toList());
        }
        jdbc.update("UPDATE notifications SET is_read = TRUE, read_at = :now WHERE user_id = :uid AND is_read = FALSE"
                + typeFilter, p);
        if (NotificationCategory.includesSystemSources(category)) {
            if (v.receivesPlatform()) {
                jdbc.update("INSERT INTO user_platform_notification_reads (user_id, platform_notification_id, read_at)"
                        + " SELECT :uid, p.id, :now FROM platform_notifications p WHERE " + InboxReadModel.PLATFORM_VISIBLE
                        + " ON CONFLICT DO NOTHING", readModel.platformParams(v).addValue("now", now));
            }
            Set<AdminNotificationType> types = v.adminTypes();
            if (!types.isEmpty()) {
                jdbc.update("UPDATE admin_notifications SET is_read = TRUE, updated_at = :now"
                        + " WHERE is_read = FALSE AND type IN (:types)", new MapSqlParameterSource("now", now)
                        .addValue("types", types.stream().map(Enum::name).toList()));
            }
        }
        pushRead(v, null, category == null ? null : category.name());
    }

    private boolean platformVisible(MapSqlParameterSource p) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM platform_notifications p WHERE p.id = :id AND "
                + InboxReadModel.PLATFORM_VISIBLE, p, Integer.class);
        return n != null && n > 0;
    }

    private void pushRead(Viewer v, List<String> ids, String category) {
        long unread = readModel.unreadCount(v);
        channel.sendAfterCommit(v.userId(), UserChannel.NOTIFICATIONS, NotificationEvent.read(ids, category, unread));
    }

    private static long idOf(String prefixedId, String prefix) {
        try {
            long id = Long.parseLong(prefixedId.substring(prefix.length()));
            if (id <= 0) {
                throw notFound();
            }
            return id;
        } catch (NumberFormatException e) {
            throw notFound();
        }
    }

    private static BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy thông báo.");
    }
}
