package com.genie.auth.login.application;

import com.genie.auth.profile.application.UserSummary;
import com.genie.auth.token.application.OpenedSession;

/**
 * Kết quả xác thực thành công: phiên vừa mở + thông tin người dùng cho {@code $AuthTokens.user}.
 */
public record AuthResult(OpenedSession session, UserSummary user) {
}
