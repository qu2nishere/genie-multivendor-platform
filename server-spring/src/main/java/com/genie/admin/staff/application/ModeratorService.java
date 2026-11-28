// Cấu hình nhân viên duyệt sản phẩm — API_SPEC 11.12 "Cấu hình nhân viên duyệt" (UC-CAT-13 bước 7; plan C1 D15)
package com.genie.admin.staff.application;

import com.genie.admin.staff.domain.AdminProfile;
import com.genie.admin.staff.domain.AdminProfileRepository;
import com.genie.catalog.moderation.application.ModeratorPool;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.error.InvalidRequestException.Violation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách = nhân viên STAFF đang ACTIVE có quyền {@code MANAGE_CATALOG} chưa thu hồi; cờ "Đang nhận việc" lưu ở
 * {@code admin_profiles.is_active_moderator} ({@link AdminProfile#toggleModerator}). Cài cổng {@link ModeratorPool} để
 * catalog phân công hàng chờ.
 */
@Service
public class ModeratorService implements ModeratorPool {

    public record AvailabilityItem(Long staffId, Boolean isAccepting) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final AdminProfileRepository profiles;

    public ModeratorService(NamedParameterJdbcTemplate jdbc, AdminProfileRepository profiles) {
        this.jdbc = jdbc;
        this.profiles = profiles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Moderator> moderators() {
        return jdbc.query("""
                SELECT u.id, COALESCE(NULLIF(ap.full_name, ''), u.email) AS name,
                       COALESCE(ap.is_active_moderator, FALSE) AS accepting
                  FROM users u
                  LEFT JOIN admin_profiles ap ON ap.user_id = u.id
                 WHERE u.role = 'STAFF' AND u.status = 'ACTIVE'
                   AND EXISTS (SELECT 1 FROM staff_permissions sp WHERE sp.staff_user_id = u.id
                               AND sp.permission = 'MANAGE_CATALOG' AND sp.revoked_at IS NULL)
                 ORDER BY name, u.id""", new MapSqlParameterSource(),
                (rs, i) -> new Moderator(rs.getLong("id"), rs.getString("name"), rs.getBoolean("accepting")));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> names(Collection<Long> userIds) {
        Map<Long, String> out = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return out;
        jdbc.query("""
                SELECT u.id, COALESCE(NULLIF(ap.full_name, ''), u.email) AS name
                  FROM users u LEFT JOIN admin_profiles ap ON ap.user_id = u.id WHERE u.id IN (:ids)""",
                new MapSqlParameterSource("ids", userIds), rs -> {
                    out.put(rs.getLong("id"), rs.getString("name"));
                });
        return out;
    }

    /** "Lưu cấu hình": bật / tắt nhận việc của nhiều nhân viên trong một lần; người không thuộc danh sách → 422 cả lô. */
    @Transactional
    public List<Moderator> updateAvailability(List<AvailabilityItem> items) {
        if (items == null || items.isEmpty()) {
            throw InvalidRequestException.of("items", "REQUIRED", null, "Chọn ít nhất một nhân viên.");
        }
        Set<Long> eligible = moderators().stream().map(Moderator::staffId).collect(Collectors.toSet());
        List<Violation> v = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            AvailabilityItem it = items.get(i);
            if (it == null || it.staffId() == null || !eligible.contains(it.staffId())) {
                v.add(new Violation("items[" + i + "].staff_id", "NOT_FOUND", it == null ? null : it.staffId(),
                        "Không phải nhân viên kiểm duyệt sản phẩm."));
            } else if (it.isAccepting() == null) {
                v.add(new Violation("items[" + i + "].is_accepting", "REQUIRED", null, "Chọn trạng thái nhận việc."));
            }
        }
        if (!v.isEmpty()) throw new InvalidRequestException(v);
        for (AvailabilityItem it : items) {
            AdminProfile p = profiles.findByUserIdForUpdate(it.staffId()).orElseThrow(() -> InvalidRequestException.of(
                    "items.staff_id", "NOT_FOUND", it.staffId(), "Nhân viên chưa có hồ sơ quản trị."));
            p.toggleModerator(it.isAccepting());
        }
        profiles.flush();
        return moderators();
    }
}
