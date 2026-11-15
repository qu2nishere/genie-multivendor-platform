package com.genie.auth.otp.application;

import java.time.Duration;
import java.util.Optional;

/**
 * Cổng lưu challenge OTP (cài đặt Redis ở {@code auth.otp.infra.RedisOtpStore}).
 */
public interface OtpStore {

    void save(OtpChallenge challenge, Duration ttl);

    Optional<OtpChallenge> find(String id);

    /** Tăng số lần sai, trả số mới (nguyên tử — HINCRBY). */
    int incrementAttempts(String id);

    /** Xóa challenge; {@code true} chỉ với lời gọi thực sự xóa được (dùng một lần khi 2 request đúng đồng thời). */
    boolean delete(String id);

    /** Challenge đang sống của (mục đích, email) — để hủy challenge cũ khi xin mã mới. */
    Optional<String> activeId(OtpPurpose purpose, String email);

    void setActive(OtpPurpose purpose, String email, String id, Duration ttl);
}
