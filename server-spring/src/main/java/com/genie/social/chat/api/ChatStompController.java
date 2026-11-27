// Cửa STOMP của chat: /app/chat.send | chat.read | chat.typing (API_SPEC 13; plan 2026-11-26 chat M04.02, N1, D1)
package com.genie.social.chat.api;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.AuthUser;
import com.genie.common.security.StompAuthInterceptor.StompUser;
import com.genie.social.chat.application.ChatRealtime;
import com.genie.social.chat.application.ChatService;
import com.genie.social.chat.application.SendCommand;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * Gọi <b>cùng</b> {@link ChatService} với REST — một luật nghiệp vụ, hai cửa (N1). Kết quả đi qua {@code /user/queue/chat}
 * (người gửi nhận lại {@code MESSAGE_CREATED} làm ack). Lỗi nghiệp vụ không làm rớt kết nối: trả {@code MESSAGE_FAILED}
 * về chính người gửi. Tài khoản nội bộ (STAFF / ADMIN) bị từ chối như ở REST (D5).
 */
@Controller
public class ChatStompController {

    private static final Logger log = LoggerFactory.getLogger(ChatStompController.class);

    private final ChatService chat;
    private final ChatRealtime realtime;

    public ChatStompController(ChatService chat, ChatRealtime realtime) {
        this.chat = chat;
        this.realtime = realtime;
    }

    /** Khung {@code SEND /app/chat.send}. */
    public record SendFrame(Long conversationId, String clientMessageId, String type, String text,
                            String imageObjectKey, Long productId, Long variantId, Long orderId) {
    }

    public record ConversationFrame(Long conversationId) {
    }

    @MessageMapping("chat.send")
    public void send(@Payload SendFrame frame, Principal principal) {
        AuthUser me = userOf(principal);
        if (me == null) {
            return;
        }
        run(me, frame.clientMessageId(), frame.conversationId(), () -> {
            requireConversation(frame.conversationId());
            chat.send(me.id(), null, frame.conversationId(), new SendCommand(frame.type(), frame.text(),
                    frame.imageObjectKey(), frame.productId(), frame.variantId(), frame.orderId(),
                    frame.clientMessageId()));
        });
    }

    @MessageMapping("chat.read")
    public void read(@Payload ConversationFrame frame, Principal principal) {
        AuthUser me = userOf(principal);
        if (me != null) {
            run(me, null, frame.conversationId(), () -> {
                requireConversation(frame.conversationId());
                chat.read(me.id(), null, frame.conversationId());
            });
        }
    }

    @MessageMapping("chat.typing")
    public void typing(@Payload ConversationFrame frame, Principal principal) {
        AuthUser me = userOf(principal);
        if (me == null || me.isStaffOrAdmin() || frame.conversationId() == null) {
            return;
        }
        try {
            chat.typing(me.id(), frame.conversationId());
        } catch (RuntimeException ignored) {
            // "đang gõ" không quan trọng — không báo lỗi
        }
    }

    private void run(AuthUser me, String cmid, Long conversationId, Runnable action) {
        try {
            if (me.isStaffOrAdmin()) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            action.run();
        } catch (BusinessException e) {
            realtime.failed(me.id(), cmid, conversationId, e.getErrorCode().code(), e.getMessage());
        } catch (InvalidRequestException e) {
            String message = e.getViolations().isEmpty() ? ErrorCode.VALIDATION_FAILED.defaultMessage()
                    : e.getViolations().get(0).message();
            realtime.failed(me.id(), cmid, conversationId, ErrorCode.VALIDATION_FAILED.code(), message);
        } catch (RuntimeException e) {
            log.warn("Lỗi xử lý khung chat của {}: {}", me.id(), e.toString());
            realtime.failed(me.id(), cmid, conversationId, ErrorCode.INTERNAL_ERROR.code(),
                    ErrorCode.INTERNAL_ERROR.defaultMessage());
        }
    }

    private static void requireConversation(Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            throw InvalidRequestException.of("conversation_id", "REQUIRED", conversationId, "Thiếu conversation_id.");
        }
    }

    private static AuthUser userOf(Principal principal) {
        return principal instanceof StompUser u ? u.user() : null;
    }
}
