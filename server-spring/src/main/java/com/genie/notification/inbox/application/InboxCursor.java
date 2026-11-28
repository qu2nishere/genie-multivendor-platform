// Con trỏ phân trang của hộp thư gộp 3 nguồn — API_SPEC 3.12 (plan 2026-11-26 thông báo D8, N04.04)
package com.genie.notification.inbox.application;

import com.genie.common.error.InvalidRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Khóa sắp xếp toàn phần, giảm dần: ({@code at} — micro giây kể từ epoch, {@code source} — 0 = {@code ntf_}, 1 = {@code pn_},
 * 2 = {@code an_}, {@code id}). Mã hóa Base64URL của {@code "<at>:<source>:<id>"}; hỏng → 422 trường {@code cursor}.
 */
public record InboxCursor(long at, int source, long id) {

    public static final int PERSONAL = 0;
    public static final int PLATFORM = 1;
    public static final int ADMIN = 2;

    public String encode() {
        String raw = at + ":" + source + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static InboxCursor decode(String value) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value.trim()), StandardCharsets.UTF_8);
            String[] p = raw.split(":");
            if (p.length != 3) {
                throw new IllegalArgumentException(raw);
            }
            int source = Integer.parseInt(p[1]);
            if (source < PERSONAL || source > ADMIN) {
                throw new IllegalArgumentException(raw);
            }
            return new InboxCursor(Long.parseLong(p[0]), source, Long.parseLong(p[2]));
        } catch (RuntimeException e) {
            throw InvalidRequestException.of("cursor", "INVALID_FORMAT", value, "Con trỏ phân trang không hợp lệ.");
        }
    }

    /** {@code (at, source, id)} của một dòng nhỏ hơn con trỏ — dòng đó thuộc trang sau. */
    public boolean isAfter(long rowAt, int rowSource, long rowId) {
        if (rowAt != at) {
            return rowAt < at;
        }
        if (rowSource != source) {
            return rowSource < source;
        }
        return rowId < id;
    }
}
