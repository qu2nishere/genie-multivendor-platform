package com.genie.promotion.voucher.domain;

/**
 * Trạng thái quản lý của voucher (API_SPEC 10.7, 11.14; plan 2026-11-26 voucher D1) — suy ra từ cột, không lưu.
 * <ul>
 *   <li>{@code DISABLED} — shop / sàn đã tắt ({@code is_active = false}) và không phải do hết ngân sách;</li>
 *   <li>{@code SCHEDULED} — chưa tới {@code valid_from};</li>
 *   <li>{@code ENDED} — quá {@code valid_to}, hết lượt, hoặc hết ngân sách (kể cả khi {@code consume} đã tự tắt);</li>
 *   <li>{@code ACTIVE} — còn lại.</li>
 * </ul>
 */
public enum VoucherStatus { ACTIVE, SCHEDULED, ENDED, DISABLED }
