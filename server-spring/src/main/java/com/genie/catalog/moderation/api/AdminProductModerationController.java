// API_SPEC 11.12 Duyệt sản phẩm — UC-CAT-13 (plan C1 P07.05). Quyền MANAGE_CATALOG.
package com.genie.catalog.moderation.api;

import com.genie.catalog.moderation.application.ModerationDecisionService;
import com.genie.catalog.moderation.application.ModerationQueueService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.PageResponse;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('MANAGE_CATALOG')")
public class AdminProductModerationController {

    private final ModerationQueueService queue;
    private final ModerationDecisionService decisions;
    private final ApiResponseFactory responses;

    public AdminProductModerationController(ModerationQueueService queue, ModerationDecisionService decisions,
                                            ApiResponseFactory responses) {
        this.queue = queue;
        this.decisions = decisions;
        this.responses = responses;
    }

    /** {@code reason_code} theo API 16 (MISSING_SAFETY_CERT, POLICY_VIOLATION, BAD_IMAGE, …, OTHER cần ghi chú). */
    public record RejectRequest(String reasonCode, String note) {
    }

    public record AssigneeRequest(Long staffId) {
    }

    public record RedistributeResult(int reassignedCount) {
    }

    @Operation(summary = "UC-CAT-13 — Hàng chờ duyệt sản phẩm (tab PENDING / PUBLISHED / REJECTED, summary.counts)")
    @GetMapping("/product-moderation")
    public ResponseEntity<PageResponse<ModerationQueueService.Row>> list(@RequestParam(required = false) String tab,
                                                                        @RequestParam(required = false) String q,
                                                                        @RequestParam(required = false) Integer page,
                                                                        @RequestParam(required = false) Integer size) {
        ModerationQueueService.Page result = queue.list(tab, q, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.pageBody(result.items(), result.pagination(), Map.of("counts", result.counts())));
    }

    @Operation(summary = "UC-CAT-13 — Chi tiết sản phẩm cần duyệt (kết quả AI, giấy tờ, bảng SKU)")
    @GetMapping("/product-moderation/{productId}")
    public ResponseEntity<ApiResponse<ModerationQueueService.Detail>> detail(@PathVariable long productId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.body(queue.detail(productId)));
    }

    @Operation(summary = "UC-CAT-13 — Duyệt sản phẩm lên sàn")
    @PostMapping("/products/{id}/approve")
    public ResponseEntity<ApiResponse<ModerationDecisionService.Result>> approve(@CurrentUser AuthUser me,
                                                                                @PathVariable long id, ClientInfo client) {
        return responses.ok(decisions.approve(me.id(), id, client));
    }

    @Operation(summary = "UC-CAT-13 — Từ chối sản phẩm kèm lý do chuẩn")
    @PostMapping("/products/{id}/reject")
    public ResponseEntity<ApiResponse<ModerationDecisionService.Result>> reject(@CurrentUser AuthUser me,
                                                                               @PathVariable long id,
                                                                               @RequestBody RejectRequest body,
                                                                               ClientInfo client) {
        return responses.ok(decisions.reject(me.id(), id, body == null ? null : body.reasonCode(),
                body == null ? null : body.note(), client));
    }

    @Operation(summary = "UC-CAT-13 — Đưa sản phẩm đã quyết về hàng chờ (Chờ duyệt lại)")
    @PostMapping("/products/{id}/requeue")
    public ResponseEntity<ApiResponse<ModerationDecisionService.Result>> requeue(@CurrentUser AuthUser me,
                                                                                @PathVariable long id, ClientInfo client) {
        return responses.ok(decisions.requeue(me.id(), id, client));
    }

    @Operation(summary = "UC-CAT-13 — Giao lại người duyệt")
    @PutMapping("/products/{id}/assignee")
    public ResponseEntity<ApiResponse<ModerationDecisionService.Result>> assign(@CurrentUser AuthUser me,
                                                                               @PathVariable long id,
                                                                               @RequestBody AssigneeRequest body,
                                                                               ClientInfo client) {
        return responses.ok(decisions.assign(me.id(), id, body == null ? null : body.staffId(), client));
    }

    @Operation(summary = "UC-CAT-13 — Phân phối lại hàng chờ cho nhân viên đang nhận việc")
    @PostMapping("/moderators/redistribute")
    public ResponseEntity<ApiResponse<RedistributeResult>> redistribute() {
        return responses.ok(new RedistributeResult(queue.redistribute()));
    }
}
