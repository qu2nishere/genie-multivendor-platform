// Đọc hộp thư và lịch sử tin (plan 2026-11-26 chat M03.07; API_SPEC 8.1, 8.2, 8.3, 8.6; D13, D16)
package com.genie.social.chat.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.auth.profile.application.UserSummary;
import com.genie.common.api.CursorPage;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.realtime.OnlineUsers;
import com.genie.common.realtime.PresenceListener;
import com.genie.social.chat.application.ChatShopDirectory.ChatShop;
import com.genie.social.chat.application.ChatViews.ConversationView;
import com.genie.social.chat.application.ChatViews.Counterpart;
import com.genie.social.chat.application.ChatViews.LastMessage;
import com.genie.social.chat.application.ChatViews.MessageView;
import com.genie.social.chat.application.ChatViews.OpenedConversation;
import com.genie.social.chat.application.ChatViews.OrderPickView;
import com.genie.social.chat.application.ChatViews.ShopStats;
import com.genie.social.chat.domain.Conversation;
import com.genie.social.chat.domain.Conversation.Side;
import com.genie.social.chat.domain.ConversationRepository;
import com.genie.social.chat.domain.Message;
import com.genie.social.chat.domain.MessageRepository;
import com.genie.social.chat.domain.MessageType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hộp thư theo phía người gọi ({@link Side#CUSTOMER} = {@code /me/…}, {@link Side#VENDOR} = {@code /seller/…}); bên kia
 * nạp theo lô qua {@link ChatShopDirectory} / {@link UserFacade} (chống N+1). Hội thoại không thuộc người gọi ở đúng phía
 * → {@code 404 GNE-SYS-4040} (N5 — không lộ tồn tại).
 */
@Service
public class ChatQueryService {

    static final int CONVERSATION_LIMIT = 20;
    static final int MESSAGE_LIMIT = 30;
    static final int MAX_LIMIT = 50;
    static final String FALLBACK_CUSTOMER = "Người dùng Genie";

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ChatShopDirectory shops;
    private final ChatOrderLookup orders;
    private final UserFacade users;
    private final OnlineUsers online;
    private final PresenceListener presence;
    private final ChatMessageViews views;
    private final ChatStats stats;

    public ChatQueryService(ConversationRepository conversations, MessageRepository messages, ChatShopDirectory shops,
                            ChatOrderLookup orders, UserFacade users, OnlineUsers online, PresenceListener presence,
                            ChatMessageViews views, ChatStats stats) {
        this.conversations = conversations;
        this.messages = messages;
        this.shops = shops;
        this.orders = orders;
        this.users = users;
        this.online = online;
        this.presence = presence;
        this.views = views;
        this.stats = stats;
    }

    public record InboxPage(CursorPage<ConversationView> page, long unreadTotal) {
    }

    /** 8.1 — chỉ hội thoại đã có tin, {@code last_message_at DESC, id DESC}. */
    @Transactional(readOnly = true)
    public InboxPage conversations(long me, Side side, String cursor, Integer limit) {
        int size = ChatCursor.limit(limit, CONVERSATION_LIMIT, MAX_LIMIT);
        PageRequest page = PageRequest.of(0, size + 1);
        List<Conversation> rows;
        if (cursor == null || cursor.isBlank()) {
            rows = side == Side.CUSTOMER ? conversations.pageForCustomer(me, page) : conversations.pageForVendor(me, page);
        } else {
            ChatCursor c = ChatCursor.decode(cursor, "cursor");
            rows = side == Side.CUSTOMER ? conversations.pageForCustomerAfter(me, c.at(), c.id(), page)
                    : conversations.pageForVendorAfter(me, c.at(), c.id(), page);
        }
        boolean hasMore = rows.size() > size;
        List<Conversation> pageRows = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? new ChatCursor(pageRows.get(size - 1).getLastMessageAt(), pageRows.get(size - 1).getId())
                .encode() : null;

        Map<Long, Counterpart> counterparts = counterparts(pageRows, side);
        Map<Long, Message> latest = pageRows.isEmpty() ? Map.of()
                : messages.latestOf(pageRows.stream().map(Conversation::getId).toList()).stream()
                        .collect(Collectors.toMap(Message::getConversationId, Function.identity()));
        List<ConversationView> items = new ArrayList<>(pageRows.size());
        for (Conversation c : pageRows) {
            Counterpart who = counterparts.get(c.getId());
            if (who == null) {
                continue;   // shop / tài khoản đã biến mất hẳn — không có gì để hiện
            }
            items.add(new ConversationView(c.getId(), who, lastMessage(latest.get(c.getId()), c),
                    c.unreadFor(side)));
        }
        return new InboxPage(new CursorPage<>(items, new CursorPage.Pagination(size, hasMore, next)),
                unreadTotal(me, side));
    }

    /** 8.3 — mới nhất trước; {@code before} = cursor của trang trước. */
    @Transactional(readOnly = true)
    public CursorPage<MessageView> messages(long me, Side side, long conversationId, String before, Integer limit) {
        member(me, side, conversationId);
        int size = ChatCursor.limit(limit, MESSAGE_LIMIT, MAX_LIMIT);
        PageRequest page = PageRequest.of(0, size + 1);
        List<Message> rows;
        if (before == null || before.isBlank()) {
            rows = messages.page(conversationId, page);
        } else {
            ChatCursor c = ChatCursor.decode(before, "before");
            rows = messages.pageBefore(conversationId, c.at(), c.id(), page);
        }
        boolean hasMore = rows.size() > size;
        List<Message> pageRows = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? new ChatCursor(pageRows.get(size - 1).getCreatedAt(), pageRows.get(size - 1).getId())
                .encode() : null;
        return new CursorPage<>(views.of(pageRows, me), new CursorPage.Pagination(size, hasMore, next));
    }

    /** 8.6 — đơn gần nhất giữa đúng hai bên của hội thoại. */
    @Transactional(readOnly = true)
    public List<OrderPickView> orders(long me, Side side, long conversationId, Integer limit) {
        Conversation c = member(me, side, conversationId);
        int size = ChatCursor.limit(limit, 10, 20);
        return orders.recentBetween(c.getCustomerId(), c.getVendorId(), size).stream()
                .map(o -> new OrderPickView(o.id(), o.code(), o.status(), o.total(), o.createdAt(), o.itemCount(),
                        o.thumbnailUrl()))
                .toList();
    }

    /** Badge: tổng chưa đọc phía người gọi. */
    @Transactional(readOnly = true)
    public long unreadTotal(long me, Side side) {
        return side == Side.CUSTOMER ? conversations.sumUnreadForCustomer(me) : conversations.sumUnreadForVendor(me);
    }

    /** Phản hồi 8.2 (phía khách). */
    @Transactional(readOnly = true)
    public OpenedConversation opened(Conversation c, ChatShop shop) {
        return new OpenedConversation(c.getId(), shopCounterpart(shop, presence.lastActive(List.of(shop.vendorUserId()))),
                new ShopStats(shop.chatResponseRate(), stats.avgResponseMinutes(shop.vendorUserId())),
                c.unreadFor(Side.CUSTOMER));
    }

    /** Hội thoại của người gọi ở đúng phía; ngược lại 404. */
    Conversation member(long me, Side side, long conversationId) {
        Conversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (c.sideOf(me) != side) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return c;
    }

    private Map<Long, Counterpart> counterparts(List<Conversation> rows, Side side) {
        Map<Long, Counterpart> out = new java.util.HashMap<>();
        if (rows.isEmpty()) {
            return out;
        }
        if (side == Side.CUSTOMER) {
            List<Long> vendorIds = rows.stream().map(Conversation::getVendorId).distinct().toList();
            Map<Long, ChatShop> byVendor = shops.byVendorUsers(vendorIds);
            Map<Long, OffsetDateTime> last = presence.lastActive(vendorIds);
            for (Conversation c : rows) {
                ChatShop s = byVendor.get(c.getVendorId());
                if (s != null) {
                    out.put(c.getId(), shopCounterpart(s, last));
                }
            }
        } else {
            List<Long> customerIds = rows.stream().map(Conversation::getCustomerId).distinct().toList();
            Map<Long, UserSummary> summaries = users.findSummaries(customerIds);
            Map<Long, OffsetDateTime> last = presence.lastActive(customerIds);
            for (Conversation c : rows) {
                UserSummary u = summaries.get(c.getCustomerId());
                if (u != null) {
                    boolean on = online.isOnline(u.id());
                    String name = u.fullName() == null || u.fullName().isBlank() ? FALLBACK_CUSTOMER : u.fullName();
                    out.put(c.getId(), new Counterpart("CUSTOMER", u.id(), name, u.avatarUrl(), false, on,
                            on ? null : last.get(u.id())));
                }
            }
        }
        return out;
    }

    private Counterpart shopCounterpart(ChatShop s, Map<Long, OffsetDateTime> lastActive) {
        boolean on = online.isOnline(s.vendorUserId());
        return new Counterpart("SHOP", s.shopId(), s.name(), s.logoUrl(), s.isMall(), on,
                on ? null : lastActive.get(s.vendorUserId()));
    }

    private static LastMessage lastMessage(Message m, Conversation c) {
        if (m == null) {
            return c.getLastMessageAt() == null ? null
                    : new LastMessage("TEXT", c.getLastMessageSnippet(), c.getLastMessageAt());
        }
        String preview = m.getType() == MessageType.TEXT ? c.getLastMessageSnippet() : null;
        return new LastMessage(m.getType().name(), preview, m.getCreatedAt());
    }
}
