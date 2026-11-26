package com.genie.order.checkout.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.order.checkout.application.CheckoutService;
import com.genie.order.checkout.application.CheckoutView;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Phiên checkout & đơn con theo shop — UC-ORD-07 (API_SPEC 6.10, 6.11; plan C2 O07.05). */
@RestController
@RequestMapping("/api/v1/me/checkout-sessions")
public class CheckoutSessionController {

    private final CheckoutService checkout;
    private final ApiResponseFactory responses;

    public CheckoutSessionController(CheckoutService checkout, ApiResponseFactory responses) {
        this.checkout = checkout;
        this.responses = responses;
    }

    @Operation(summary = "UC-ORD-07 — Đặt hàng (tách đơn theo shop, giữ chỗ 15 phút)",
            description = "Bắt buộc header Idempotency-Key — gửi lại cùng khóa trả phiên cũ. COD → COMPLETED ngay; VNPAY / "
                    + "VIETQR → PAYMENT_PENDING + khối payment. 409 GNE-INV-4090 (details.out_of_stock_cart_item_ids), "
                    + "GNE-ORD-4093 (details.shop_ids), GNE-ORD-4094; 503 GNE-SYS-5030 thử lại cùng khóa.")
    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutView>> create(@CurrentUser AuthUser me,
                                                            @RequestHeader(value = "Idempotency-Key", required = false)
                                                            String idempotencyKey,
                                                            @Valid @RequestBody CreateCheckoutRequest req) {
        CheckoutView v = checkout.place(me.id(), req.command(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(responses.created(v).getBody());
    }

    @Operation(summary = "UC-ORD-07 — Kết quả đặt hàng / trạng thái thanh toán",
            description = "Chỉ chủ phiên; phiên của người khác → 404.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CheckoutView>> get(@CurrentUser AuthUser me, @PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(checkout.get(me.id(), id)).getBody());
    }
}
