// Gói tin /user/queue/chat (API_SPEC 13; plan 2026-11-26 chat D15)
package com.genie.social.chat.application;

import com.genie.social.chat.application.ChatViews.MessageView;

/** Bốn loại gói tin của kênh chat — JSON snake_case. */
public final class ChatEvents {

    public static final String MESSAGE_CREATED = "MESSAGE_CREATED";
    public static final String MESSAGE_SEEN = "MESSAGE_SEEN";
    public static final String TYPING = "TYPING";
    public static final String MESSAGE_FAILED = "MESSAGE_FAILED";

    private ChatEvents() {
    }

    /** Tin mới — gửi cho <b>cả hai</b> bên, {@code message} theo góc nhìn người nhận gói tin. */
    public record MessageCreated(String type, long conversationId, MessageView message, String clientMessageId,
                                 long unreadTotal) {
    }

    /** Bên kia đã mở hội thoại: tin của mình tới {@code upToMessageId} thành "Đã xem". */
    public record MessageSeen(String type, long conversationId, long upToMessageId) {
    }

    public record Typing(String type, long conversationId) {
    }

    /** Gửi qua {@code /app/chat.send} bị từ chối — chỉ về chính người gửi. */
    public record MessageFailed(String type, String clientMessageId, Long conversationId, String code,
                                String message) {
    }
}
