package com.genie.admin.banner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Luật của banner (plan banner B02.03). */
class HomeBannerTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-01T00:00:00+07:00");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-06-30T00:00:00+07:00");

    private static HomeBanner banner(BannerPosition position, Long categoryId, boolean active) {
        return HomeBanner.create("Siêu sale", "banner/1/desktop-x.jpg", null, "/flash-sale", position, categoryId, 1,
                START, END, active, 7L);
    }

    @Test
    void stateFollowsSwitchAndScheduleBoundaries() {
        HomeBanner b = banner(BannerPosition.HOME_TOP_CAROUSEL, null, true);
        assertThat(b.state(START.minusSeconds(1))).isEqualTo(BannerState.SCHEDULED);
        assertThat(b.state(START)).isEqualTo(BannerState.LIVE);
        assertThat(b.state(END.minusSeconds(1))).isEqualTo(BannerState.LIVE);
        assertThat(b.state(END)).isEqualTo(BannerState.ENDED);
        assertThat(b.isLive(START)).isTrue();
        assertThat(b.isLive(END)).isFalse();
        b.deactivate();
        assertThat(b.state(START.plusDays(1))).isEqualTo(BannerState.OFF);
        b.activate();
        assertThat(b.state(START.plusDays(1))).isEqualTo(BannerState.LIVE);
    }

    @Test
    void onlyOffOrEndedIsDeletable() {
        HomeBanner b = banner(BannerPosition.POPUP, null, true);
        assertThat(b.isDeletable(START.minusDays(1))).isFalse();   // SCHEDULED
        assertThat(b.isDeletable(START.plusDays(1))).isFalse();    // LIVE
        assertThat(b.isDeletable(END.plusDays(1))).isTrue();       // ENDED
        b.deactivate();
        assertThat(b.isDeletable(START.plusDays(1))).isTrue();     // OFF
    }

    @Test
    void categoryTopRequiresCategoryAndOtherPositionsDropIt() {
        assertThatThrownBy(() -> banner(BannerPosition.CATEGORY_TOP, null, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(banner(BannerPosition.CATEGORY_TOP, 3L, true).getCategoryId()).isEqualTo(3L);
        assertThat(banner(BannerPosition.HOME_MIDDLE, 3L, true).getCategoryId()).isNull();
    }

    @Test
    void scheduleMustEndAfterStart() {
        HomeBanner b = banner(BannerPosition.HOME_MIDDLE, null, true);
        assertThatThrownBy(() -> b.changeSchedule(END, START)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> b.changeSchedule(START, START)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void imagesAndStats() {
        HomeBanner b = banner(BannerPosition.HOME_MIDDLE, null, true);
        assertThatThrownBy(() -> b.changeImages(" ", null)).isInstanceOf(IllegalArgumentException.class);
        b.changeImages("banner/1/desktop-y.jpg", "");
        assertThat(b.getMobileImageUrl()).isNull();
        assertThat(b.ctr()).isZero();
        b.recordStats(4, 1);
        b.recordStats(6, 1);
        assertThat(b.getImpressionCount()).isEqualTo(10);
        assertThat(b.ctr()).isEqualTo(0.2);
    }
}
