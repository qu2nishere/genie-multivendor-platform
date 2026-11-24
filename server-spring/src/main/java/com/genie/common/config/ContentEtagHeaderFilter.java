// ETag theo nội dung, bỏ khối meta (API_SPEC 0.12; sửa lỗi ETag /public/** không bao giờ 304 — checklist mục 3, 2026-11-24)
package com.genie.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Như {@link ShallowEtagHeaderFilter} nhưng băm thân phản hồi <b>sau khi bỏ khối {@code meta}</b>.
 * Vỏ {@code ApiResponse} có {@code meta.request_id} + {@code meta.timestamp} đổi theo từng request, nên băm nguyên thân
 * thì ETag không bao giờ trùng và {@code If-None-Match} không bao giờ được {@code 304}.
 * Thân không phải JSON object thì băm nguyên văn như bộ lọc gốc.
 */
public class ContentEtagHeaderFilter extends ShallowEtagHeaderFilter {

    private final ObjectMapper objectMapper;

    public ContentEtagHeaderFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
        byte[] body = inputStream.readAllBytes();
        byte[] hashed = withoutMeta(body);
        StringBuilder builder = new StringBuilder(37);
        if (isWeak) {
            builder.append("W/");
        }
        builder.append("\"0");
        DigestUtils.appendMd5DigestAsHex(hashed, builder);
        builder.append('"');
        return builder.toString();
    }

    private byte[] withoutMeta(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root instanceof ObjectNode obj && obj.has("meta")) {
                obj.remove("meta");
                return objectMapper.writeValueAsBytes(obj);
            }
        } catch (IOException ignored) {
            // không phải JSON → băm nguyên văn
        }
        return body;
    }
}
