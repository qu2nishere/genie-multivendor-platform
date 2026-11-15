package com.genie.common.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Cổng lưu trữ tệp kiểu S3 (cài đặt: {@link MinioObjectStorage}). Tệp KHÔNG đi qua backend: trình duyệt tải thẳng lên
 * bằng link có chữ ký, backend chỉ ký link, kiểm tra và di chuyển tệp (TECH_STACK 5.4).
 * Lỗi kết nối kho tệp → {@code 503 GNE-SYS-5030}.
 */
public interface ObjectStorage {

    /** Link PUT có chữ ký cho {@code bucket/objectKey}, hết hạn sau {@code ttl}. */
    PresignedUpload presignPut(String bucket, String objectKey, String contentType, Duration ttl);

    /** Link GET có chữ ký (bucket riêng tư). */
    String presignGet(String bucket, String objectKey, Duration ttl);

    /** Thông tin tệp đã tải lên; rỗng nếu chưa có / đã bị dọn. */
    Optional<StoredObject> stat(String bucket, String objectKey);

    /** Ghi tệp do chính backend tạo / tải về (vd. ảnh đại diện Google). */
    void put(String bucket, String objectKey, byte[] data, String contentType);

    /** Chuyển tệp trong cùng bucket (copy rồi xóa bản gốc). */
    void move(String bucket, String sourceKey, String targetKey);

    void delete(String bucket, String objectKey);

    /** URL công khai cố định (chỉ có nghĩa với bucket cho đọc công khai). */
    String publicUrl(String bucket, String objectKey);

    record PresignedUpload(String url, Map<String, String> headers, Instant expiresAt) {
    }

    record StoredObject(String contentType, long size) {
    }
}
