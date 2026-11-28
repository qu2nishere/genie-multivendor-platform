// API 11.11 — Duyệt đề xuất danh mục (UC-CAT-17; plan C1 P08.02). MANAGE_CATALOG; gửi hoa hồng cần thêm UPDATE_COMMISSION.
package com.genie.catalog.proposal.api;

import com.genie.catalog.proposal.application.CategoryProposalService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/category-proposals")
@PreAuthorize("hasAuthority('MANAGE_CATALOG')")
public class AdminCategoryProposalController {

    private final CategoryProposalService proposals;
    private final ApiResponseFactory responses;

    public AdminCategoryProposalController(CategoryProposalService proposals, ApiResponseFactory responses) {
        this.proposals = proposals;
        this.responses = responses;
    }

    public record ApproveRequest(BigDecimal commissionRate) {
    }

    public record RejectRequest(String reason) {
    }

    @Operation(summary = "UC-CAT-17 — Hàng chờ đề xuất danh mục")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryProposalService.AdminView>>> list(
            @RequestParam(required = false) String status) {
        return responses.ok(proposals.forAdmin(status));
    }

    @Operation(summary = "UC-CAT-17 — Duyệt đề xuất (tạo danh mục dưới đúng cha)")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<CategoryProposalService.Approved>> approve(
            @CurrentUser AuthUser me, Authentication auth, @PathVariable long id,
            @RequestBody(required = false) ApproveRequest body, ClientInfo client) {
        boolean canCommission = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "UPDATE_COMMISSION".equals(a.getAuthority()));
        return responses.ok(proposals.approve(me.id(), id, body == null ? null : body.commissionRate(),
                canCommission, client));
    }

    @Operation(summary = "UC-CAT-17 — Từ chối đề xuất kèm lý do")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(@CurrentUser AuthUser me, @PathVariable long id,
                                                                  @RequestBody(required = false) RejectRequest body) {
        proposals.reject(me.id(), id, body == null ? null : body.reason());
        return responses.ok(Map.of("id", id, "status", "REJECTED"));
    }
}
