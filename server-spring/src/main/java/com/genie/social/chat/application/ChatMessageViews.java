// Dựng tin 8.3 theo góc nhìn người xem — nạp thẻ theo lô, ký link ảnh (plan 2026-11-26 chat M03.07, M07.02, D3, D7, D8)
package com.genie.social.chat.application;

import com.genie.common.storage.ObjectStorage;
import com.genie.common.storage.UploadPurpose;
import com.genie.social.chat.application.ChatViews.MessageView;
import com.genie.social.chat.application.ChatViews.OrderCardView;
import com.genie.social.chat.application.ChatViews.ProductCardView;
import com.genie.social.chat.domain.Message;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Một lời gọi cổng sản phẩm + một lời gọi cổng đơn cho cả trang tin (chống N+1). */
@Component
public class ChatMessageViews {

    /** Link ký đọc ảnh chat (N6). */
    static final Duration IMAGE_TTL = Duration.ofMinutes(10);
    private static final Logger log = LoggerFactory.getLogger(ChatMessageViews.class);

    private final ChatProductLookup products;
    private final ChatOrderLookup orders;
    private final ObjectStorage storage;

    public ChatMessageViews(ChatProductLookup products, ChatOrderLookup orders, ObjectStorage storage) {
        this.products = products;
        this.orders = orders;
        this.storage = storage;
    }

    public List<MessageView> of(List<Message> messages, long viewerId) {
        Map<Long, ChatProductLookup.ProductCard> cards = products.cards(messages.stream()
                .map(Message::getProductVariantId).filter(Objects::nonNull).distinct().toList());
        Map<Long, ChatOrderLookup.OrderCard> orderCards = orders.cards(messages.stream()
                .map(Message::getOrderId).filter(Objects::nonNull).distinct().toList());
        List<MessageView> out = new ArrayList<>(messages.size());
        for (Message m : messages) {
            out.add(view(m, viewerId, cards, orderCards));
        }
        return out;
    }

    public MessageView of(Message m, long viewerId) {
        return of(List.of(m), viewerId).get(0);
    }

    private MessageView view(Message m, long viewerId, Map<Long, ChatProductLookup.ProductCard> cards,
                             Map<Long, ChatOrderLookup.OrderCard> orderCards) {
        ProductCardView product = null;
        if (m.getProductVariantId() != null) {
            ChatProductLookup.ProductCard c = cards.get(m.getProductVariantId());
            if (c != null) {
                product = new ProductCardView(c.productId(), c.variantId(), c.slug(), c.name(), c.imageUrl(),
                        c.available() ? c.price() : null, c.available());
            }
        }
        OrderCardView order = null;
        if (m.getOrderId() != null) {
            ChatOrderLookup.OrderCard c = orderCards.get(m.getOrderId());
            if (c != null) {
                order = new OrderCardView(c.id(), c.code(), c.status());
            }
        }
        return new MessageView(m.getId(), m.getSenderId() == viewerId ? "ME" : "THEM", m.getType().name(),
                m.getContent(), product, order, imageUrl(m.getAttachmentUrl()), m.getCreatedAt(), m.status(),
                Boolean.TRUE.equals(m.getIsAuto()), null);
    }

    /** Khóa trong bucket riêng tư → link ký 10 phút, tạo lại mỗi lần đọc. */
    private String imageUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            return storage.presignGet(UploadPurpose.CHAT_IMAGE.bucket(), objectKey, IMAGE_TTL);
        } catch (RuntimeException e) {
            log.warn("Không ký được link ảnh chat {}: {}", objectKey, e.getMessage());
            return null;
        }
    }
}
