package com.genie.catalog.category.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cây danh mục dựng một lần từ danh sách phẳng (vài trăm dòng): tra cha, con, cấp, tổ tiên, con cháu, độ cao nhánh,
 * "đang hiển thị" (bản thân và mọi tổ tiên đều bật — plan B2 D5). Bất biến — dựng lại sau mỗi lần cache bị xóa.
 * Anh em sắp theo {@code display_order} (rỗng xuống cuối) rồi theo tên.
 */
public final class CategoryIndex {

    /** Độ sâu tối đa của cây (API 5.1 "cây 3 cấp" — plan B2 D4). */
    public static final int MAX_LEVEL = 3;

    static final Comparator<CategoryRow> ORDER = Comparator
            .comparing((CategoryRow r) -> r.displayOrder() == null ? Integer.MAX_VALUE : r.displayOrder())
            .thenComparing(CategoryRow::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(CategoryRow::id);

    private final Map<Long, CategoryRow> byId = new LinkedHashMap<>();
    private final Map<Long, List<CategoryRow>> children = new HashMap<>();
    private final List<CategoryRow> roots = new ArrayList<>();

    public CategoryIndex(List<CategoryRow> rows) {
        for (CategoryRow r : rows) byId.put(r.id(), r);
        for (CategoryRow r : rows) {
            if (r.parentId() == null || !byId.containsKey(r.parentId())) roots.add(r);
            else children.computeIfAbsent(r.parentId(), k -> new ArrayList<>()).add(r);
        }
        roots.sort(ORDER);
        children.values().forEach(l -> l.sort(ORDER));
    }

    public Optional<CategoryRow> find(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<CategoryRow> findBySlug(String slug) {
        return byId.values().stream().filter(r -> slug.equals(r.slug())).findFirst();
    }

    public List<CategoryRow> all() {
        return List.copyOf(byId.values());
    }

    public List<CategoryRow> roots() {
        return Collections.unmodifiableList(roots);
    }

    public List<CategoryRow> children(long id) {
        return children.getOrDefault(id, List.of());
    }

    /** Tổ tiên từ gốc xuống cha trực tiếp (không gồm chính nó). */
    public List<CategoryRow> ancestors(long id) {
        List<CategoryRow> path = new ArrayList<>();
        CategoryRow cur = byId.get(id);
        int guard = 0;
        while (cur != null && cur.parentId() != null && guard++ < 50) {
            cur = byId.get(cur.parentId());
            if (cur != null) path.add(0, cur);
        }
        return path;
    }

    /** Mọi con cháu (không gồm chính nó). */
    public List<CategoryRow> descendants(long id) {
        List<CategoryRow> out = new ArrayList<>();
        for (CategoryRow c : children(id)) {
            out.add(c);
            out.addAll(descendants(c.id()));
        }
        return out;
    }

    /** Cấp: gốc = 1. */
    public int level(long id) {
        return ancestors(id).size() + 1;
    }

    /** Số cấp bên dưới (lá = 0). */
    public int height(long id) {
        int h = 0;
        for (CategoryRow c : children(id)) h = Math.max(h, 1 + height(c.id()));
        return h;
    }

    /** Bản thân và mọi tổ tiên đều đang bật. */
    public boolean visible(long id) {
        CategoryRow r = byId.get(id);
        if (r == null || !r.active()) return false;
        return ancestors(id).stream().allMatch(CategoryRow::active);
    }
}
