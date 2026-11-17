package com.genie.admin.audit.application;

/**
 * Tham số thô của {@code GET /admin/audit-logs} và {@code …/export} (API_SPEC 11.10; plan nhật ký kiểm toán L03.01).
 * Kiểm và chuẩn hóa ở {@link AuditLogQueryService#normalize}: sai → {@code 422 GNE-VAL-4220} kèm {@code errors[]}.
 *
 * @param from     ISO-8601 ({@code 2026-06-01T00:00:00+07:00}) hoặc ngày {@code 2026-06-01} (đầu ngày giờ Việt Nam); mặc định
 *                 7 ngày trước {@code to}
 * @param to       như {@code from}, loại trừ; dạng ngày được tính <b>hết</b> ngày đó; mặc định = bây giờ
 * @param category {@code AUTH, KYC, FINANCE, MODERATION, SECURITY, CONFIG}; trống = mọi nhóm
 * @param q        tìm trong email / IP / mã tham chiếu, ≤ 100 ký tự
 * @param cursor   {@code next_cursor} của trang trước
 * @param limit    1–100, mặc định 50
 */
public record AuditLogQuery(String from, String to, String category, String q, String cursor, Integer limit) {

    public static AuditLogQuery forExport(String from, String to, String category, String q) {
        return new AuditLogQuery(from, to, category, q, null, null);
    }
}
