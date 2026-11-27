// Job tỉ lệ phản hồi chat + shop_stats (plan 2026-11-26 chat M08.03–M08.05; D12, N7)
package com.genie.social.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.social.chat.application.ChatFacade;
import com.genie.social.chat.application.ChatService;
import com.genie.support.TestDataBuilder;
import com.genie.vendor.shop.application.ChatResponseRateJob;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatResponseRateJobIT extends ChatTestSupport {

    @Autowired
    ChatResponseRateJob job;

    @Autowired
    ChatFacade facade;

    @Autowired
    ChatService chat;

    private final OffsetDateTime now = OffsetDateTime.parse(START);

    private long conversation(long buyer, long vendorUserId) {
        return jdbc.queryForObject("INSERT INTO conversations (customer_id, vendor_id, customer_unread_count, "
                + "vendor_unread_count, created_at, updated_at, last_message_at) VALUES (?, ?, 0, 0, now(), now(), now()) "
                + "RETURNING id", Long.class, buyer, vendorUserId);
    }

    private void message(long conv, long sender, OffsetDateTime at, boolean auto) {
        jdbc.update("INSERT INTO messages (conversation_id, sender_id, type, content, is_read, created_at, is_auto) "
                + "VALUES (?, ?, 'TEXT', 'x', FALSE, ?, ?)", conv, sender, at, auto);
    }

    /** Khách hỏi lúc {@code askedHoursAgo}; shop trả lời sau {@code replyMinutes} (null = không trả lời). */
    private void thread(TestDataBuilder.Shop shop, int askedHoursAgo, Integer replyMinutes, boolean autoOnly) {
        long buyer = customer();
        long conv = conversation(buyer, shop.userId());
        OffsetDateTime asked = now.minusHours(askedHoursAgo);
        message(conv, buyer, asked, false);
        if (autoOnly) {
            message(conv, shop.userId(), asked.plusSeconds(1), true);
        }
        if (replyMinutes != null) {
            message(conv, shop.userId(), asked.plusMinutes(replyMinutes), false);
        }
    }

    @Test
    void rateCountsHumanRepliesWithinTwentyFourHoursOverThirtyDays() {
        TestDataBuilder.Shop shop = shop();
        for (int i = 0; i < 7; i++) {
            thread(shop, 48 + i, 5 + i, false);          // trả lời trong vài phút
        }
        thread(shop, 72, 23 * 60, true);                 // có tin tự động, người trả lời sau 23 giờ → tính
        thread(shop, 96, null, true);                    // chỉ tin tự động → KHÔNG tính
        thread(shop, 120, 25 * 60, false);               // trả lời sau 25 giờ → không tính
        thread(shop, 24 * 40, 1, false);                 // ngoài cửa sổ 30 ngày → bỏ qua

        TestDataBuilder.Shop few = shop();
        thread(few, 10, 1, false);
        thread(few, 11, 1, false);

        job.run();

        assertThat(jdbc.queryForObject("SELECT chat_response_rate FROM vendor_profiles WHERE id = ?", BigDecimal.class,
                shop.shopId())).isEqualByComparingTo("80.00");
        assertThat(jdbc.queryForObject("SELECT chat_response_rate FROM vendor_profiles WHERE id = ?", BigDecimal.class,
                few.shopId())).isNull();   // dưới 5 hội thoại

        var stats = facade.responseStats(java.util.List.of(shop.userId()), now.minusDays(30), now).get(shop.userId());
        assertThat(stats.conversations()).isEqualTo(10);
        assertThat(stats.answered()).isEqualTo(8);
        assertThat(stats.medianMinutes()).isBetween(8, 9);   // trung vị của {5..11, 1380}

        // 8.2 shop_stats: tỉ lệ từ job + trung vị phút (cache 1 giờ)
        var opened = chat.open(customer(), shop.shopId()).view();
        assertThat(opened.shopStats().chatResponseRate()).isEqualByComparingTo("80.00");
        assertThat(opened.shopStats().avgResponseMinutes()).isBetween(8, 9);
        // job chạy lại khi số không đổi → không cập nhật dòng nào
        assertThat(job.run()).isZero();
    }
}
