package com.genie.auth.address.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genie.auth.address.application.AddressInput;
import com.genie.auth.address.application.AddressService;
import com.genie.auth.address.application.AddressView;
import com.genie.auth.address.domain.AddressLabel;
import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sổ địa chỉ của tôi (API_SPEC 3.8, UC-AUTH-09) — Tài khoản › Địa chỉ, modal "Địa chỉ của tôi" ở Thanh toán. */
@RestController
@RequestMapping("/api/v1/me/addresses")
public class AddressController {

    private final AddressService service;
    private final ApiResponseFactory responses;

    public AddressController(AddressService service, ApiResponseFactory responses) {
        this.service = service;
        this.responses = responses;
    }

    /** {@code $AddressInput}: SĐT {@code ^0\d{9,10}$}, đủ tỉnh + phường (địa giới 2 cấp), {@code address_line} không rỗng. */
    public record AddressRequest(
            @NotBlank @Size(max = 255) String recipientName,
            @NotBlank @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại phải gồm 10–11 chữ số, bắt đầu bằng 0.")
            String phoneNumber,
            @NotNull @Positive Integer provinceId,
            @NotBlank @Size(max = 50) String wardCode,
            @NotBlank @Size(max = 500) String addressLine,
            AddressLabel label,
            @JsonProperty("is_default") Boolean isDefault,
            @Size(max = 200, message = "Ghi chú tối đa 200 ký tự.") String deliveryNote) {

        AddressInput toInput() {
            return new AddressInput(recipientName, phoneNumber, provinceId, wardCode, addressLine,
                    label == null ? AddressLabel.HOME : label, Boolean.TRUE.equals(isDefault), deliveryNote);
        }
    }

    public record DefaultRequest(@JsonProperty("is_default") @NotNull Boolean isDefault) {
    }

    @Operation(summary = "UC-AUTH-09 — Danh sách địa chỉ (mặc định đứng đầu)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressView>>> list(@CurrentUser AuthUser me) {
        return responses.ok(service.list(me.id()));
    }

    @Operation(summary = "UC-AUTH-09 — Thêm địa chỉ (địa chỉ đầu tiên tự thành mặc định)")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressView>> create(@CurrentUser AuthUser me,
                                                          @Valid @RequestBody AddressRequest body) {
        return responses.created(service.create(me.id(), body.toInput()));
    }

    @Operation(summary = "UC-AUTH-09 — Sửa toàn bộ địa chỉ")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressView>> update(@CurrentUser AuthUser me, @PathVariable long id,
                                                          @Valid @RequestBody AddressRequest body) {
        return responses.ok(service.update(me.id(), id, body.toInput()));
    }

    @Operation(summary = "UC-AUTH-09 — Đặt địa chỉ mặc định")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressView>> setDefault(@CurrentUser AuthUser me, @PathVariable long id,
                                                              @Valid @RequestBody DefaultRequest body) {
        if (!body.isDefault()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Chỉ hỗ trợ đặt làm mặc định — chọn địa chỉ khác làm mặc định để bỏ cờ địa chỉ này.");
        }
        return responses.ok(service.setDefault(me.id(), id));
    }

    @Operation(summary = "UC-AUTH-09 — Xóa địa chỉ (mềm; địa chỉ mặc định không xóa được)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthUser me, @PathVariable long id) {
        service.delete(me.id(), id);
        return ResponseEntity.noContent().build();
    }
}
