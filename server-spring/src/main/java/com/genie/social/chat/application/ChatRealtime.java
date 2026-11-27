// Đẩy realtime của chat + thông báo khi người nhận offline (plan 2026-11-26 chat M04.01, M04.03; N4, D14, D15)
package com.genie.social.chat.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.auth.profile.application.UserSummary;
import com.genie.common.realtime.OnlineUsers;
import com.genie.common.realtime.UserChannel;
import com.genie.notification.inbox.application.NotificationFacade;
import com.genie.social.chat.domain.Conversation;
import com.genie.social.chat.domain.Conversation.Side;
import com.genie.social.chat.domain.ConversationRepository;
import com.genie.social.chat.domain.Message;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Gói tin được dựng <b>trong</b> transaction (thấy dữ liệu mới) và gửi <b>sau commit</b> ({@link UserChannel#sendAfterCommit})
 * — rollback thì không ai nhận tin sai. Simple broker không lưu tin: người nhận không có kết nối → thông báo in-app
 * {@code CHAT_MESSAGE} (một dòng chưa đọc / hội thoại — {@link NotificationFacade#upsertChat}).
 */
@Component
public class ChatRealtime {

    static final Duration TYPING_THROTTLE = Duration.ofSeconds(2);
    private static final Logger log = LoggerFactory.getLogger(ChatRealtime.class);

    private final UserChannel channel;
    private final OnlineUsers online;
    private final NotificationFacade notifications;
    private final ConversationRepository conversations;
    private final ChatMessageViews views;
    private final ChatShopDirectory shops;
    private final UserFacade users;
    private final StringRedisTemplate redis;

    public ChatRealtime(UserChannel channel, OnlineUsers online, NotificationFacade notifications,
                        ConversationRepository conversations, ChatMessageViews views, ChatShopDirectory shops,
                        UserFacade users, StringRedisTemplate redis) {
        this.channel = channel;
        this.online = online;
        this.notifications = notifications;
        this.conversations = conversations;
        this.views = views;
        this.shops = shops;
        this.users = users;
        this.redis = redis;
    }

    /** Gọi trong transaction ghi tin, sau {@code onNewMessage}. */
    public void messageCreated(Conversation c, Message m, String clientMessageId) {
        long sender = m.getSenderId();
        long recipient = c.otherThan(sender);
        channel.sendAfterCommit(sender, UserChannel.CHAT, new ChatEvents.MessageCreated(ChatEvents.MESSAGE_CREATED,
                c.getId(), views.of(m, sender).withClientMessageId(clientMessageId), clientMessageId,
                unreadTotal(sender, c.sideOf(sender))));
        channel.sendAfterCommit(recipient, UserChannel.CHAT, new ChatEvents.MessageCreated(ChatEvents.MESSAGE_CREATED,
                c.getId(), views.of(m, recipient), null, unreadTotal(recipient, c.sideOf(recipient))));
        if (!Boolean.TRUE.equals(m.getIsAuto()) && !online.isOnline(recipient)) {
            notifications.upsertChat(recipient, c.getId(), "Tin nhắn mới từ " + senderName(c, sender),
                    c.getLastMessageSnippet());
        }
    }

    /** Người đọc {@code readerId} vừa xem tới {@code upToMessageId} → bên kia thấy "Đã xem". */
    public void seen(Conversation c, long readerId, Long upToMessageId) {
        if (upToMessageId != null) {
            channel.sendAfterCommit(c.otherThan(readerId), UserChannel.CHAT,
                    new ChatEvents.MessageSeen(ChatEvents.MESSAGE_SEEN, c.getId(), upToMessageId));
        }
    }

    /** Đang gõ — tối đa 1 gói / 2 giây / hội thoại / người, không lưu (D14). */
    public void typing(Conversation c, long from) {
        if (throttled("chat:typing:" + c.getId() + ":" + from)) {
            return;
        }
        channel.sendNow(c.otherThan(from), UserChannel.CHAT, new ChatEvents.Typing(ChatEvents.TYPING, c.getId()));
    }

    public void failed(long userId, String clientMessageId, Long conversationId, String code, String message) {
        channel.sendNow(userId, UserChannel.CHAT, new ChatEvents.MessageFailed(ChatEvents.MESSAGE_FAILED,
                clientMessageId, conversationId, code, message));
    }

    private boolean throttled(String key) {
        try {
            return !Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "1", TYPING_THROTTLE));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private long unreadTotal(long userId, Side side) {
        return side == Side.CUSTOMER ? conversations.sumUnreadForCustomer(userId)
                : conversations.sumUnreadForVendor(userId);
    }

    private String senderName(Conversation c, long sender) {
        try {
            if (c.sideOf(sender) == Side.VENDOR) {
                return shops.byVendorUser(sender).map(ChatShopDirectory.ChatShop::name).orElse("Shop");
            }
            return users.findSummary(sender).map(UserSummary::fullName).filter(n -> !n.isBlank())
                    .orElse(ChatQueryService.FALLBACK_CUSTOMER);
        } catch (RuntimeException e) {
            log.debug("Không lấy được tên người gửi: {}", e.getMessage());
            return "Genie";
        }
    }
}
