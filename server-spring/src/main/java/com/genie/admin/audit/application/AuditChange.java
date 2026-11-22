package com.genie.admin.audit.application;

/**
 * Một trường bị đổi trong dòng nhật ký — đọc từ {@code details.changes} (plan 2026-11-22 nhật ký banner N4; API_SPEC 11.10).
 * Giá trị là chuỗi như lúc ghi ({@code "null"} = trống).
 */
public record AuditChange(String from, String to) {
}
