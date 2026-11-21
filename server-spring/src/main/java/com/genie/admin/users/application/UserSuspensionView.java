package com.genie.admin.users.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

/**
 * Lý do + thời hạn khóa (API 11.13). {@code reasonCode} theo 16.4 (client dịch); {@code until = null} = vô thời hạn —
 * luôn trả khóa {@code until} / {@code note} kể cả khi {@code null} để client phân biệt "vô thời hạn".
 */
public record UserSuspensionView(String reasonCode,
                                 @JsonInclude(JsonInclude.Include.ALWAYS) String note,
                                 @JsonInclude(JsonInclude.Include.ALWAYS) OffsetDateTime until) {
}
