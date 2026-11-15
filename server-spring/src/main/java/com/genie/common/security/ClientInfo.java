package com.genie.common.security;

/**
 * IP thật + User-Agent của request (đã cắt độ dài) — dùng cho rate limit, nhật ký, phiên đăng nhập.
 * Controller nhận trực tiếp làm tham số: {@code login(@RequestBody … body, ClientInfo client)}.
 */
public record ClientInfo(String ip, String userAgent) {

    public static final int MAX_USER_AGENT = 500;

    public static ClientInfo system() {
        return new ClientInfo(null, null);
    }
}
