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
    AVATAR("avatar-assets", Set.of("image/jpeg", "image/png"), 1L * 1024 * 1024, 0, true, false, null),

    /** Ảnh banner trang chủ (UC-ADM-12) — {@code content-assets} công khai, JPEG / PNG / WebP ≤ 5 MB, quyền {@code MANAGE_VOUCHER}. */
    BANNER_IMAGE("content-assets", Set.of("image/jpeg", "image/png", "image/webp"), 5L * 1024 * 1024, 0, true, false,
            "MANAGE_VOUCHER"),

    /**
     * Ảnh banner của thông báo toàn sàn (UC-NOT-05; API_SPEC 4.1; plan 2026-11-26 thông báo D14) — {@code content-assets}
     * công khai, JPEG / PNG / WebP ≤ 5 MB, quyền {@code MANAGE_PLATFORM_NOTIFICATION}; khóa chính thức {@code notice/{id}/…}.
     */
    NOTIFICATION_IMAGE("content-assets", Set.of("image/jpeg", "image/png", "image/webp"), 5L * 1024 * 1024, 0, true, false,
            "MANAGE_PLATFORM_NOTIFICATION"),

    /**
     * Giấy tờ KYC (UC-VND-02; plan Mở shop & KYC D4) — bucket RIÊNG TƯ {@code kyc-dossiers}, PDF / JPEG / PNG ≤ 10 MB,
     * ảnh ≥ 20 KB (ảnh quá nhỏ không đọc được QR / MRZ), link tải lên 10 phút, chỉ VENDOR. Tệp {@code tmp/} được backend
     * mã hóa AES-256-GCM khi nộp hồ sơ ({@code KycDossierStorage}) — không bao giờ để bản rõ ở khóa chính thức.
     */
    KYC_DOCUMENT("kyc-dossiers", Set.of("application/pdf", "image/jpeg", "image/png"), 10L * 1024 * 1024, 20L * 1024,
            false, true, "ROLE_VENDOR"),

    /** Logo shop (UC-VND-21) — {@code avatar-assets} công khai, JPEG / PNG ≤ 1 MB, chỉ VENDOR. */
    SHOP_LOGO("avatar-assets", Set.of("image/jpeg", "image/png"), 1L * 1024 * 1024, 0, true, false, "ROLE_VENDOR"),

    /** Ảnh bìa shop (UC-VND-21) — {@code avatar-assets} công khai, JPEG / PNG ≤ 2 MB, chỉ VENDOR. */
    SHOP_COVER("avatar-assets", Set.of("image/jpeg", "image/png"), 2L * 1024 * 1024, 0, true, false, "ROLE_VENDOR");

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
    private final long minImageBytes;
    private final boolean publicRead;
    private final boolean sensitive;
    private final String requiredAuthority;

    UploadPurpose(String bucket, Set<String> contentTypes, long maxBytes, long minImageBytes, boolean publicRead,
                  boolean sensitive, String requiredAuthority) {
        this.bucket = bucket;
        this.contentTypes = contentTypes;
        this.maxBytes = maxBytes;
        this.minImageBytes = minImageBytes;
        this.publicRead = publicRead;
        this.sensitive = sensitive;
        this.requiredAuthority = requiredAuthority;
    }

    /** Dung lượng tối thiểu của tệp ẢNH ({@code image/*}); 0 = không giới hạn. PDF không áp dụng. */
    public long minImageBytes() {
        return minImageBytes;
    }

    /** Tệp loại {@code contentType} có dung lượng {@code size} có nằm trong giới hạn của purpose không. */
    public boolean acceptsSize(String contentType, long size) {
        if (size <= 0 || size > maxBytes) {
            return false;
        }
        return minImageBytes <= 0 || contentType == null || !contentType.startsWith("image/") || size >= minImageBytes;
    }

    /**
     * Mã quyền bắt buộc để xin link tải lên ({@code null} = mọi người đã đăng nhập); dạng {@code ROLE_VENDOR} = theo vai trò.
     */
    public String requiredAuthority() {
        return requiredAuthority;
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
