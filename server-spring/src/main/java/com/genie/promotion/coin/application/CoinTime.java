package com.genie.promotion.coin.application;

import com.genie.promotion.coin.domain.DailyCheckin;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * "Ngày" của Điểm Genie tính theo giờ Việt Nam (plan Điểm Genie N6) + quy tắc ô chuỗi 7 ngày cho giao diện (D6).
 */
public final class CoinTime {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private CoinTime() {
    }

    /** Bây giờ theo offset +07:00. */
    public static OffsetDateTime nowVn(Clock clock) {
        return OffsetDateTime.now(clock).atZoneSameInstant(VN).toOffsetDateTime();
    }

    /** 00:00 ngày hôm sau (giờ VN) — lúc điểm danh được tiếp. */
    public static OffsetDateTime nextMidnight(OffsetDateTime nowVn) {
        return nowVn.toLocalDate().plusDays(1).atStartOfDay(VN).toOffsetDateTime();
    }

    /** Đã điểm danh trong ngày (giờ VN) chưa. */
    public static boolean checkedInOn(DailyCheckin d, OffsetDateTime nowVn) {
        return d != null && d.getLastCheckinAt() != null
                && lastDay(d).equals(nowVn.toLocalDate());
    }

    /**
     * Ô hiện tại của chuỗi 7 ngày (D6): {@code ((streak − 1) mod 7) + 1} khi chuỗi còn hiệu lực (điểm danh hôm nay hoặc hôm
     * qua), ngược lại 0. Sau ngày thứ 7 ô quay về 1 ("bắt đầu chuỗi mới").
     */
    public static int streakDays(DailyCheckin d, OffsetDateTime nowVn) {
        if (d == null || d.getLastCheckinAt() == null || d.getStreakCount() == null || d.getStreakCount() < 1) {
            return 0;
        }
        LocalDate last = lastDay(d);
        LocalDate today = nowVn.toLocalDate();
        if (!last.equals(today) && !last.plusDays(1).equals(today)) {
            return 0;
        }
        return ((d.getStreakCount() - 1) % 7) + 1;
    }

    private static LocalDate lastDay(DailyCheckin d) {
        return d.getLastCheckinAt().atZoneSameInstant(VN).toLocalDate();
    }
}
