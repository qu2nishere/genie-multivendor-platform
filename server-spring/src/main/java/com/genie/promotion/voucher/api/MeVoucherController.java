// Kho Voucher (API_SPEC 3.14; plan 2026-11-26 voucher V05.05) — tài khoản nội bộ bị BuyerOnlyFilter chặn (403 GNE-ORD-4031)
package com.genie.promotion.voucher.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.api.PageResponse;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.promotion.voucher.application.VoucherView;
import com.genie.promotion.voucher.application.VoucherWalletService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/vouchers")
public class MeVoucherController {

    private final VoucherWalletService wallet;
    private final ApiResponseFactory responses;

    public MeVoucherController(VoucherWalletService wallet, ApiResponseFactory responses) {
        this.wallet = wallet;
        this.responses = responses;
    }

    /** Thân lưu voucher: {@code code} hoặc {@code voucher_id}. */
    public record SaveRequest(String code, Long voucherId) {
    }

    @Operation(summary = "UC-PRO-03 — Kho Voucher",
            description = "state ∈ AVAILABLE (gồm sắp diễn ra), USED, EXPIRED; type ∈ PLATFORM, SHOP, FREESHIP; "
                    + "summary.counts theo loại; page từ 1, size 1–50 (mặc định 20).")
    @GetMapping
    public ResponseEntity<PageResponse<VoucherView>> list(@CurrentUser AuthUser me,
            @RequestParam(required = false) String state, @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        VoucherWalletService.WalletPage p = wallet.page(me.id(), state, type, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.pageBody(p.items(), p.pagination(), p.summary()));
    }

    @Operation(summary = "UC-PRO-03 — Lưu voucher vào Kho",
            description = "404 GNE-VCH-4041 (không tồn tại / đã kết thúc / quà đổi Điểm); 409 GNE-VCH-4091 đã lưu hoặc "
                    + "details.reason = USAGE_LIMIT_REACHED.")
    @PostMapping
    public ResponseEntity<ApiResponse<VoucherView>> save(@CurrentUser AuthUser me, @RequestBody SaveRequest body) {
        return responses.created(wallet.save(me.id(), body.code(), body.voucherId()));
    }

    @Operation(summary = "UC-PRO-03 — Xóa voucher khỏi Kho (idempotent)")
    @DeleteMapping("/{voucherId}")
    public ResponseEntity<Void> remove(@CurrentUser AuthUser me, @PathVariable @Positive long voucherId) {
        wallet.remove(me.id(), voucherId);
        return ResponseEntity.noContent().build();
    }
}
