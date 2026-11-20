package com.genie.catalog.product.application;

import com.genie.common.storage.ObjectStorage;
import org.springframework.stereotype.Component;

/**
 * Đổi giá trị cột ảnh thành URL tuyệt đối (API_SPEC 0.5; LUU_TRU 5.6): cột đã là URL ({@code http…}) giữ nguyên,
 * còn lại coi là khóa đối tượng trong bucket công khai {@value #PRODUCT_MEDIA_BUCKET}.
 */
@Component
public class MediaUrls {

    public static final String PRODUCT_MEDIA_BUCKET = "product-media";

    private final ObjectStorage storage;

    public MediaUrls(ObjectStorage storage) {
        this.storage = storage;
    }

    public String product(String stored) {
        return resolve(PRODUCT_MEDIA_BUCKET, stored);
    }

    public String resolve(String bucket, String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (stored.startsWith("http://") || stored.startsWith("https://")) {
            return stored;
        }
        return storage.publicUrl(bucket, stored.startsWith("/") ? stored.substring(1) : stored);
    }
}
