// Chat phía shop — Seller Center › Chat (API_SPEC 8.1, 8.3–8.6; plan 2026-11-26 chat M05.02)
package com.genie.social.chat.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.social.chat.application.ChatQueryService;
import com.genie.social.chat.application.ChatService;
import com.genie.social.chat.application.ChatViews.ConversationView;
import com.genie.social.chat.application.ChatViews.MessageView;
import com.genie.social.chat.application.ChatViews.OrderPickView;
import com.genie.social.chat.domain.Conversation.Side;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /seller/**} đã yêu cầu vai trò VENDOR (SecurityConfig) và shop {@code VERIFIED} (cổng KYC — {@code 403 GNE-VND-4031}).
 * Chỉ hội thoại có {@code vendor_id = me} (N5); bên kia là khách hàng.
 */
@RestController
@RequestMapping("/api/v1/seller/conversations")
public class SellerConversationController {

    private final ChatService chat;
    private final ChatQueryService query;
    private final ApiResponseFactory responses;

    public SellerConversationController(ChatService chat, ChatQueryService query, ApiResponseFactory responses) {
        this.chat = chat;
        this.query = query;
        this.responses = responses;
    }

    @Operation(summary = "UC-SOC-01 — Hộp thư chat của shop", description = "summary.unread_total = badge nav Chat.")
    @GetMapping
    public ResponseEntity<ApiListResponse<ConversationView>> list(@CurrentUser AuthUser me,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        ChatQueryService.InboxPage r = query.conversations(me.id(), Side.VENDOR, cursor, limit);
        return MeConversationController.noStore(responses.list(r.page(), Map.of("unread_total", r.unreadTotal())));
    }

    @Operation(summary = "UC-SOC-01 — Lịch sử tin (phía shop)")
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiListResponse<MessageView>> messages(@CurrentUser AuthUser me, @PathVariable @Positive long id,
            @RequestParam(required = false) String before, @RequestParam(required = false) Integer limit) {
        return MeConversationController.noStore(responses.list(query.messages(me.id(), Side.VENDOR, id, before, limit),
                null));
    }

    @Operation(summary = "UC-SOC-01 / UC-SOC-02 — Shop gửi tin")
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageView>> send(@CurrentUser AuthUser me, @PathVariable @Positive long id,
            @Valid @RequestBody ChatRequests.SendRequest body) {
        ChatService.Sent s = chat.send(me.id(), Side.VENDOR, id, body.toCommand());
        return s.created() ? responses.created(s.view()) : responses.ok(s.view());
    }

    @Operation(summary = "UC-SOC-01 — Shop đánh dấu đã xem")
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@CurrentUser AuthUser me, @PathVariable @Positive long id) {
        chat.read(me.id(), Side.VENDOR, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "UC-SOC-02 — Đơn gần nhất giữa hai bên (phía shop)")
    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<OrderPickView>>> orders(@CurrentUser AuthUser me,
            @PathVariable @Positive long id, @RequestParam(required = false) Integer limit) {
        return MeConversationController.noStore(responses.ok(query.orders(me.id(), Side.VENDOR, id, limit)));
    }
}
