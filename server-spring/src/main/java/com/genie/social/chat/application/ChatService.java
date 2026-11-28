// Mở hội thoại, gửi tin, đánh dấu đã xem — một luật cho cả REST và STOMP (plan 2026-11-26 chat M03.05, N1, N3, N5, D5–D10)
package com.genie.social.chat.application;

import com.genie.auth.profile.application.UserFacade;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.RateLimiter;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import com.genie.notification.inbox.application.NotificationFacade;
import com.genie.social.chat.application.ChatShopDirectory.ChatShop;
import com.genie.social.chat.application.ChatViews.MessageView;
import com.genie.social.chat.application.ChatViews.OpenedConversation;
import com.genie.social.chat.domain.Conversation;
import com.genie.social.chat.domain.Conversation.Side;
import com.genie.social.chat.domain.ConversationRepository;
import com.genie.social.chat.domain.Message;
import com.genie.social.chat.domain.MessageRepository;
import com.genie.social.chat.domain.MessageType;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Nghiệp vụ chat. Ghi tin + cập nhật hội thoại trong <b>một</b> transaction có khóa dòng {@code conversations} (N3).
 * Kiểm quyền theo hội thoại (N5): {@code side} là phía người gọi đang đứng ({@code /me} = khách, {@code /seller} = shop);
 * {@code null} = suy từ hội thoại (STOMP).
 */
@Service
public class ChatService {

    public static final int MAX_TEXT = 2000;
    static final int RATE_LIMIT = 20;
    static final Duration RATE_WINDOW = Duration.ofSeconds(10);

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final ChatShopDirectory shops;
    private final UserFacade users;
    private final ChatQueryService query;
    private final ChatMessageViews views;
    private final ChatIdempotency idempotency;
    private final RateLimiter rateLimiter;
    private final NotificationFacade notifications;
    private final ChatRealtime realtime;
    private final ChatProductLookup products;
    private final ChatOrderLookup orders;
    private final UploadService uploads;
    private final AutoReplies autoReplies;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;
    private final TransactionTemplate tx;

    public ChatService(ConversationRepository conversations, MessageRepository messages, ChatShopDirectory shops,
                       UserFacade users, ChatQueryService query, ChatMessageViews views, ChatIdempotency idempotency,
                       RateLimiter rateLimiter, NotificationFacade notifications, ChatRealtime realtime,
                       ChatProductLookup products, ChatOrderLookup orders, UploadService uploads,
                       AutoReplies autoReplies, NamedParameterJdbcTemplate jdbc,
                       Clock clock, PlatformTransactionManager transactionManager) {
        this.conversations = conversations;
        this.messages = messages;
        this.shops = shops;
        this.users = users;
        this.query = query;
        this.views = views;
        this.idempotency = idempotency;
        this.rateLimiter = rateLimiter;
        this.notifications = notifications;
        this.realtime = realtime;
        this.products = products;
        this.orders = orders;
        this.uploads = uploads;
        this.autoReplies = autoReplies;
        this.jdbc = jdbc;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public record Opened(OpenedConversation view, boolean created) {
    }

    /** Kết quả gửi: {@code created = false} khi gửi lại cùng {@code client_message_id} (D4). */
    public record Sent(MessageView view, boolean created) {
    }

    /** 8.2 — mở (hoặc lấy) hội thoại của khách {@code me} với shop {@code shopId} (3.1). */
    public Opened open(long me, long shopId) {
        return tx.execute(status -> {
            ChatShop shop = shops.find(shopId).filter(ChatShop::active)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_UNAVAILABLE));
            if (shop.vendorUserId() == me) {
                throw new BusinessException(ErrorCode.CHAT_OWN_SHOP);
            }
            boolean created = conversations.insertIgnore(me, shop.vendorUserId()) == 1;
            long id = conversations.findByCustomerIdAndVendorId(me, shop.vendorUserId()).orElseThrow().getId();
            Conversation c = conversations.findByIdForUpdate(id).orElseThrow();
            autoReplies.greet(c, shop, created, now());   // lời chào UC-VND-06 (D11a)
            return new Opened(query.opened(c, shop), created);
        });
    }

    /** 8.4 / {@code /app/chat.send} (3.2). */
    public Sent send(long me, Side side, long conversationId, SendCommand cmd) {
        String cmid = ChatIdempotency.normalize(cmd.clientMessageId());
        MessageType type = typeOf(cmd);
        String text = type == MessageType.TEXT ? normalizeText(cmd.text()) : null;
        ChatIdempotency.Claim claim = idempotency.claim(me, cmid);
        if (!claim.fresh()) {
            long existing = claim.existingId().orElseThrow(() -> new BusinessException(ErrorCode.RATE_LIMITED));
            return tx.execute(status -> replay(me, side, conversationId, existing, cmid));
        }
        try {
            rateLimiter.check("rl:chat:" + me, RATE_LIMIT, RATE_WINDOW);
            Message saved = tx.execute(status -> {
                Conversation c = lockMember(me, side, conversationId);
                ChatShop shop = ensureWritable(c);
                Message m = build(c, me, type, text, cmd);
                messages.save(m);
                boolean fromCustomer = c.sideOf(me) == Side.CUSTOMER;
                c.onNewMessage(m, fromCustomer);
                realtime.messageCreated(c, m, cmid);
                if (fromCustomer) {
                    autoReplies.afterCustomerMessage(c, shop, m.getCreatedAt());   // vắng mặt / tạm nghỉ (D11b–c)
                }
                return m;
            });
            idempotency.complete(me, cmid, saved.getId());
            return new Sent(views.of(saved, me).withClientMessageId(cmid), true);
        } catch (RuntimeException e) {
            idempotency.release(me, cmid);
            throw e;
        }
    }

    /**
     * 8.5 / {@code /app/chat.read} (3.3): bộ đếm phía mình về 0, tin của bên kia thành "Đã xem", thông báo
     * {@code CHAT_MESSAGE} của hội thoại đã đọc. Trả id tin lớn nhất vừa chuyển sang đã xem ({@code null} nếu không có).
     */
    public Long read(long me, Side side, long conversationId) {
        return tx.execute(status -> {
            Conversation c = lockMember(me, side, conversationId);
            c.markReadBy(c.sideOf(me) == Side.CUSTOMER);
            Long upTo = jdbc.queryForObject("""
                    WITH u AS (UPDATE messages SET is_read = TRUE
                               WHERE conversation_id = :cid AND sender_id <> :me AND is_read = FALSE RETURNING id)
                    SELECT MAX(id) FROM u""", new MapSqlParameterSource("cid", conversationId).addValue("me", me),
                    Long.class);
            notifications.markChatRead(me, conversationId);
            realtime.seen(c, me, upTo);
            return upTo;
        });
    }

    /** {@code /app/chat.typing} — chuyển tiếp cho bên kia, không lưu (D14). */
    public void typing(long me, long conversationId) {
        Conversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        checkSide(c, me, null);
        realtime.typing(c, me);
    }

    /** Loại tin + kiểm trường bắt buộc theo loại (API_SPEC 8.4) — 422 trước khi tốn lượt rate limit. */
    static MessageType typeOf(SendCommand cmd) {
        MessageType type;
        try {
            type = MessageType.valueOf(cmd.type() == null ? "" : cmd.type().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidRequestException.of("type", "UNSUPPORTED", cmd.type(), "Loại tin không được hỗ trợ.");
        }
        switch (type) {
            case IMAGE -> {
                if (cmd.imageObjectKey() == null || cmd.imageObjectKey().isBlank()) {
                    throw InvalidRequestException.of("image_object_key", "REQUIRED", null, "Thiếu ảnh.");
                }
            }
            case PRODUCT_CARD -> {
                if (cmd.productId() == null || cmd.productId() <= 0) {
                    throw InvalidRequestException.of("product_id", "REQUIRED", cmd.productId(), "Thiếu sản phẩm.");
                }
            }
            case ORDER_CARD -> {
                if (cmd.orderId() == null || cmd.orderId() <= 0) {
                    throw InvalidRequestException.of("order_id", "REQUIRED", cmd.orderId(), "Thiếu đơn hàng.");
                }
            }
            case TEXT -> {
                // kiểm ở normalizeText
            }
        }
        return type;
    }

    /** Dựng tin theo loại (UC-SOC-02 — D7, D8, D9); gọi khi đang khóa hội thoại. */
    private Message build(Conversation c, long me, MessageType type, String text, SendCommand cmd) {
        OffsetDateTime at = now();
        return switch (type) {
            case TEXT -> Message.text(c.getId(), me, text, at);
            case IMAGE -> {
                // khóa tmp/user/{me}/… của CHÍNH người gửi → conversation/{id}/{uuid}.{ext} (bucket riêng tư)
                String key = uploads.promoteTo(me, UploadPurpose.CHAT_IMAGE, cmd.imageObjectKey().trim(),
                        "conversation/" + c.getId() + "/").objectKey();
                deleteOnRollback(key);
                yield Message.image(c.getId(), me, key, at);
            }
            case PRODUCT_CARD -> {
                ChatProductLookup.ProductRef ref = products.resolve(cmd.productId(), cmd.variantId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_CARD_FORBIDDEN));
                if (ref.vendorUserId() != c.getVendorId() || !ref.published()) {
                    throw new BusinessException(ErrorCode.CHAT_CARD_FORBIDDEN);
                }
                if (ref.variantId() == null) {
                    if (cmd.variantId() != null) {
                        throw InvalidRequestException.of("variant_id", "NOT_IN_PRODUCT", cmd.variantId(),
                                "Biến thể không thuộc sản phẩm.");
                    }
                    throw new BusinessException(ErrorCode.CHAT_CARD_FORBIDDEN);
                }
                yield Message.productCard(c.getId(), me, ref.variantId(), at);
            }
            case ORDER_CARD -> {
                if (!orders.belongs(cmd.orderId(), c.getCustomerId(), c.getVendorId())) {
                    throw new BusinessException(ErrorCode.CHAT_CARD_FORBIDDEN);
                }
                yield Message.orderCard(c.getId(), me, cmd.orderId(), at);
            }
        };
    }

    /** Transaction rollback sau khi đã chuyển ảnh → xóa tệp để không sót ảnh mồ côi trong bucket riêng tư. */
    private void deleteOnRollback(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        uploads.deleteQuietly(UploadPurpose.CHAT_IMAGE, key);
                    }
                }
            });
        }
    }

    /** Chữ: bỏ ký tự điều khiển (giữ xuống dòng / tab), cắt khoảng trắng hai đầu, 1–2.000 ký tự (D6). */
    public static String normalizeText(String raw) {
        String s = raw == null ? "" : raw.replace("\r\n", "\n").replaceAll("[\\p{Cc}&&[^\\n\\t]]", "").strip();
        if (s.isEmpty()) {
            throw InvalidRequestException.of("text", "REQUIRED", raw, "Tin nhắn không được để trống.");
        }
        if (s.length() > MAX_TEXT) {
            throw InvalidRequestException.of("text", "TOO_LONG", s.length(), "Tin nhắn tối đa 2.000 ký tự.");
        }
        return s;
    }

    private Sent replay(long me, Side side, long conversationId, long messageId, String cmid) {
        Conversation c = member(me, side, conversationId);
        Message m = messages.findById(messageId).filter(x -> x.getConversationId().equals(c.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return new Sent(views.of(m, me).withClientMessageId(cmid), false);
    }

    /** Hội thoại (khóa dòng) mà {@code me} là thành viên ở đúng phía; ngược lại 404 (N5). */
    Conversation lockMember(long me, Side side, long conversationId) {
        Conversation c = conversations.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        checkSide(c, me, side);
        return c;
    }

    private Conversation member(long me, Side side, long conversationId) {
        Conversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        checkSide(c, me, side);
        return c;
    }

    private static void checkSide(Conversation c, long me, Side side) {
        Side actual = c.sideOf(me);
        if (actual == Side.NONE || (side != null && actual != side)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    /** Shop còn hoạt động và khách còn hoạt động (D5, D18) — hội thoại cũ chỉ còn đọc. */
    private ChatShop ensureWritable(Conversation c) {
        ChatShop shop = shops.byVendorUser(c.getVendorId()).orElse(null);
        if (shop == null || !shop.active()) {
            throw new BusinessException(ErrorCode.SHOP_UNAVAILABLE);
        }
        if (!users.isActive(c.getCustomerId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return shop;
    }

    private OffsetDateTime now() {
        // micro giây như PostgreSQL — cursor (created_at, id) so khớp đúng sau khi đọc lại
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
