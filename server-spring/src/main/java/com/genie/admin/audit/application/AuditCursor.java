package com.genie.admin.audit.application;

import com.genie.common.error.InvalidRequestException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

/**
 * Con trỏ keyset của nhật ký = Base64URL({@code <created_at ISO>|<id>}) (plan nhật ký kiểm toán L03.02).
 * Client coi là chuỗi mờ, chỉ gửi lại nguyên văn.
 */
public record AuditCursor(OffsetDateTime createdAt, String id) {

    public String encode() {
        String raw = createdAt.toInstant().toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Giải mã; hỏng → {@code 422} trường {@code cursor}. */
    public static AuditCursor decode(String value) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            if (sep <= 0 || sep == raw.length() - 1) {
                throw new IllegalArgumentException("thiếu phần");
            }
            return new AuditCursor(Instant.parse(raw.substring(0, sep)).atOffset(ZoneOffset.UTC), raw.substring(sep + 1));
        } catch (RuntimeException e) {
            throw InvalidRequestException.of("cursor", "INVALID_FORMAT", value, "Con trỏ phân trang không hợp lệ.");
        }
    }
}
