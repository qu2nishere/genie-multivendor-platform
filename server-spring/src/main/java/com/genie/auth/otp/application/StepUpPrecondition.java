package com.genie.auth.otp.application;

/**
 * Cổng kiểm điều kiện <b>trước khi gửi</b> mã step-up, để màn hình báo lỗi ngay (API_SPEC 3.5):
 * ACCOUNT_DELETION → còn đơn đang xử lý ({@code 409 GNE-USR-4091}); PAYOUT_ACCOUNT_DELETE → còn lệnh rút chờ
 * ({@code 409 GNE-WAL-4091}); WITHDRAWAL → chưa có tài khoản nhận tiền xác minh ({@code 403 GNE-WAL-4032}).
 * Module sở hữu dữ liệu (order, payment, vendor) cài đặt; không có cài đặt nào → cho gửi.
 */
public interface StepUpPrecondition {

    /** Mục đích mà cài đặt này kiểm. */
    boolean supports(OtpPurpose purpose);

    /** Ném {@code BusinessException} (kèm {@code details}) khi chưa đủ điều kiện. */
    void check(long userId, OtpPurpose purpose);
}
