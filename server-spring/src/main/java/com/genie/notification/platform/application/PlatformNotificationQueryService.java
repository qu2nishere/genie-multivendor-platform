// Bảng / chi tiết / thống kê của màn Admin › Thông báo sàn — UC-NOT-05 bước 5 (API_SPEC 11.17; plan 2026-11-26 thông báo N06.07)
package com.genie.notification.platform.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.html.HtmlSanitizer;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import com.genie.notification.inbox.application.NotificationViews;
import com.genie.notification.platform.domain.PlatformNotification;
import com.genie.notification.platform.domain.PlatformNotificationTarget;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformNotificationQueryService {

    public static final int DEFAULT_SIZE = 5;
    static final int PREVIEW_CHARS = 160;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final NamedParameterJdbcTemplate jdbc;
    private final PlatformAudience audience;
    private final UploadService uploads;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PlatformNotificationQueryService(NamedParameterJdbcTemplate jdbc, PlatformAudience audience,
                                            UploadService uploads, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.audience = audience;
        this.uploads = uploads;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    private static final String SELECT = "SELECT p.id, p.title, p.content, p.image_url, p.target, p.send_at,"
            + " p.dispatched_at, p.reach_count, p.created_by,"
            + " (SELECT count(*) FROM user_platform_notification_reads r WHERE r.platform_notification_id = p.id) AS reads,"
            + " COALESCE(ap.full_name, u.email, 'Admin') AS creator"
            + " FROM platform_notifications p LEFT JOIN users u ON u.id = p.created_by"
            + " LEFT JOIN admin_profiles ap ON ap.user_id = p.created_by";

    /** Danh sách + {@code summary} (tháng này theo giờ VN). */
    public record ListResult(Page<PlatformNotificationViews.Row> page, Map<String, Object> summary) {
    }

    @Transactional(readOnly = true)
    public ListResult list(String target, String q, Integer page, Integer size) {
        int p = page == null ? 1 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 1) {
            throw InvalidRequestException.of("page", "OUT_OF_RANGE", page, "page phải ≥ 1.");
        }
        if (s < 1 || s > 50) {
            throw InvalidRequestException.of("size", "OUT_OF_RANGE", size, "size phải từ 1 đến 50.");
        }
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (target != null && !target.isBlank()) {
            try {
                params.addValue("target", PlatformNotificationTarget.valueOf(target.trim()).name());
            } catch (IllegalArgumentException e) {
                throw InvalidRequestException.of("target", "INVALID_VALUE", target, "Đối tượng nhận không hợp lệ.");
            }
            where.append(" AND p.target = :target");
        }
        if (q != null && !q.isBlank()) {
            params.addValue("q", "%" + q.trim().toLowerCase() + "%");
            where.append(" AND (immutable_unaccent(lower(p.title)) LIKE immutable_unaccent(:q)"
                    + " OR immutable_unaccent(lower(p.content)) LIKE immutable_unaccent(:q))");
        }
        Long total = jdbc.queryForObject("SELECT count(*) FROM platform_notifications p" + where, params, Long.class);
        params.addValue("limit", s).addValue("offset", (long) (p - 1) * s);
        List<PlatformNotificationViews.Row> rows = jdbc.query(SELECT + where
                        + " ORDER BY p.created_at DESC, p.id DESC LIMIT :limit OFFSET :offset", params,
                (rs, i) -> row(rs));
        Page<PlatformNotificationViews.Row> result = new PageImpl<>(rows, PageRequest.of(p - 1, s),
                total == null ? 0 : total);
        return new ListResult(result, summary());
    }

    @Transactional(readOnly = true)
    public PlatformNotificationViews.Detail detail(long id) {
        List<PlatformNotificationViews.Detail> found = jdbc.query(SELECT + " WHERE p.id = :id",
                new MapSqlParameterSource("id", id), (rs, i) -> detailOf(rs));
        if (found.isEmpty()) {
            throw new com.genie.common.error.BusinessException(com.genie.common.error.ErrorCode.NOT_FOUND,
                    "Không tìm thấy thông báo.");
        }
        return found.get(0);
    }

    /** Sau khi ghi (trong cùng transaction — đã flush). */
    PlatformNotificationViews.Detail detail(PlatformNotification n) {
        return detail(n.getId());
    }

    private Map<String, Object> summary() {
        OffsetDateTime now = OffsetDateTime.now(clock).atZoneSameInstant(VN).toOffsetDateTime();
        OffsetDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay(VN).toOffsetDateTime();
        MapSqlParameterSource p = new MapSqlParameterSource("from", Timestamp.from(monthStart.toInstant()))
                .addValue("to", Timestamp.from(monthStart.plusMonths(1).toInstant()));
        Map<String, Object> row = jdbc.queryForMap("SELECT count(*) AS sent, COALESCE(sum(p.reach_count), 0) AS reach,"
                + " COALESCE(sum((SELECT count(*) FROM user_platform_notification_reads r"
                + " WHERE r.platform_notification_id = p.id)), 0) AS reads"
                + " FROM platform_notifications p WHERE p.dispatched_at >= :from AND p.dispatched_at < :to", p);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sent_this_month", ((Number) row.get("sent")).longValue());
        out.put("total_reach", ((Number) row.get("reach")).longValue());
        out.put("total_reads", ((Number) row.get("reads")).longValue());
        return out;
    }

    private PlatformNotificationViews.Row row(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        OffsetDateTime dispatched = time(rs, "dispatched_at");
        OffsetDateTime sendAt = time(rs, "send_at");
        return new PlatformNotificationViews.Row(id, PlatformNotificationViews.code(id), rs.getString("title"),
                HtmlSanitizer.plainPreview(rs.getString("content"), PREVIEW_CHARS), imageUrl(rs.getString("image_url")),
                rs.getString("target"), rs.getString("creator") + " (" + rs.getLong("created_by") + ")",
                NotificationViews.vn(dispatched != null ? dispatched : sendAt), dispatched == null,
                rs.getLong("reads"), rs.getLong("reach_count"));
    }

    private PlatformNotificationViews.Detail detailOf(ResultSet rs) throws SQLException {
        PlatformNotificationViews.Row r = row(rs);
        List<String> emails = List.of();
        if (PlatformNotificationTarget.SPECIFIC_USERS.name().equals(r.target())) {
            emails = audience.emailsOf(ids(jdbc.queryForObject(
                    "SELECT CAST(recipient_user_ids_json AS TEXT) FROM platform_notifications WHERE id = :id",
                    new MapSqlParameterSource("id", r.id()), String.class)));
        }
        return new PlatformNotificationViews.Detail(r.id(), r.code(), r.title(), r.contentPreview(), r.imageUrl(),
                r.target(), r.createdBy(), r.sendAt(), r.isScheduled(), r.readCount(), r.reachCount(),
                rs.getString("content"), emails);
    }

    /** Ảnh lưu khóa đối tượng → URL công khai; giá trị đã là URL (dữ liệu cũ) thì giữ. */
    String imageUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.startsWith("http") ? key : uploads.publicUrl(UploadPurpose.NOTIFICATION_IMAGE, key);
    }

    private List<Long> ids(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static OffsetDateTime time(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toInstant().atOffset(ZoneOffset.UTC);
    }
}
