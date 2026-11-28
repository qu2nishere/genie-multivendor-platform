package com.genie.promotion.voucher.domain;

/**
 * Điều kiện SQL tương đương {@link Voucher#status} (plan 2026-11-26 voucher D2) — viết MỘT lần, bảng ghép bí danh {@code v},
 * tham số {@code :now}. {@code VoucherStatusSqlIT} so từng voucher dựng sẵn với {@code status()} để hai bên không lệch.
 */
public final class VoucherStatusSql {

    private static final String EXHAUSTED = "(v.budget IS NOT NULL AND v.total_discount_used >= v.budget)";

    public static final String DISABLED = "(NOT v.is_active AND NOT " + EXHAUSTED + ")";
    public static final String SCHEDULED = "(v.is_active AND :now < v.valid_from)";
    public static final String ACTIVE = "(v.is_active AND v.valid_from <= :now AND v.valid_to > :now "
            + "AND v.used_count < v.usage_limit AND NOT " + EXHAUSTED + ")";
    public static final String ENDED = "((NOT v.is_active AND " + EXHAUSTED + ") OR (v.is_active AND v.valid_from <= :now "
            + "AND (v.valid_to <= :now OR v.used_count >= v.usage_limit OR " + EXHAUSTED + ")))";

    /** Biểu thức cột trạng thái: {@code CASE … END}. */
    public static final String CASE = "CASE WHEN " + DISABLED + " THEN 'DISABLED' WHEN " + SCHEDULED + " THEN 'SCHEDULED' "
            + "WHEN " + ACTIVE + " THEN 'ACTIVE' ELSE 'ENDED' END";

    private VoucherStatusSql() {
    }

    public static String of(VoucherStatus status) {
        return switch (status) {
            case ACTIVE -> ACTIVE;
            case SCHEDULED -> SCHEDULED;
            case ENDED -> ENDED;
            case DISABLED -> DISABLED;
        };
    }

    /** Thứ tự danh sách quản lý: đang chạy / sắp diễn ra — bắt đầu gần nhất trước; đã kết thúc / đã tắt — kết thúc mới nhất trước. */
    public static String orderBy(VoucherStatus status) {
        if (status == null) {
            return "v.id DESC";
        }
        return switch (status) {
            case ACTIVE, SCHEDULED -> "v.valid_from ASC, v.id DESC";
            case ENDED, DISABLED -> "v.valid_to DESC, v.id DESC";
        };
    }
}
