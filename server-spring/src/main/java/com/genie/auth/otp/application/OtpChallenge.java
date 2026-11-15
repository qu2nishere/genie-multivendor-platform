package com.genie.auth.otp.application;

import java.time.Instant;
import java.util.Map;

/**
 * Một lần gửi mã OTP đang chờ xác minh — lưu ở Redis {@code otp:challenge:{id}}, TTL 5 phút (plan AUTH D4).
 *
 * @param id          mã challenge trả cho client ({@code challenge_id})
 * @param purpose     mục đích
 * @param email       email nhận mã (đã chuẩn hóa)
 * @param userId      NULLABLE — chủ tài khoản (null với REGISTER, hoặc khi email không tồn tại — challenge "giả")
 * @param codeHash    SHA-256 của mã (kèm id + pepper); rỗng với challenge giả → không bao giờ khớp
 * @param attempts    số lần nhập sai đã ghi nhận
 * @param resendCount số lần gửi lại
 * @param expiresAt   thời điểm hết hạn
 * @param data        dữ liệu kèm theo luồng (ví dụ email mới khi đổi email)
 */
public record OtpChallenge(String id, OtpPurpose purpose, String email, Long userId, String codeHash, int attempts,
                           int resendCount, Instant expiresAt, Map<String, String> data) {

    public OtpChallenge {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
