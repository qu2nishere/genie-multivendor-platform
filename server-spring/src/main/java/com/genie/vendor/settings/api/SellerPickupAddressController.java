package com.genie.vendor.settings.api;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import com.genie.vendor.settings.application.PickupAddressService;
import com.genie.vendor.settings.application.ShopSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kho lấy hàng GHN — UC-VND-05 (API_SPEC 10.11 "Lưu và Đồng bộ với GHN"; plan Mở shop & KYC K07.04). */
@RestController
@RequestMapping("/api/v1/seller/pickup-address")
public class SellerPickupAddressController {

    private final PickupAddressService pickups;
    private final ApiResponseFactory responses;

    public SellerPickupAddressController(PickupAddressService pickups, ApiResponseFactory responses) {
        this.pickups = pickups;
        this.responses = responses;
    }

    public record PickupRequest(@NotBlank @Pattern(regexp = "^0\\d{9,10}$") String phoneNumber,
                                @NotNull @Positive Integer provinceId,
                                @NotBlank @Size(max = 50) String wardCode,
                                @NotBlank @Size(max = 255) String addressLine) {
    }

    @Operation(summary = "UC-VND-05 — Cấu hình kho lấy hàng & Đồng bộ GHN Shop ID")
    @PutMapping
    public ResponseEntity<ApiResponse<ShopSettingsService.PickupAddress>> save(@CurrentUser AuthUser me,
                                                                              @Valid @RequestBody PickupRequest body) {
        ShopSettingsService.PickupAddress saved = pickups.save(me.id(), new PickupAddressService.Input(
                body.phoneNumber(), body.provinceId(), body.wardCode().trim(), body.addressLine()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(responses.ok(saved).getBody());
    }
}
