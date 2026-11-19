package com.genie.common.ai;

import com.genie.common.security.InternalTokenFilter;
import com.genie.common.security.SecurityProperties;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gọi AI Worker (FastAPI) — TS 4.6, 10.2; plan Mở shop & KYC K05.04. Mọi yêu cầu kèm header {@code X-Internal-Token} (cùng
 * token AI Worker dùng khi gọi lại backend). Timeout kết nối 3 giây / đọc 10 giây — AI Worker chỉ trả {@code 202} rồi xử lý
 * nền. Lỗi mạng / 4xx / 5xx → {@link AiWorkerException} để nghiệp vụ tự chuyển hướng (vd. KYC → duyệt tay).
 */
@Component
public class AiWorkerHttpClient {

    private final RestClient http;

    public AiWorkerHttpClient(RestClient.Builder builder, AiWorkerProperties props, SecurityProperties security) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(10_000);
        RestClient.Builder b = builder.clone().requestFactory(factory).baseUrl(props.url());
        if (security.internalToken() != null) {
            b = b.defaultHeader(InternalTokenFilter.HEADER, security.internalToken());
        }
        this.http = b.build();
    }

    /** {@code POST {AI_WORKER_URL}{path}} với thân JSON; trả mã trạng thái 2xx. */
    public int post(String path, Map<String, ?> body) {
        try {
            HttpStatusCode status = http.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().toBodilessEntity().getStatusCode();
            return status.value();
        } catch (RestClientException e) {
            throw new AiWorkerException("AI Worker không phản hồi: " + e.getMessage(), e);
        }
    }

    public static class AiWorkerException extends RuntimeException {
        public AiWorkerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
