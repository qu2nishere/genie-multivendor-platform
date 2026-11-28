// Gửi tin tự động của shop vào hội thoại (UC-VND-06 b4–5; plan 2026-11-26 chat D11, M08.02; N7)
package com.genie.social.chat.application;

import com.genie.common.realtime.OnlineUsers;
import com.genie.social.chat.application.ChatShopDirectory.ChatShop;
import com.genie.social.chat.domain.Conversation;
import com.genie.social.chat.domain.Message;
import com.genie.social.chat.domain.MessageRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Gọi <b>trong</b> transaction đang khóa hội thoại. Tin tự động: {@code is_auto = true}, đứng tên chủ shop, đẩy realtime như
 * tin thường, không tạo thông báo, không tính tỉ lệ phản hồi. Lần gửi tin vắng mặt / tạm nghỉ gần nhất giữ ở Redis
 * {@code chat:away:{conversationId}} (lời chào không chặn tin vắng mặt); Redis lỗi → dùng tin tự động gần nhất trong CSDL.
 */
@Component
public class AutoReplies {

    private static final String AWAY_KEY = "chat:away:";
    private static final Duration AWAY_KEY_TTL = Duration.ofHours(2);
    private static final Logger log = LoggerFactory.getLogger(AutoReplies.class);

    private final MessageRepository messages;
    private final ChatRealtime realtime;
    private final OnlineUsers online;
    private final StringRedisTemplate redis;

    public AutoReplies(MessageRepository messages, ChatRealtime realtime, OnlineUsers online,
                       StringRedisTemplate redis) {
        this.messages = messages;
        this.realtime = realtime;
        this.online = online;
        this.redis = redis;
    }

    /** Lời chào khi mở hội thoại (gọi trước khi dựng phản hồi 8.2). */
    public void greet(Conversation c, ChatShop shop, boolean created, OffsetDateTime now) {
        AutoReplyPolicy.greetingFor(shop, created, c.getLastMessageAt(),
                messages.lastAutoAt(c.getId()).orElse(null), now).ifPresent(text -> post(c, shop, text, now));
    }

    /** Khách vừa nhắn → tin vắng mặt / tạm nghỉ nếu đúng luật. */
    public void afterCustomerMessage(Conversation c, ChatShop shop, OffsetDateTime now) {
        AutoReplyPolicy.awayFor(shop, online.isOnline(shop.vendorUserId()), lastAwayAt(c), now).ifPresent(text -> {
            post(c, shop, text, now.plusNanos(1_000));   // sau tin của khách một micro giây — đúng thứ tự hiển thị
            markAway(c, now);
        });
    }

    private void post(Conversation c, ChatShop shop, String text, OffsetDateTime at) {
        Message m = messages.save(Message.auto(c.getId(), shop.vendorUserId(), text, at));
        c.onNewMessage(m, false);
        realtime.messageCreated(c, m, null);
    }

    private OffsetDateTime lastAwayAt(Conversation c) {
        try {
            String v = redis.opsForValue().get(AWAY_KEY + c.getId());
            return v == null ? null : Instant.ofEpochSecond(Long.parseLong(v)).atOffset(ZoneOffset.UTC);
        } catch (RuntimeException e) {
            log.debug("Không đọc được mốc tin vắng mặt, dùng CSDL: {}", e.getMessage());
            return messages.lastAutoAt(c.getId()).orElse(null);
        }
    }

    private void markAway(Conversation c, OffsetDateTime now) {
        try {
            redis.opsForValue().set(AWAY_KEY + c.getId(), String.valueOf(now.toEpochSecond()), AWAY_KEY_TTL);
        } catch (RuntimeException e) {
            log.debug("Không ghi được mốc tin vắng mặt: {}", e.getMessage());
        }
    }
}
