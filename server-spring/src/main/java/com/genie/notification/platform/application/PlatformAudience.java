// Đối tượng nhận của thông báo toàn sàn (UC-NOT-05; plan 2026-11-26 thông báo D1, D3, D4, N06.02)
package com.genie.notification.platform.application;

import com.genie.notification.platform.domain.PlatformNotificationTarget;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Người nhận = tài khoản {@code ACTIVE} vai trò {@code CUSTOMER} / {@code VENDOR} (nhân viên / Admin không nhận — D1),
 * tạo trước thời điểm phát (D3), thuộc {@code target}. Read-model SQL thẳng bảng {@code users} (module lá — README mục 3).
 */
@Component
public class PlatformAudience {

    /** Điều kiện chung trên bảng {@code users u}; tham số {@code :target}, {@code :ids}, {@code :at}. */
    private static final String MATCH = "u.status = 'ACTIVE' AND u.role IN ('CUSTOMER', 'VENDOR') AND u.created_at <= :at"
            + " AND (:target = 'ALL' OR (:target IN ('CUSTOMER', 'VENDOR') AND u.role = :target)"
            + " OR (:target = 'SPECIFIC_USERS' AND u.id IN (:ids)))";

    private final NamedParameterJdbcTemplate jdbc;

    public PlatformAudience(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Kết quả đổi email → id (D1): email không thuộc khách / shop đang hoạt động nằm ở {@code unknown}. */
    public record Resolved(List<Long> ids, List<String> unknown) {
    }

    public Resolved resolveEmails(Collection<String> emails) {
        Map<String, String> wanted = new LinkedHashMap<>();
        for (String e : emails) {
            if (e != null && !e.isBlank()) {
                wanted.putIfAbsent(e.trim().toLowerCase(Locale.ROOT), e.trim());
            }
        }
        if (wanted.isEmpty()) {
            return new Resolved(List.of(), List.of());
        }
        Map<String, Long> found = new LinkedHashMap<>();
        jdbc.query("SELECT id, lower(email) AS email FROM users WHERE lower(email) IN (:emails)"
                        + " AND status = 'ACTIVE' AND role IN ('CUSTOMER', 'VENDOR')",
                new MapSqlParameterSource("emails", wanted.keySet()),
                rs -> {
                    found.put(rs.getString("email"), rs.getLong("id"));
                });
        List<Long> ids = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (Map.Entry<String, String> w : wanted.entrySet()) {
            Long id = found.get(w.getKey());
            if (id == null) {
                unknown.add(w.getValue());
            } else {
                ids.add(id);
            }
        }
        return new Resolved(ids, unknown);
    }

    /** Email của người nhận cụ thể (màn chi tiết). */
    public List<String> emailsOf(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.queryForList("SELECT email FROM users WHERE id IN (:ids) ORDER BY email",
                new MapSqlParameterSource("ids", ids), String.class);
    }

    /** Số người nhận tại thời điểm {@code at} (D3). */
    public long count(PlatformNotificationTarget target, List<Long> recipientIds, OffsetDateTime at) {
        if (target == PlatformNotificationTarget.SPECIFIC_USERS && recipientIds.isEmpty()) {
            return 0;
        }
        Long n = jdbc.queryForObject("SELECT count(*) FROM users u WHERE " + MATCH, params(target, recipientIds, at),
                Long.class);
        return n == null ? 0 : n;
    }

    /** Những người trong {@code candidates} (vd đang trực tuyến) thuộc đối tượng (D4). */
    public Set<Long> among(Collection<Long> candidates, PlatformNotificationTarget target, List<Long> recipientIds,
                           OffsetDateTime at) {
        if (candidates.isEmpty() || (target == PlatformNotificationTarget.SPECIFIC_USERS && recipientIds.isEmpty())) {
            return Set.of();
        }
        return new HashSet<>(jdbc.queryForList("SELECT u.id FROM users u WHERE u.id IN (:candidates) AND " + MATCH,
                params(target, recipientIds, at).addValue("candidates", candidates), Long.class));
    }

    private static MapSqlParameterSource params(PlatformNotificationTarget target, List<Long> ids, OffsetDateTime at) {
        return new MapSqlParameterSource("target", target.name())
                // IN () rỗng không hợp lệ trong SQL — dùng id không tồn tại
                .addValue("ids", ids.isEmpty() ? List.of(-1L) : ids)
                .addValue("at", Timestamp.from(at.toInstant()));
    }
}
