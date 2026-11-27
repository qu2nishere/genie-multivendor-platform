// Admin › Thông báo sàn — UC-NOT-05 (API_SPEC 11.17; plan 2026-11-26 thông báo N06.08). Quyền MANAGE_PLATFORM_NOTIFICATION.
package com.genie.notification.platform.api;

import com.genie.common.api.ApiListResponse;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import com.genie.notification.platform.application.PlatformNotificationAdminService;
import com.genie.notification.platform.application.PlatformNotificationQueryService;
import com.genie.notification.platform.application.PlatformNotificationViews;
import io.swagger.v3.oas.annotations.Operation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/platform-notifications")
@PreAuthorize("hasAuthority('MANAGE_PLATFORM_NOTIFICATION')")
public class AdminPlatformNotificationController {

    private final PlatformNotificationAdminService admin;
    private final PlatformNotificationQueryService query;
    private final ApiResponseFactory responses;

    public AdminPlatformNotificationController(PlatformNotificationAdminService admin,
                                               PlatformNotificationQueryService query, ApiResponseFactory responses) {
        this.admin = admin;
        this.query = query;
        this.responses = responses;
    }

    /** Thân {@code POST} / {@code PUT} (API 11.17). */
    public record NotificationRequest(String title, String contentHtml, String imageObjectKey, String target,
                                      List<String> recipientEmails, OffsetDateTime sendAt) {
        PlatformNotificationViews.Command toCommand() {
            return new PlatformNotificationViews.Command(title, contentHtml, imageObjectKey, target, recipientEmails, sendAt);
        }
    }

    @Operation(summary = "UC-NOT-05 — Danh sách thông báo sàn + thống kê tháng",
            description = "target ∈ ALL, CUSTOMER, VENDOR, SPECIFIC_USERS; q tìm tiêu đề / nội dung; page / size (mặc định 5). "
                    + "summary: sent_this_month, total_reach, total_reads.")
    @GetMapping
    public ResponseEntity<ApiListResponse<PlatformNotificationViews.Row>> list(
            @RequestParam(required = false) String target, @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        PlatformNotificationQueryService.ListResult r = query.list(target, q, page, size);
        return responses.page(r.page(), r.summary());
    }

    @Operation(summary = "UC-NOT-05 — Chi tiết thông báo sàn (kèm content_html, recipient_emails)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformNotificationViews.Detail>> detail(@PathVariable long id) {
        return responses.ok(query.detail(id));
    }

    @Operation(summary = "UC-NOT-05 — Soạn và phát / hẹn giờ thông báo sàn",
            description = "send_at = null → phát ngay; 422 GNE-NOT-4221 khi người nhận rỗng / email lạ (details.unknown_emails).")
    @PostMapping
    public ResponseEntity<ApiResponse<PlatformNotificationViews.Detail>> create(@CurrentUser AuthUser me,
                                                                                @RequestBody NotificationRequest body,
                                                                                ClientInfo client) {
        return responses.created(admin.create(me.id(), body.toCommand(), client));
    }

    @Operation(summary = "UC-NOT-05 — Sửa thông báo sàn chưa phát",
            description = "409 GNE-NOT-4091 nếu đã phát; image_object_key null = giữ ảnh, chuỗi rỗng = bỏ ảnh.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformNotificationViews.Detail>> update(@CurrentUser AuthUser me,
                                                                                @PathVariable long id,
                                                                                @RequestBody NotificationRequest body,
                                                                                ClientInfo client) {
        return responses.ok(admin.update(me.id(), id, body.toCommand(), client));
    }

    @Operation(summary = "UC-NOT-05 — Xóa thông báo sàn (kể cả đã phát — gỡ khỏi hộp thư mọi người)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthUser me, @PathVariable long id, ClientInfo client) {
        admin.delete(me.id(), id, client);
        return ResponseEntity.noContent().build();
    }
}
