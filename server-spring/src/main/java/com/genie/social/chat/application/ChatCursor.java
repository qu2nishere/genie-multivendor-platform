// Con trỏ phân trang chat (plan 2026-11-26 chat M03.04; API_SPEC 8.1, 8.3)
package com.genie.social.chat.application;

import com.genie.common.error.InvalidRequestException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

/** Base64URL của {@code <thời điểm ISO>|<id>} — mờ với client; hỏng → {@code 422} trường {@code field}. */
public record ChatCursor(OffsetDateTime at, long id) {

    public String encode() {
        String raw = at.toInstant() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ChatCursor decode(String value, String field) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value.trim()), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            long id = Long.parseLong(raw.substring(sep + 1));
            if (sep <= 0 || id <= 0) {
                throw new IllegalArgumentException();
            }
            return new ChatCursor(Instant.parse(raw.substring(0, sep)).atOffset(ZoneOffset.UTC), id);
        } catch (RuntimeException e) {
            throw InvalidRequestException.of(field, "INVALID_FORMAT", value, "Con trỏ phân trang không hợp lệ.");
        }
    }

    /** {@code limit} 1–{@code max}, mặc định {@code def}. */
    public static int limit(Integer limit, int def, int max) {
        int size = limit == null ? def : limit;
        if (size < 1 || size > max) {
            throw InvalidRequestException.of("limit", "OUT_OF_RANGE", limit, "limit phải từ 1 đến " + max + ".");
        }
        return size;
    }
}
