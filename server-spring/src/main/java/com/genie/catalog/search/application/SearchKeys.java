package com.genie.catalog.search.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Khóa Redis của thống kê từ khóa (plan 2026-11-17 A6; LUU_TRU 4). Ngày tính theo giờ Việt Nam.
 * <ul>
 *   <li>{@code search:kw:{yyyyMMdd}} — Sorted Set: từ khóa → số lượt trong ngày (top-K cho xu hướng), sống 8 ngày;</li>
 *   <li>{@code search:kw:pending} — Hash {@code yyyy-MM-dd|từ khóa → số lượt} chờ job ghi xuống DB;</li>
 *   <li>{@code search:kw:flushing} — bản đổi tên nguyên tử của pending trong lúc job ghi (lỗi thì giữ để lần sau ghi lại);</li>
 *   <li>{@code search:dedupe:{ipHash}:{từ khóa}} — chống một IP đếm lặp cùng từ trong 10 phút.</li>
 * </ul>
 */
public final class SearchKeys {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    public static final String PENDING = "search:kw:pending";
    public static final String FLUSHING = "search:kw:flushing";
    static final String FLUSH_LOCK = "job:search-kw-flush";

    private SearchKeys() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(VN));
    }

    public static String daily(LocalDate day) {
        return "search:kw:" + DAY.format(day);
    }

    static String dedupe(String ipHash, String keyword) {
        return "search:dedupe:" + ipHash + ":" + keyword;
    }
}
