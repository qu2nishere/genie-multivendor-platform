// Bảng ca luật tin tự động (plan 2026-11-26 chat M08.01; UC-VND-06, D11)
package com.genie.social.chat.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.social.chat.application.ChatShopDirectory.ChatShop;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AutoReplyPolicyTest {

    /** 14:00 giờ VN. */
    private static final OffsetDateTime DAY = OffsetDateTime.parse("2026-06-08T14:00:00+07:00");

    private static ChatShop shop(boolean enabled, String greeting, String away, OffsetDateTime vacFrom,
                                 OffsetDateTime vacTo, String vacation) {
        return new ChatShop(1, 2, "TechZone", null, false, true, enabled, greeting, away, vacFrom, vacTo, vacation, null);
    }

    private static ChatShop on() {
        return shop(true, "Xin chào!", "Shop đang vắng", null, null, null);
    }

    @ParameterizedTest
    @CsvSource({
            "07:59, false", "08:00, true", "14:00, true", "21:59, true", "22:00, false", "23:30, false", "00:00, false"})
    void businessHoursInVietnamTime(String hhmm, boolean within) {
        assertThat(AutoReplyPolicy.withinHours(OffsetDateTime.parse("2026-06-08T" + hhmm + ":00+07:00"))).isEqualTo(within);
        // cùng thời điểm biểu diễn theo UTC vẫn ra cùng kết quả
        assertThat(AutoReplyPolicy.withinHours(
                OffsetDateTime.parse("2026-06-08T" + hhmm + ":00+07:00").withOffsetSameInstant(java.time.ZoneOffset.UTC)))
                .isEqualTo(within);
    }

    @Test
    void greetingOnNewConversationOrAfterTwentyFourIdleHours() {
        assertThat(AutoReplyPolicy.greetingFor(on(), true, null, null, DAY)).contains("Xin chào!");
        assertThat(AutoReplyPolicy.greetingFor(on(), false, DAY.minusHours(23), null, DAY)).isEmpty();
        assertThat(AutoReplyPolicy.greetingFor(on(), false, DAY.minusHours(24), null, DAY)).contains("Xin chào!");
        assertThat(AutoReplyPolicy.greetingFor(on(), false, DAY.minusHours(30), DAY.minusHours(2), DAY)).isEmpty();
        assertThat(AutoReplyPolicy.greetingFor(shop(false, "Xin chào!", "x", null, null, null), true, null, null, DAY))
                .isEmpty();
        assertThat(AutoReplyPolicy.greetingFor(shop(true, "  ", "x", null, null, null), true, null, null, DAY)).isEmpty();
    }

    @Test
    void awayWhenOutsideHoursOrOwnerOffline() {
        OffsetDateTime night = OffsetDateTime.parse("2026-06-08T23:00:00+07:00");
        assertThat(AutoReplyPolicy.awayFor(on(), true, null, DAY)).isEmpty();
        assertThat(AutoReplyPolicy.awayFor(on(), false, null, DAY)).contains("Shop đang vắng");
        assertThat(AutoReplyPolicy.awayFor(on(), true, null, night)).contains("Shop đang vắng");
        assertThat(AutoReplyPolicy.awayFor(shop(false, "a", "Shop đang vắng", null, null, null), false, null, night))
                .isEmpty();
        assertThat(AutoReplyPolicy.awayFor(shop(true, "a", "", null, null, null), false, null, night)).isEmpty();
    }

    @Test
    void awayAtMostOncePerSixtyMinutes() {
        assertThat(AutoReplyPolicy.awayFor(on(), false, DAY.minusMinutes(59), DAY)).isEmpty();
        assertThat(AutoReplyPolicy.awayFor(on(), false, DAY.minusMinutes(60), DAY)).contains("Shop đang vắng");
    }

    @Test
    void vacationMessageWinsEvenWhenAutoReplyIsOff() {
        ChatShop vac = shop(false, null, null, DAY.minusDays(1), DAY.plusDays(2), "Shop nghỉ Tết tới 10/6");
        assertThat(AutoReplyPolicy.awayFor(vac, true, null, DAY)).contains("Shop nghỉ Tết tới 10/6");
        assertThat(AutoReplyPolicy.awayFor(vac, true, null, DAY.plusDays(3))).isEmpty();   // hết tạm nghỉ, công tắc tắt
        ChatShop vacOn = shop(true, "a", "Shop đang vắng", DAY.minusDays(1), DAY.plusDays(2), "Nghỉ phép");
        assertThat(AutoReplyPolicy.awayFor(vacOn, false, null, DAY)).contains("Nghỉ phép");
        assertThat(AutoReplyPolicy.awayFor(vacOn, false, DAY.minusMinutes(10), DAY)).isEmpty();
        ChatShop notYet = shop(true, "a", "Shop đang vắng", DAY.plusDays(1), DAY.plusDays(2), "Nghỉ phép");
        assertThat(AutoReplyPolicy.awayFor(notYet, false, null, DAY)).contains("Shop đang vắng");
    }
}
