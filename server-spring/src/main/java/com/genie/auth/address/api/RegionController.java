package com.genie.auth.address.api;

import com.genie.auth.address.application.GeocodeService;
import com.genie.auth.address.application.RegionDirectory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import com.genie.common.security.RateLimiter;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đơn vị hành chính 2 cấp — Tỉnh / thành → Phường / xã (API_SPEC 3.9, UC-AUTH-09; plan AUTH A12.02, D25) — công khai,
 * không cần đăng nhập.
 * {@code Cache-Control: public, max-age=86400} + {@code ETag}: trình duyệt giữ 24 giờ, hỏi lại thì nhận {@code 304}.
 * Ô "Tìm nhanh" của bộ chọn lọc phía client.
 */
@RestController
@RequestMapping("/api/v1/public/regions")
public class RegionController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    /** Số lần định vị mỗi phút / IP — bảo vệ hạn mức 1 yêu cầu / giây của Nominatim. */
    static final int MAX_GEOCODE_PER_MINUTE = 10;

    private final RegionDirectory regions;
    private final GeocodeService geocode;
    private final RateLimiter rateLimiter;
    private final ApiResponseFactory responses;

    public RegionController(RegionDirectory regions, GeocodeService geocode, RateLimiter rateLimiter,
                            ApiResponseFactory responses) {
        this.regions = regions;
        this.geocode = geocode;
        this.rateLimiter = rateLimiter;
        this.responses = responses;
    }

    @Operation(summary = "UC-AUTH-09 — Dùng vị trí hiện tại: tọa độ → Tỉnh / Phường + số nhà, đường")
    @GetMapping("/reverse-geocode")
    public ResponseEntity<ApiResponse<GeocodeService.GeocodeView>> reverseGeocode(@RequestParam double lat,
                                                                                 @RequestParam double lng,
                                                                                 ClientInfo client) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Tọa độ không hợp lệ.");
        }
        rateLimiter.check("rl:geocode:" + (client == null ? "?" : client.ip()), MAX_GEOCODE_PER_MINUTE,
                Duration.ofMinutes(1));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(responses.ok(geocode.reverse(lat, lng)).getBody());
    }

    @Operation(summary = "UC-AUTH-09 — Danh sách 34 tỉnh / thành (địa giới từ 01/07/2025)")
    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<RegionDirectory.Region>>> provinces(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        return cached(regions.provinces(), ifNoneMatch);
    }

    @Operation(summary = "UC-AUTH-09 — Phường / xã / đặc khu của tỉnh (địa giới 2 cấp, mã chuỗi)")
    @GetMapping("/provinces/{provinceId}/wards")
    public ResponseEntity<ApiResponse<List<RegionDirectory.Ward>>> wards(
            @PathVariable int provinceId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        return cached(regions.wards(provinceId), ifNoneMatch);
    }

    /** ETag theo nội dung danh sách (không theo {@code meta.request_id} đổi mỗi lần). */
    private <T> ResponseEntity<ApiResponse<List<T>>> cached(List<T> data, String ifNoneMatch) {
        String etag = "\"" + hash(data.toString()) + "\"";
        if (ifNoneMatch != null && ifNoneMatch.contains(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(CACHE).build();
        }
        return ResponseEntity.ok().eTag(etag).cacheControl(CACHE).body(responses.ok(data).getBody());
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
