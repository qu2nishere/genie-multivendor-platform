package com.genie.common.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình {@code genie.storage.*} (application.yml — TECH_STACK 16.5, LUU_TRU_DU_LIEU 5, plan AUTH A11.01, ADR 0010).
 * Máy dev / test: Silo trong Docker. Production: AWS S3 (cùng giao thức S3 — chỉ khác giá trị cấu hình).
 *
 * @param endpoint           địa chỉ kho tệp backend gọi tới ({@code MINIO_ENDPOINT}) — dev: {@code http://localhost:9000};
 *                           prod: {@code https://s3.<vùng>.amazonaws.com}
 * @param publicUrl          địa chỉ kho tệp trình duyệt thấy ({@code PUBLIC_S3_URL}) — dùng để KÝ link và dựng URL ảnh
 *                           công khai; chữ ký S3 gắn với host, ký sai host → trình duyệt nhận 403 SignatureDoesNotMatch.
 *                           Host kết thúc {@code amazonaws.com} → URL dạng virtual-host {@code https://<bucket>.s3.<vùng>.amazonaws.com/<key>}
 * @param accessKey          khóa truy cập ứng dụng — dev: tài khoản {@code genie-app} của Silo; prod: IAM user chỉ có quyền
 *                           trên các bucket của Genie (không dùng root — deploy/aws/iam-policy.json)
 * @param secretKey          khóa bí mật tương ứng
 * @param presignedPutTtl    hạn link tải lên (15 phút)
 * @param presignedPutKycTtl hạn link tải lên hồ sơ nhạy cảm — KYC (10 phút)
 * @param presignedGetTtl    hạn link xem tệp bucket riêng tư (5 phút)
 * @param region             vùng ký S3 ({@code STORAGE_REGION}); đặt sẵn để ký link không phải gọi mạng hỏi vùng
 * @param bucketPrefix       tiền tố tên bucket thật ({@code STORAGE_BUCKET_PREFIX}) — tên bucket AWS S3 là duy nhất toàn
 *                           cầu nên prod đặt vd. {@code genie-prod-} → {@code genie-prod-avatar-assets}; dev để trống.
 *                           Mã nghiệp vụ luôn dùng tên logic ({@link UploadPurpose#bucket()}), chỉ lớp cài đặt ghép tiền tố
 */
@ConfigurationProperties(prefix = "genie.storage")
public record StorageProperties(
        @DefaultValue("http://localhost:9000") String endpoint,
        @DefaultValue("http://localhost:9000") String publicUrl,
        String accessKey,
        String secretKey,
        @DefaultValue("15m") Duration presignedPutTtl,
        @DefaultValue("10m") Duration presignedPutKycTtl,
        @DefaultValue("5m") Duration presignedGetTtl,
        @DefaultValue("us-east-1") String region,
        @DefaultValue("") String bucketPrefix) {

    /** Tên bucket thật trên kho tệp = tiền tố + tên logic. */
    public String physicalBucket(String logicalBucket) {
        return bucketPrefix == null ? logicalBucket : bucketPrefix + logicalBucket;
    }
}
