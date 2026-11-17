package com.genie.admin.audit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.admin.staff.domain.AdminProfile;
import com.genie.admin.staff.domain.AdminProfileRepository;
import com.genie.common.api.CursorPage;
import com.genie.common.audit.AuditAction;
import com.genie.common.audit.AuditCategory;
import com.genie.common.audit.SystemLog;
import com.genie.common.audit.SystemLogRepository;
import com.genie.common.audit.SystemLogSpecs;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra cứu nhật ký kiểm toán — UC-ADM-07 (API_SPEC 11.10; plan nhật ký kiểm toán L03). Chỉ đọc {@code system_logs}.
 */
@Service
public class AuditLogQueryService {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final Duration DEFAULT_RANGE = Duration.ofDays(7);
    static final Duration MAX_RANGE = Duration.ofDays(90);
    static final int DEFAULT_LIMIT = 50;
    static final int MAX_LIMIT = 100;
    static final int MAX_Q = 100;
    static final String SYSTEM_ACTOR = "system";
    private static final Pattern DATE_ONLY = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final SystemLogRepository logs;
    private final AdminProfileRepository adminProfiles;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditLogQueryService(SystemLogRepository logs, AdminProfileRepository adminProfiles, ObjectMapper objectMapper,
                                Clock clock) {
        this.logs = logs;
        this.adminProfiles = adminProfiles;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Kết quả trang: dòng + phân trang cursor + {@code denied_count} trong bộ lọc hiện tại. */
    public record SearchResult(CursorPage<AuditLogView> page, long deniedCount) {
    }

    @Transactional(readOnly = true)
    public SearchResult search(AuditLogQuery query) {
        AuditLogFilter f = normalize(query, true);
        Specification<SystemLog> base = f.base();
        Specification<SystemLog> spec = f.cursor() == null ? base
                : base.and(SystemLogSpecs.after(f.cursor().createdAt(), f.cursor().id()));
        List<SystemLog> rows = logs.findBy(spec, q -> q.sortBy(SystemLogSpecs.NEWEST_FIRST).limit(f.limit() + 1).all());
        boolean hasMore = rows.size() > f.limit();
        List<SystemLog> pageRows = hasMore ? rows.subList(0, f.limit()) : rows;
        String next = null;
        if (hasMore) {
            SystemLog last = pageRows.get(pageRows.size() - 1);
            next = new AuditCursor(last.getCreatedAt(), last.getId()).encode();
        }
        Map<Long, String> titles = staffTitles(pageRows.stream().filter(r -> "STAFF".equals(r.getUserRole()))
                .map(SystemLog::getUserId).filter(id -> id != null).collect(Collectors.toSet()));
        List<AuditLogView> items = pageRows.stream().map(r -> toView(r, titles)).toList();
        long denied = logs.count(base.and(SystemLogSpecs.denied()));
        return new SearchResult(new CursorPage<>(items, new CursorPage.Pagination(f.limit(), hasMore, next)), denied);
    }

    // ---------- kiểm tham số ----------

    /** Kiểm + chuẩn hóa tham số; gom mọi lỗi trường rồi ném một lần ({@code 422 GNE-VAL-4220}). */
    public AuditLogFilter normalize(AuditLogQuery query, boolean paged) {
        List<Violation> errors = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now(clock).atZoneSameInstant(VN).toOffsetDateTime();
        OffsetDateTime to = parseBound(query.to(), "to", true, errors);
        OffsetDateTime from = parseBound(query.from(), "from", false, errors);
        if (to == null && !hasText(query.to())) {
            to = now;
        }
        if (from == null && !hasText(query.from()) && to != null) {
            from = to.minus(DEFAULT_RANGE);
        }
        if (from != null && to != null) {
            if (!to.isAfter(from)) {
                errors.add(new Violation("to", "INVALID_RANGE", query.to(), "Thời điểm kết thúc phải sau thời điểm bắt đầu."));
            } else if (Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
                errors.add(new Violation("from", "RANGE_TOO_LONG", query.from(),
                        "Mỗi lần chỉ tra cứu tối đa 90 ngày."));
            }
        }
        AuditCategory category = null;
        if (hasText(query.category())) {
            try {
                category = AuditCategory.valueOf(query.category().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                errors.add(new Violation("category", "INVALID_VALUE", query.category(), "Nhóm hành động không hợp lệ."));
            }
        }
        String q = hasText(query.q()) ? query.q().trim() : null;
        if (q != null && q.length() > MAX_Q) {
            errors.add(new Violation("q", "TOO_LONG", null, "Từ khóa tối đa 100 ký tự."));
        }
        int limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        if (paged && (limit < 1 || limit > MAX_LIMIT)) {
            errors.add(new Violation("limit", "OUT_OF_RANGE", query.limit(), "limit phải từ 1 đến 100."));
        }
        AuditCursor cursor = null;
        if (paged && hasText(query.cursor())) {
            try {
                cursor = AuditCursor.decode(query.cursor().trim());
            } catch (InvalidRequestException e) {
                errors.addAll(e.getViolations());
            }
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
        return new AuditLogFilter(from, to, category, q, cursor, limit);
    }

    private static OffsetDateTime parseBound(String raw, String field, boolean upper, List<Violation> errors) {
        if (!hasText(raw)) {
            return null;
        }
        String v = raw.trim();
        try {
            if (DATE_ONLY.matcher(v).matches()) {
                LocalDate d = LocalDate.parse(v);
                return (upper ? d.plusDays(1) : d).atStartOfDay(VN).toOffsetDateTime();
            }
            return OffsetDateTime.parse(v);
        } catch (DateTimeParseException e) {
            errors.add(new Violation(field, "INVALID_FORMAT", raw,
                    "Thời điểm phải theo ISO-8601 (2026-06-01 hoặc 2026-06-01T00:00:00+07:00)."));
            return null;
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // ---------- ánh xạ dòng ----------

    /** Chức danh hiện tại của nhân viên (D4) — một truy vấn cho cả trang. */
    Map<Long, String> staffTitles(Collection<Long> staffIds) {
        if (staffIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> out = new HashMap<>();
        for (AdminProfile p : adminProfiles.findByUserIdIn(staffIds)) {
            if (p.getStaffTitle() != null) {
                out.put(p.getUserId(), p.getStaffTitle().name());
            }
        }
        return out;
    }

    /** Mọi chức danh nhân viên (xuất CSV — bảng admin_profiles nhỏ). */
    Map<Long, String> allStaffTitles() {
        Map<Long, String> out = new HashMap<>();
        for (AdminProfile p : adminProfiles.findAll()) {
            if (p.getStaffTitle() != null) {
                out.put(p.getUserId(), p.getStaffTitle().name());
            }
        }
        return out;
    }

    AuditLogView toView(SystemLog r, Map<Long, String> staffTitles) {
        String role = r.getUserRole();
        if ("STAFF".equals(role) && r.getUserId() != null) {
            role = staffTitles.getOrDefault(r.getUserId(), role);
        }
        String actor = r.getUserId() == null ? SYSTEM_ACTOR : r.getUserEmail();
        String result = r.getActionType() == AuditAction.ACCESS_DENIED ? "DENIED" : r.getStatus();
        return new AuditLogView(r.getId(), r.getCreatedAt().atZoneSameInstant(VN).toOffsetDateTime(), actor, role,
                r.getActionType().name(), r.getActionType().category(), target(r), r.getIpAddress(), result);
    }

    /** {@code details.target_label} → {@code reference_id} → {@code null} (D3). */
    String target(SystemLog r) {
        if (r.getDetails() != null) {
            try {
                JsonNode label = objectMapper.readTree(r.getDetails()).get("target_label");
                if (label != null && label.isTextual() && !label.asText().isBlank()) {
                    return label.asText();
                }
            } catch (Exception ignored) {
                // details không phải JSON hợp lệ — dùng reference_id
            }
        }
        return r.getReferenceId();
    }
}
