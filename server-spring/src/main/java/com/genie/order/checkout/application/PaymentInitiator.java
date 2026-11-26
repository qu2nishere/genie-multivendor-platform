package com.genie.order.checkout.application;

import com.genie.common.domain.PaymentMethod;
import java.time.OffsetDateTime;

/**
 * Cổng khởi tạo thanh toán online cho một phiên checkout (API 6.10 khối {@code payment}; plan C2 D3). Chuỗi C3
 * (UC-PAY-01 — VNPay / VietQR + SePay) cài bản thật; bản mặc định ({@link CheckoutPorts}, {@code PAYMENT_MODE=stub})
 * trả khối {@code payment} không có liên kết / mã QR — đủ để kiểm cron hủy phiên bỏ dở (UC-ORD-08).
 */
public interface PaymentInitiator {

    /** Phiên cần thanh toán: id, mã {@code CS-…}, số tiền (VND), hạn giữ chỗ, URL quay lại của client. */
    record PaymentRequest(long sessionId, String sessionCode, long userId, long amount, PaymentMethod method,
                          OffsetDateTime expiresAt, String returnUrl) {
    }

    /** Khối {@code payment} (API 6.10): VNPAY → {@code payment_url}; VIETQR → {@code vietqr}; COD → chỉ {@code method}. */
    record Payment(String method, String paymentUrl, Object vietqr) {

        public static Payment cod() {
            return new Payment(PaymentMethod.COD.name(), null, null);
        }
    }

    Payment initiate(PaymentRequest request);
}
