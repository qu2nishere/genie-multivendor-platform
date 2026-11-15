package com.genie.common.storage;

import java.util.Map;
import java.util.Set;

/**
 * Mục đích tải tệp (API_SPEC 4.1, khớp {@code web/src/shared/lib/upload.rules.ts}): bucket, loại tệp, dung lượng tối đa.
 * Plan AUTH A11.01: làm trước {@link #AVATAR}; module khác (catalog, social, vendor KYC…) thêm giá trị của mình
 * — mỗi giá trị cần kiểm quyền riêng ở nghiệp vụ dùng tệp.
 */
public enum UploadPurpose {

    /** Ảnh đại diện người dùng — bucket công khai {@code avatar-assets}, JPEG / PNG ≤ 1 MB. */
    AVATAR("avatar-assets", Set.of("image/jpeg", "image/png"), 1L * 1024 * 1024, true, false);

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "video/mp4", "mp4",
            "application/pdf", "pdf");

    private final String bucket;
    private final Set<String> contentTypes;
    private final long maxBytes;
    private final boolean publicRead;
    private final boolean sensitive;

    UploadPurpose(String bucket, Set<String> contentTypes, long maxBytes, boolean publicRead, boolean sensitive) {
        this.bucket = bucket;
        this.contentTypes = contentTypes;
        this.maxBytes = maxBytes;
        this.publicRead = publicRead;
        this.sensitive = sensitive;
    }

    public String bucket() {
        return bucket;
    }

    public Set<String> contentTypes() {
        return contentTypes;
    }

    public long maxBytes() {
        return maxBytes;
    }

    /** Bucket cho xem không cần chữ ký (ảnh đại diện, ảnh sản phẩm…). */
    public boolean publicRead() {
        return publicRead;
    }

    /** Hồ sơ nhạy cảm (KYC) — link tải lên ngắn hạn hơn. */
    public boolean sensitive() {
        return sensitive;
    }

    public boolean accepts(String contentType) {
        return contentType != null && contentTypes.contains(contentType);
    }

    /** Đuôi tệp theo loại nội dung (không tin tên tệp người dùng gửi). */
    public static String extensionOf(String contentType) {
        return EXTENSIONS.getOrDefault(contentType, "bin");
    }
}
