package com.genie.admin.banner.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.admin.banner.application.BannerEventService;
import com.genie.admin.banner.application.BannerEventService.Event;
import com.genie.admin.banner.application.BannerEventService.EventType;
import com.genie.common.error.InvalidRequestException;
import com.genie.common.security.ClientInfo;
import io.swagger.v3.oas.annotations.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ghi lượt hiển thị / bấm banner gộp lô — UC-ADM-12 (API_SPEC 5.15). Công khai, luôn {@code 204}; nhận cả
 * {@code text/plain} vì {@code navigator.sendBeacon} gửi chuỗi JSON với kiểu đó.
 */
@RestController
public class PublicBannerEventController {

    private final BannerEventService events;
    private final ObjectMapper objectMapper;

    public PublicBannerEventController(BannerEventService events, ObjectMapper objectMapper) {
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "UC-ADM-12 — Ghi lượt hiển thị / bấm banner (gộp lô)",
            description = "{events:[{banner_id, type: IMPRESSION|CLICK}]} 1–50 phần tử; 60 lần / phút / IP (429). "
                    + "Banner không còn hiển thị bị bỏ qua. Luôn 204.")
    @PostMapping(value = "/api/v1/public/banner-events",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<Void> record(@RequestBody String body, ClientInfo client) {
        events.record(parse(body), client.ip());
        return ResponseEntity.noContent().build();
    }

    private List<Event> parse(String body) {
        try {
            JsonNode arr = objectMapper.readTree(body).get("events");
            if (arr == null || !arr.isArray()) {
                throw new IllegalArgumentException("thiếu events");
            }
            if (arr.size() > BannerEventService.MAX_EVENTS) {
                throw InvalidRequestException.of("events", "OUT_OF_RANGE", arr.size(), "Mỗi lần gửi từ 1 đến 50 sự kiện.");
            }
            List<Event> out = new ArrayList<>();
            for (JsonNode n : arr) {
                JsonNode id = n.get("banner_id");
                JsonNode type = n.get("type");
                EventType t = null;
                if (type != null && type.isTextual()) {
                    try {
                        t = EventType.valueOf(type.asText().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        // loại lạ → bỏ qua phần tử
                    }
                }
                out.add(new Event(id != null && id.canConvertToLong() ? id.asLong() : null, t));
            }
            return out;
        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw InvalidRequestException.of("events", "INVALID_FORMAT", null, "Thân yêu cầu không hợp lệ.");
        }
    }
}
