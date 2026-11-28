// Lệnh gửi tin — chung cho REST 8.4 và STOMP /app/chat.send (plan 2026-11-26 chat D1, N1)
package com.genie.social.chat.application;

/**
 * @param type            {@code TEXT | IMAGE | PRODUCT_CARD | ORDER_CARD}
 * @param clientMessageId UUID client tự sinh (tùy chọn) — gửi lại cùng giá trị → cùng tin (D4)
 */
public record SendCommand(String type, String text, String imageObjectKey, Long productId, Long variantId, Long orderId,
                          String clientMessageId) {

    public static SendCommand text(String text, String clientMessageId) {
        return new SendCommand("TEXT", text, null, null, null, null, clientMessageId);
    }
}
