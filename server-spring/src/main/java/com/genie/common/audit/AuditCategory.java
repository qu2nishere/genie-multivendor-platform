package com.genie.common.audit;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Nhóm hành động kiểm toán cho bộ lọc màn Nhật ký hệ thống (API_SPEC 11.10, 16; plan nhật ký kiểm toán D2).
 * Ánh xạ nằm ở {@link AuditAction#category()}.
 */
public enum AuditCategory {
    AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG;

    /** Mọi hành động thuộc nhóm — dùng cho điều kiện {@code action_type IN (…)}. */
    public Set<AuditAction> actions() {
        EnumSet<AuditAction> set = EnumSet.noneOf(AuditAction.class);
        Arrays.stream(AuditAction.values()).filter(a -> a.category() == this).forEach(set::add);
        return set;
    }
}
