package com.genie.auth.oauth.application;

import java.time.Duration;
import java.util.Optional;

/** Lưu {@link OAuthState} theo giá trị {@code state} (cài đặt Redis: {@code infra.RedisOAuthStateStore}). */
public interface OAuthStateStore {

    void save(String state, OAuthState value, Duration ttl);

    /** Lấy và XÓA ngay (dùng một lần — gửi lại cùng {@code state} sẽ không còn). */
    Optional<OAuthState> take(String state);
}
