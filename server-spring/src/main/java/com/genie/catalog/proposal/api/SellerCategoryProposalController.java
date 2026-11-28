// API 10.6 — Đề xuất danh mục mới của người bán (UC-CAT-17; plan C1 P08.02)
package com.genie.catalog.proposal.api;

import com.genie.catalog.proposal.application.CategoryProposalService;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/category-proposals")
public class SellerCategoryProposalController {

    private final CategoryProposalService proposals;
    private final ApiResponseFactory responses;

    public SellerCategoryProposalController(CategoryProposalService proposals, ApiResponseFactory responses) {
        this.proposals = proposals;
        this.responses = responses;
    }

    @Operation(summary = "UC-CAT-17 — Đề xuất danh mục của shop")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryProposalService.SellerView>>> mine(@CurrentUser AuthUser me) {
        return responses.ok(proposals.mine(me.id()));
    }

    @Operation(summary = "UC-CAT-17 — Gửi đề xuất danh mục mới")
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryProposalService.SellerView>> create(
            @CurrentUser AuthUser me, @RequestBody CategoryProposalService.CreateRequest body) {
        return responses.created(proposals.create(me.id(), body));
    }
}
