package com.genie.common.storage;

import com.genie.common.api.ApiResponse;
import com.genie.common.api.ApiResponseFactory;
import com.genie.common.security.AuthUser;
import com.genie.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/v1/uploads/presigned-urls} (API_SPEC 4.1) — xin link tải tệp lên MinIO. Cần đăng nhập.
 * Phản hồi khớp {@code web/src/shared/api/upload.ts}: {@code {upload_url, method, headers, object_key, expires_at}}.
 */
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final UploadService uploads;
    private final ApiResponseFactory responses;

    public UploadController(UploadService uploads, ApiResponseFactory responses) {
        this.uploads = uploads;
        this.responses = responses;
    }

    public record PresignRequest(
            @NotNull UploadPurpose purpose,
            @Size(max = 255) String fileName,
            @NotBlank @Size(max = 100) String contentType,
            @NotNull @Positive Long contentLength) {
    }

    @Operation(summary = "API 4.1 — Xin link có chữ ký để tải tệp thẳng lên MinIO")
    @PostMapping("/presigned-urls")
    public ResponseEntity<ApiResponse<UploadService.IssuedUpload>> presign(@CurrentUser AuthUser me,
                                                                          @Valid @RequestBody PresignRequest body) {
        return responses.ok(uploads.issue(me.id(), body.purpose(), body.contentType(), body.contentLength()));
    }
}
