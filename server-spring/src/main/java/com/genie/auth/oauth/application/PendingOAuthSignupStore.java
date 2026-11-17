package com.genie.auth.oauth.application;

import java.time.Duration;
import java.util.Optional;

/** Lưu {@link PendingOAuthSignup} theo mã ngẫu nhiên (cài đặt Redis: {@code infra.RedisPendingOAuthSignupStore}). */
public interface PendingOAuthSignupStore {

    void save(String token, PendingOAuthSignup value, Duration ttl);

    /** Đọc, không xóa (người dùng có thể nhập lại email / xin lại mã). */
    Optional<PendingOAuthSignup> find(String token);

    void delete(String token);
}
