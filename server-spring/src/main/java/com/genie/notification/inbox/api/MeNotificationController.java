// Hộp thư thông báo của người đang đăng nhập — UC-NOT-01, UC-NOT-05 (API_SPEC 3.12; plan 2026-11-26 thông báo N04.07)
package com.genie.notification.inbox.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.notification.inbox.application.InboxQueryService;
import com.genie.notification.inbox.application.InboxReadService;
import com.genie.notification.inbox.application.NotificationView;
import com.genie.notification.inbox.domain.NotificationCategory;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mọi vai trò dùng chung một endpoint; {@code userId} luôn lấy từ token (chống IDOR). */
@RestController
@RequestMapping("/api/v1/me/notifications")
public class MeNotificationController {

    private final InboxQueryService query;
    private final InboxReadService reads;
    private final ApiResponseFactory responses;

    public MeNotificationController(InboxQueryService query, InboxReadService reads, ApiResponseFactory responses) {
        this.query = query;
        this.reads = reads;
        this.responses = responses;
    }

    @Operation(summary = "UC-NOT-01 — Hộp thư thông báo (cá nhân + thông báo sàn + thông báo nhân viên)",
            description = "category ∈ ORDER, PROMOTION, SYSTEM, CHAT (trống = tất cả); cursor; limit 1–50 (mặc định 20); "
                    + "summary.unread_count = tổng chưa đọc của mọi nguồn.")
    @GetMapping
    public ResponseEntity<ApiListResponse<NotificationView>> list(@CurrentUser AuthUser me,
                                                                  @RequestParam(required = false) String category,
                                                                  @RequestParam(required = false) String cursor,
                                                                  @RequestParam(required = false) Integer limit) {
        InboxQueryService.InboxPage page = query.page(me.id(), parseCategory(category), cursor, limit);
        var body = responses.list(page.page(), Map.of("unread_count", page.unreadCount())).getBody();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    public record ReadRequest(Boolean isRead) {
    }

    @Operation(summary = "UC-NOT-01 — Đánh dấu một thông báo đã đọc",
            description = "{is_read: true} → 204 (gọi lại vẫn 204). is_read false → 422. id của người khác / ngoài đối tượng → 404.")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> markRead(@CurrentUser AuthUser me, @PathVariable String id,
                                         @RequestBody(required = false) ReadRequest body) {
        if (body == null || !Boolean.TRUE.equals(body.isRead())) {
            throw InvalidRequestException.of("is_read", "UNSUPPORTED", body == null ? null : body.isRead(),
                    "Chỉ hỗ trợ đánh dấu đã đọc (is_read = true).");
        }
        reads.markRead(me.id(), id);
        return ResponseEntity.noContent().build();
    }

    public record MarkAllRequest(String category) {
    }

    @Operation(summary = "UC-NOT-01 — Đánh dấu tất cả đã đọc (theo nhóm)",
            description = "{category: null | ORDER | PROMOTION | SYSTEM | CHAT} → 204.")
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(@CurrentUser AuthUser me, @RequestBody(required = false) MarkAllRequest body) {
        reads.markAllRead(me.id(), parseCategory(body == null ? null : body.category()));
        return ResponseEntity.noContent().build();
    }

    static NotificationCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return NotificationCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidRequestException.of("category", "INVALID_VALUE", raw,
                    "category phải là ORDER, PROMOTION, SYSTEM hoặc CHAT.");
        }
    }
}
