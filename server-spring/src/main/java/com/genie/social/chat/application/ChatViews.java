// Dạng trả về của chat (API_SPEC 8.1–8.6, 13; plan 2026-11-26 chat D3, D15, D16)
package com.genie.social.chat.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Record JSON (snake_case qua JacksonConfig) — mọi khóa luôn có mặt, khóa không dùng theo {@code type} là {@code null}. */
public final class ChatViews {

    private ChatViews() {
    }

    /** Bên kia của hội thoại: {@code SHOP} (phía khách, {@code id} = shop_id) hoặc {@code CUSTOMER} (phía shop). */
    public record Counterpart(String type, long id, String name, String avatarUrl, boolean isMall, boolean isOnline,
                              OffsetDateTime lastActiveAt) {
    }

    public record LastMessage(String type, String preview, OffsetDateTime sentAt) {
    }

    /** Một dòng hộp thư 8.1. */
    public record ConversationView(long id, Counterpart counterpart, LastMessage lastMessage, int unreadCount) {
    }

    public record ShopStats(BigDecimal chatResponseRate, Integer avgResponseMinutes) {
    }

    /** Phản hồi 8.2. */
    public record OpenedConversation(long id, Counterpart counterpart, ShopStats shopStats, int unreadCount) {
    }

    /** Thẻ sản phẩm — thông tin hiện tại; {@code price = null} khi không còn bán. */
    public record ProductCardView(long id, Long variantId, String slug, String name, String imageUrl, Long price,
                                  boolean isAvailable) {
    }

    public record OrderCardView(long id, String code, String status) {
    }

    /** Một tin 8.3 theo góc nhìn người xem. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record MessageView(long id, String sender, String type, String text, ProductCardView product,
                              OrderCardView order, String imageUrl, OffsetDateTime sentAt, String status,
                              boolean isAuto, String clientMessageId) {

        public MessageView withClientMessageId(String cmid) {
            return new MessageView(id, sender, type, text, product, order, imageUrl, sentAt, status, isAuto, cmid);
        }
    }

    /** Một dòng ô "Gửi đơn" 8.6. */
    public record OrderPickView(long id, String code, String status, long total, OffsetDateTime createdAt,
                                int itemCount, String thumbnailUrl) {
    }
}
