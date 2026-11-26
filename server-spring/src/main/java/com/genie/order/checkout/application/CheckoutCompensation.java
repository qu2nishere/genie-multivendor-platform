package com.genie.order.checkout.application;

import java.util.List;

/**
 * Cổng hoàn lại những gì phiên checkout đã "tiêu" khi phiên bị hủy vì quá hạn thanh toán (UC-ORD-08; plan C2 D13): lượt
 * dùng voucher, Điểm Genie, suất Flash Sale. Chuỗi C6 (UC-PRO-04, PRO-06, PRO-10) cài bản thật; bản mặc định
 * ({@link CheckoutPorts}) không làm gì vì C2 chưa trừ các thứ đó. Gọi TRONG transaction hủy phiên.
 */
public interface CheckoutCompensation {

    void onExpired(long sessionId, long userId, List<Long> orderIds);
}
