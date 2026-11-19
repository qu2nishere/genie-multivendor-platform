package com.genie.vendor.settings.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.vendor.settings.application.ShopSettingsService;
import com.genie.vendor.settings.application.ShopShippingSettings;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cài đặt Shop — UC-VND-21 (API_SPEC 10.11). Mở cả khi shop chưa VERIFIED (cổng KYC — API 0.9). */
@RestController
@RequestMapping("/api/v1/seller/settings")
public class SellerSettingsController {

    private final ShopSettingsService settings;
    private final ApiResponseFactory responses;

    public SellerSettingsController(ShopSettingsService settings, ApiResponseFactory responses) {
        this.settings = settings;
        this.responses = responses;
    }

    public record ProfileBody(@Size(max = 100) String shopName, @Size(max = 2000) String description,
                              @Size(max = 300) String logoObjectKey, @Size(max = 300) String coverObjectKey) {
    }

    public record ShippingBody(Map<String, Boolean> carriers, boolean freeShippingEnabled,
                               @DecimalMin("0") BigDecimal freeShippingThreshold) {
    }

    public record AutoReplyBody(Boolean enabled, @Size(max = 500) String greeting, @Size(max = 500) String awayMessage) {
    }

    public record PatchBody(@Valid ProfileBody profile, @Valid ShippingBody shipping, @Valid AutoReplyBody autoReply,
                            Boolean preOrderEnabled) {

        ShopSettingsService.Patch toPatch() {
            return new ShopSettingsService.Patch(
                    profile == null ? null : new ShopSettingsService.ProfilePatch(profile.shopName(), profile.description(),
                            profile.logoObjectKey(), profile.coverObjectKey()),
                    shipping == null ? null : new ShopShippingSettings.Settings(shipping.carriers(),
                            shipping.freeShippingEnabled(), shipping.freeShippingThreshold()),
                    autoReply == null ? null : new ShopSettingsService.AutoReply(autoReply.enabled(), autoReply.greeting(),
                            autoReply.awayMessage()),
                    preOrderEnabled);
        }
    }

    @Operation(summary = "UC-VND-21 — Tải toàn bộ trang Cài đặt shop")
    @GetMapping
    public ResponseEntity<ApiResponse<ShopSettingsService.Settings>> get(@CurrentUser AuthUser me) {
        return noStore(settings.get(me.id()));
    }

    @Operation(summary = "UC-VND-21 — Cập nhật thông tin gian hàng & cài đặt vận hành")
    @PatchMapping
    public ResponseEntity<ApiResponse<ShopSettingsService.Settings>> patch(@CurrentUser AuthUser me,
                                                                          @Valid @RequestBody PatchBody body) {
        return noStore(settings.patch(me.id(), body.toPatch()));
    }

    private <T> ResponseEntity<ApiResponse<T>> noStore(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(data).getBody());
    }
}
