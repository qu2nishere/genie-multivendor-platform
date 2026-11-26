package com.genie.order.checkout.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.order.checkout.application.CheckoutPricer;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xem trước & tách giỏ theo shop — UC-ORD-04, UC-ORD-06 (API_SPEC 6.8; plan C2 O06.04). Tài khoản nội bộ bị
 * {@code BuyerOnlyFilter} chặn trước ({@code 403 GNE-ORD-4031}).
 */
@RestController
@RequestMapping("/api/v1/me/checkout")
public class CheckoutController {

    private final CheckoutPricer pricer;
    private final ApiResponseFactory responses;

    public CheckoutController(CheckoutPricer pricer, ApiResponseFactory responses) {
        this.pricer = pricer;
        this.responses = responses;
    }

    @Operation(summary = "UC-ORD-04 — Tính giá giỏ hàng & thanh toán (tách đơn theo shop)",
            description = "address_id trống → địa chỉ mặc định; không có địa chỉ → address = null, shipping_fee = null. "
                    + "Dòng của người khác bị bỏ qua. Phí ship UC-ORD-06 (is_estimated_fee khi dùng bảng dự phòng). "
                    + "Chưa áp voucher / Điểm (C6): các khoản giảm = 0.")
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<PreviewResponse>> preview(@CurrentUser AuthUser me,
                                                                @Valid @RequestBody PreviewRequest req) {
        var priced = pricer.price(me.id(), req.cartItemIds(), req.addressId(),
                PreviewRequest.selection(req.vouchers(), req.useCoins()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.ok(PreviewResponse.of(priced)).getBody());
    }
}
