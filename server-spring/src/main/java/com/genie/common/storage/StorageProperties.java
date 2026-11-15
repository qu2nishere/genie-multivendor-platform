package com.genie.common.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình {@code genie.storage.*} (application.yml — TECH_STACK 16.5, LUU_TRU_DU_LIEU 5, plan AUTH A11.01).
 *
 * @param endpoint           địa chỉ MinIO backend gọi tới (trong docker: {@code http://minio:9000})
 * @param publicUrl          địa chỉ MinIO trình duyệt thấy ({@code PUBLIC_S3_URL}) — dùng để KÝ link và dựng URL ảnh
 *                           công khai; chữ ký S3 gắn với host, ký sai host → trình duyệt nhận 403 SignatureDoesNotMatch
 * @param accessKey          tài khoản ứng dụng {@code genie-app} (không dùng root — deploy/minio/init.sh)
 * @param secretKey          mật khẩu tài khoản ứng dụng
 * @param presignedPutTtl    hạn link tải lên (15 phút)
 * @param presignedPutKycTtl hạn link tải lên hồ sơ nhạy cảm — KYC (10 phút)
 * @param presignedGetTtl    hạn link xem tệp bucket riêng tư (5 phút)
 * @param region             vùng ký S3; đặt sẵn để ký link không phải gọi mạng hỏi vùng
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
        @DefaultValue("us-east-1") String region) {
}
