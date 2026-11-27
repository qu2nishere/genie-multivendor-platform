// Chat phía khách (API_SPEC 8.1–8.6; plan 2026-11-26 chat M05.01, D5)
package com.genie.social.chat.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.social.chat.application.ChatQueryService;
import com.genie.social.chat.application.ChatService;
import com.genie.social.chat.application.ChatViews.MessageView;
import com.genie.social.chat.application.ChatViews.OpenedConversation;
import com.genie.social.chat.application.ChatViews.OrderPickView;
import com.genie.social.chat.application.ChatViews.ConversationView;
import com.genie.social.chat.domain.Conversation.Side;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Người mua (CUSTOMER, hoặc chủ shop chat với shop <b>khác</b> — VENDOR kế thừa CUSTOMER). Tài khoản nội bộ không có vai
 * trò CUSTOMER → {@code 403 GNE-SEC-4030}. Chỉ hội thoại có {@code customer_id = me} (N5).
 */
@RestController
@RequestMapping("/api/v1/me/conversations")
@PreAuthorize("hasRole('CUSTOMER')")
public class MeConversationController {

    private final ChatService chat;
    private final ChatQueryService query;
    private final ApiResponseFactory responses;

    public MeConversationController(ChatService chat, ChatQueryService query, ApiResponseFactory responses) {
        this.chat = chat;
        this.query = query;
        this.responses = responses;
    }

    @Operation(summary = "UC-SOC-01 — Hộp thư chat của khách",
            description = "Chỉ hội thoại đã có tin, mới nhất trước; cursor, limit 1–50; summary.unread_total = badge nút chat.")
    @GetMapping
    public ResponseEntity<ApiListResponse<ConversationView>> list(@CurrentUser AuthUser me,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        ChatQueryService.InboxPage r = query.conversations(me.id(), Side.CUSTOMER, cursor, limit);
        return noStore(responses.list(r.page(), Map.of("unread_total", r.unreadTotal())));
    }

    @Operation(summary = "UC-SOC-01 — Mở (hoặc lấy) hội thoại với shop",
            description = "201 mới / 200 đã có; shop không hoạt động → 404 GNE-VND-4041; shop của mình → 409 GNE-SOC-4096.")
    @PostMapping
    public ResponseEntity<ApiResponse<OpenedConversation>> open(@CurrentUser AuthUser me,
            @Valid @RequestBody ChatRequests.OpenRequest body) {
        ChatService.Opened o = chat.open(me.id(), body.shopId());
        return o.created() ? responses.created(o.view()) : responses.ok(o.view());
    }

    @Operation(summary = "UC-SOC-01 — Lịch sử tin (cuộn lên)",
            description = "Mới nhất trước; before = next_cursor của trang trước; limit 1–50 (mặc định 30).")
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiListResponse<MessageView>> messages(@CurrentUser AuthUser me, @PathVariable @Positive long id,
            @RequestParam(required = false) String before, @RequestParam(required = false) Integer limit) {
        return noStore(responses.list(query.messages(me.id(), Side.CUSTOMER, id, before, limit), null));
    }

    @Operation(summary = "UC-SOC-01 / UC-SOC-02 — Gửi tin (đường dự phòng của /app/chat.send)",
            description = "TEXT / IMAGE / PRODUCT_CARD / ORDER_CARD; 201 tin mới, 200 khi gửi lại cùng client_message_id.")
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageView>> send(@CurrentUser AuthUser me, @PathVariable @Positive long id,
            @Valid @RequestBody ChatRequests.SendRequest body) {
        ChatService.Sent s = chat.send(me.id(), Side.CUSTOMER, id, body.toCommand());
        return s.created() ? responses.created(s.view()) : responses.ok(s.view());
    }

    @Operation(summary = "UC-SOC-01 — Đánh dấu đã xem", description = "204; bên kia nhận MESSAGE_SEEN.")
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@CurrentUser AuthUser me, @PathVariable @Positive long id) {
        chat.read(me.id(), Side.CUSTOMER, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "UC-SOC-02 — Đơn gần nhất giữa hai bên (ô Gửi đơn)", description = "limit 1–20, mặc định 10.")
    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<List<OrderPickView>>> orders(@CurrentUser AuthUser me,
            @PathVariable @Positive long id, @RequestParam(required = false) Integer limit) {
        return noStore(responses.ok(query.orders(me.id(), Side.CUSTOMER, id, limit)));
    }

    static <T> ResponseEntity<T> noStore(ResponseEntity<T> r) {
        return ResponseEntity.status(r.getStatusCode()).cacheControl(CacheControl.noStore()).body(r.getBody());
    }
}
