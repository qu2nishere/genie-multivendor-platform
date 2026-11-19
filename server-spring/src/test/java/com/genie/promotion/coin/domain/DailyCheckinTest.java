package com.genie.promotion.coin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.genie.promotion.coin.application.CoinTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Luật điểm danh (plan Điểm Genie P03.01, D6) — "ngày" theo giờ Việt Nam. */
class DailyCheckinTest {

    private static final OffsetDateTime DAY1 = OffsetDateTime.parse("2026-06-01T09:00:00+07:00");

    @Test
    void sevenDayStreakPaysThousandOnSeventhDayThenRestartsTheSevenCells() {
        DailyCheckin d = DailyCheckin.firstFor(1L);
        List<Integer> rewards = new ArrayList<>();
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            OffsetDateTime now = DAY1.plusDays(i);
            rewards.add(d.checkIn(now));
            cells.add(CoinTime.streakDays(d, now));
        }
        assertThat(rewards).containsExactly(100, 100, 100, 100, 100, 100, 1000, 100);
        assertThat(cells).containsExactly(1, 2, 3, 4, 5, 6, 7, 1);
        assertThat(d.getStreakCount()).isEqualTo(8);
    }

    @Test
    void skippingADayRestartsAtOneAndSameDayIsRejected() {
        DailyCheckin d = DailyCheckin.firstFor(1L);
        d.checkIn(DAY1);
        d.checkIn(DAY1.plusDays(1));
        assertThatThrownBy(() -> d.checkIn(DAY1.plusDays(1).plusHours(5))).isInstanceOf(IllegalStateException.class);
        assertThat(d.checkIn(DAY1.plusDays(3))).isEqualTo(100);
        assertThat(d.getStreakCount()).isEqualTo(1);
    }

    @Test
    void vietnamMidnightIsTheDayBoundaryEvenWhenStoredInUtc() {
        DailyCheckin d = DailyCheckin.firstFor(1L);
        d.checkIn(OffsetDateTime.parse("2026-06-01T23:59:00+07:00"));
        // CSDL trả lại cùng thời điểm nhưng offset UTC (16:59Z ngày 01)
        d.setLastCheckinAt(d.getLastCheckinAt().withOffsetSameInstant(ZoneOffset.UTC));
        // 00:01 giờ VN ngày 02 = 17:01Z ngày 01 — cùng ngày UTC nhưng là ngày VN mới → được điểm danh, chuỗi +1
        assertThat(d.checkIn(OffsetDateTime.parse("2026-06-02T00:01:00+07:00"))).isEqualTo(100);
        assertThat(d.getStreakCount()).isEqualTo(2);

        d.setLastCheckinAt(d.getLastCheckinAt().withOffsetSameInstant(ZoneOffset.UTC));
        // 06:00 giờ VN cùng ngày 02 (= 23:00Z ngày 01) → đã điểm danh
        assertThatThrownBy(() -> d.checkIn(OffsetDateTime.parse("2026-06-02T06:00:00+07:00")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void streakCellIsZeroWhenStreakBroken() {
        DailyCheckin d = DailyCheckin.firstFor(1L);
        assertThat(CoinTime.streakDays(d, DAY1)).isZero();
        d.checkIn(DAY1);
        assertThat(CoinTime.streakDays(d, DAY1.plusDays(1))).isEqualTo(1);   // hôm qua có điểm danh — còn hiệu lực
        assertThat(CoinTime.streakDays(d, DAY1.plusDays(2))).isZero();        // đứt chuỗi
        assertThat(CoinTime.checkedInOn(d, DAY1)).isTrue();
        assertThat(CoinTime.checkedInOn(d, DAY1.plusDays(1))).isFalse();
    }
}
