// Job hằng ngày: tỉ lệ phản hồi chat của shop (ENTITY 1.4 chatResponseRate; plan 2026-11-26 chat D12, M08.04)
package com.genie.vendor.shop.application;

import com.genie.social.chat.application.ChatFacade;
import com.genie.social.chat.application.ChatStats;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 03:30 giờ VN: với mọi shop {@code VERIFIED} (lô {@value #BATCH_SIZE} theo {@code id}), tính % hội thoại khách mở trong
 * 30 ngày được shop trả lời <b>không tự động</b> trong 24 giờ ({@link ChatFacade#responseStats}) và ghi
 * {@code vendor_profiles.chat_response_rate} — {@code NULL} khi dưới {@value ChatStats#MIN_CONVERSATIONS} hội thoại.
 * Đổi số thì xóa cache trang shop (B3). Đặt ở {@code vendor} (vendor → social) để {@code social} không phải import
 * {@code vendor}.
 */
@Component
public class ChatResponseRateJob {

    static final int BATCH_SIZE = 100;
    private static final Logger log = LoggerFactory.getLogger(ChatResponseRateJob.class);

    private final JdbcTemplate jdbc;
    private final ChatFacade chat;
    private final PublicShopService publicShops;
    private final Clock clock;

    public ChatResponseRateJob(JdbcTemplate jdbc, ChatFacade chat, PublicShopService publicShops, Clock clock) {
        this.jdbc = jdbc;
        this.chat = chat;
        this.publicShops = publicShops;
        this.clock = clock;
    }

    private record ShopRow(long shopId, long vendorUserId, BigDecimal rate) {
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void scheduled() {
        int changed = run();
        if (changed > 0) {
            log.info("Đã cập nhật tỉ lệ phản hồi chat của {} shop", changed);
        }
    }

    /** Chạy một lượt; trả số shop đổi giá trị. */
    public int run() {
        OffsetDateTime to = OffsetDateTime.now(clock);
        OffsetDateTime from = to.minusDays(30);
        long afterId = 0;
        int changed = 0;
        while (true) {
            List<ShopRow> batch = jdbc.query("""
                    SELECT id, user_id, chat_response_rate FROM vendor_profiles
                    WHERE kyc_status = 'VERIFIED' AND id > ? ORDER BY id LIMIT ?""",
                    (rs, n) -> new ShopRow(rs.getLong("id"), rs.getLong("user_id"), rs.getBigDecimal("chat_response_rate")),
                    afterId, BATCH_SIZE);
            if (batch.isEmpty()) {
                return changed;
            }
            try {
                Map<Long, ChatStats.ResponseStats> stats = chat.responseStats(
                        batch.stream().map(ShopRow::vendorUserId).toList(), from, to);
                for (ShopRow s : batch) {
                    BigDecimal rate = stats.get(s.vendorUserId()).rate();
                    if (!sameRate(rate, s.rate())) {
                        jdbc.update("UPDATE vendor_profiles SET chat_response_rate = ? WHERE id = ?", rate,
                                s.shopId());
                        publicShops.evict(s.shopId());
                        changed++;
                    }
                }
            } catch (RuntimeException e) {
                log.error("Tính tỉ lệ phản hồi chat lỗi ở lô sau shop {}: {}", afterId, e.toString());
                return changed;
            }
            afterId = batch.get(batch.size() - 1).shopId();
        }
    }

    private static boolean sameRate(BigDecimal a, BigDecimal b) {
        return Objects.equals(a, b) || (a != null && b != null && a.compareTo(b) == 0);
    }
}
