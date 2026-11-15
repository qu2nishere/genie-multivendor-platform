package com.genie.auth.token.application;

import java.time.OffsetDateTime;

/**
 * Một dòng ở "Thiết bị đăng nhập" (API_SPEC 3.6). {@code location} luôn null ở TLCN (chưa có GeoIP — plan AUTH D6).
 */
public record SessionView(long id, String deviceName, UserAgentParser.DeviceType deviceType, String location,
                          String ipAddress, OffsetDateTime lastActiveAt, boolean isCurrent) {
}
