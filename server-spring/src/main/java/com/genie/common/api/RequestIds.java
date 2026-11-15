package com.genie.common.api;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@code meta.request_id}: traceId của Micrometer Tracing (trùng traceId trong log JSON) → header {@code X-Request-Id}
 * client tự gửi → UUID mới (API_SPEC 0.2).
 */
@Component
public class RequestIds {

    public static final String HEADER = "X-Request-Id";

    private final ObjectProvider<Tracer> tracer;

    public RequestIds(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    public String current() {
        Tracer t = tracer.getIfAvailable();
        if (t != null) {
            Span span = t.currentSpan();
            if (span != null) {
                return "req_" + span.context().traceId();
            }
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String fromClient = req.getHeader(HEADER);
            if (fromClient != null && !fromClient.isBlank() && fromClient.length() <= 100) {
                return fromClient;
            }
        }
        return "req_" + UUID.randomUUID().toString().replace("-", "");
    }
}
