// Luật tin tự động của shop — hàm thuần (UC-VND-06 b4–5, UC-VND-19; plan 2026-11-26 chat D11, M08.01)
package com.genie.social.chat.application;

import com.genie.social.chat.application.ChatShopDirectory.ChatShop;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * <ul>
 *   <li><b>Lời chào</b>: hội thoại mới mở, hoặc mở lại khi tin cuối đã cách ≥ 24 giờ — chỉ khi bật tự trả lời và có lời
 *       chào; không lặp nếu đã có tin tự động trong 24 giờ.</li>
 *   <li><b>Tạm nghỉ</b>: shop trong khoảng {@code vacation_*} → {@code vacation_message} khi khách nhắn, bất kể công tắc
 *       (UC-VND-19 chỉ bật cờ).</li>
 *   <li><b>Vắng mặt</b>: khách nhắn lúc ngoài 08:00–22:00 giờ VN <b>hoặc</b> chủ shop không có kết nối nào →
 *       {@code away_message} (cần bật + có nội dung).</li>
 *   <li>Tạm nghỉ / vắng mặt tối đa 1 lần / hội thoại / 60 phút.</li>
 * </ul>
 */
public final class AutoReplyPolicy {

    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final LocalTime OPENS = LocalTime.of(8, 0);
    static final LocalTime CLOSES = LocalTime.of(22, 0);
    static final Duration AWAY_COOLDOWN = Duration.ofMinutes(60);
    static final Duration GREETING_GAP = Duration.ofHours(24);

    private AutoReplyPolicy() {
    }

    /** Trong giờ làm việc 08:00 ≤ giờ VN < 22:00. */
    public static boolean withinHours(OffsetDateTime now) {
        LocalTime t = now.atZoneSameInstant(VN).toLocalTime();
        return !t.isBefore(OPENS) && t.isBefore(CLOSES);
    }

    /**
     * @param created       hội thoại vừa được tạo ở lần mở này
     * @param lastMessageAt tin cuối của hội thoại ({@code null} = chưa có tin)
     * @param lastAutoAt    tin tự động gần nhất ({@code null} = chưa có)
     */
    public static Optional<String> greetingFor(ChatShop shop, boolean created, OffsetDateTime lastMessageAt,
                                               OffsetDateTime lastAutoAt, OffsetDateTime now) {
        if (!shop.autoReplyEnabled() || blank(shop.greeting())) {
            return Optional.empty();
        }
        boolean idle = created || lastMessageAt == null || !lastMessageAt.isAfter(now.minus(GREETING_GAP));
        boolean recentAuto = lastAutoAt != null && lastAutoAt.isAfter(now.minus(GREETING_GAP));
        return idle && !recentAuto ? Optional.of(shop.greeting().trim()) : Optional.empty();
    }

    /**
     * Tin trả lời khi <b>khách</b> vừa nhắn.
     *
     * @param ownerOnline chủ shop đang có kết nối WebSocket
     * @param lastAwayAt  lần gửi tin vắng mặt / tạm nghỉ gần nhất của hội thoại ({@code null} = chưa)
     */
    public static Optional<String> awayFor(ChatShop shop, boolean ownerOnline, OffsetDateTime lastAwayAt,
                                           OffsetDateTime now) {
        if (lastAwayAt != null && lastAwayAt.isAfter(now.minus(AWAY_COOLDOWN))) {
            return Optional.empty();
        }
        if (shop.onVacation(now) && !blank(shop.vacationMessage())) {
            return Optional.of(shop.vacationMessage().trim());
        }
        if (!shop.autoReplyEnabled() || blank(shop.awayMessage())) {
            return Optional.empty();
        }
        return !withinHours(now) || !ownerOnline ? Optional.of(shop.awayMessage().trim()) : Optional.empty();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
