package com.genie.common.audit;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Điều kiện đọc {@code system_logs} cho màn Nhật ký hệ thống (UC-ADM-07; plan nhật ký kiểm toán L02, D5, D7).
 * Mọi tham số đi qua Criteria API (tham số ràng buộc) — không nối chuỗi SQL.
 */
public final class SystemLogSpecs {

    /** Thứ tự keyset: mới nhất trước, hòa thời điểm thì theo id giảm dần. */
    public static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private SystemLogSpecs() {
    }

    /**
     * Bộ lọc màn Nhật ký.
     *
     * @param from    bao gồm
     * @param to      loại trừ
     * @param actions tập hành động (nhóm); {@code null} = mọi hành động
     * @param q       tìm không phân biệt hoa thường trong email / IP / mã tham chiếu; {@code null} = bỏ qua
     */
    public static Specification<SystemLog> filter(OffsetDateTime from, OffsetDateTime to, Set<AuditAction> actions,
                                                  String q) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            p.add(cb.lessThan(root.get("createdAt"), to));
            if (actions != null) {
                p.add(root.get("actionType").in(actions));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("userEmail")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("ipAddress"), "")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("referenceId"), "")), pattern, '\\')));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** Sau con trỏ ({@code createdAt}, {@code id}) theo thứ tự {@link #NEWEST_FIRST}. */
    public static Specification<SystemLog> after(OffsetDateTime createdAt, String id) {
        return (root, query, cb) -> cb.or(
                cb.lessThan(root.get("createdAt"), createdAt),
                cb.and(cb.equal(root.get("createdAt"), createdAt), cb.lessThan(root.get("id"), id)));
    }

    /** Chỉ dòng truy cập bị từ chối ({@code summary.denied_count}). */
    public static Specification<SystemLog> denied() {
        return (root, query, cb) -> cb.equal(root.get("actionType"), AuditAction.ACCESS_DENIED);
    }

    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
