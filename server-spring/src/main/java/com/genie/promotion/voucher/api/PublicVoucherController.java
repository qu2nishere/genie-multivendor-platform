// Voucher công khai để "Lưu" (API_SPEC 5.13; plan 2026-11-26 voucher V05.05)
package com.genie.promotion.voucher.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.promotion.voucher.application.PublicVoucherService;
import com.genie.promotion.voucher.application.VoucherView;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Công khai; kèm token thì có {@code is_saved} — nên không cache ở trình duyệt / CDN (danh sách đã cache Redis 60 giây). */
@RestController
@RequestMapping("/api/v1/public/vouchers")
public class PublicVoucherController {

    private final PublicVoucherService vouchers;
    private final ApiResponseFactory responses;

    public PublicVoucherController(PublicVoucherService vouchers, ApiResponseFactory responses) {
        this.vouchers = vouchers;
        this.responses = responses;
    }

    @Operation(summary = "UC-PRO-03 — Voucher công khai (trang chủ / trang shop / chiến dịch)",
            description = "Đúng một trong shop_id | code | codes (≤ 20, phẩy) | featured=true&limit=1..12. Chỉ voucher đang "
                    + "chạy / sắp diễn ra, không có quà đổi Điểm.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherView>>> list(@CurrentUser AuthUser me,
            @RequestParam(name = "shop_id", required = false) Long shopId,
            @RequestParam(required = false) String code, @RequestParam(required = false) String codes,
            @RequestParam(required = false) Boolean featured, @RequestParam(required = false) Integer limit) {
        List<VoucherView> data = vouchers.list(shopId, code, codes, featured, limit, me == null ? null : me.id());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.body(data));
    }
}
