package com.genie.common.storage;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * {@link ObjectStorage} trên kho tệp chuẩn S3, gọi qua SDK {@code io.minio} 8.5: máy dev / test là Silo (fork của MinIO —
 * ADR 0009), production là AWS S3 (ADR 0010) — chỉ khác cấu hình {@link StorageProperties}. Dùng HAI client:
 * <ul>
 *   <li>{@code client} — gọi kho tệp thật qua {@code genie.storage.endpoint} (stat, copy, xóa);</li>
 *   <li>{@code signer} — chỉ để KÝ link theo {@code genie.storage.public-url} (địa chỉ trình duyệt thấy). Ký link là
 *       phép tính cục bộ, không gọi mạng (đã đặt sẵn region), nên host công khai không cần truy cập được từ backend.</li>
 * </ul>
 * Tham số {@code bucket} của mọi phương thức là tên LOGIC ({@code avatar-assets}…); lớp này ghép
 * {@code genie.storage.bucket-prefix} thành tên bucket thật (prod: {@code genie-prod-avatar-assets}).
 */
@Component
public class S3ObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorage.class);
    private static final String AWS_HOST_SUFFIX = "amazonaws.com";
    private static final Set<String> MISSING = Set.of("NoSuchKey", "NoSuchObject", "NoSuchBucket");

    private final MinioClient client;
    private final MinioClient signer;
    private final String publicBase;
    private final boolean virtualHostStyle;
    private final StorageProperties properties;
    private final Clock clock;

    public S3ObjectStorage(StorageProperties properties, Clock clock) {
        this.client = build(properties.endpoint(), properties);
        this.signer = build(properties.publicUrl(), properties);
        this.publicBase = trimSlash(properties.publicUrl());
        this.virtualHostStyle = isAwsHost(properties.publicUrl());
        this.properties = properties;
        this.clock = clock;
    }

    private static MinioClient build(String url, StorageProperties p) {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(p.accessKey(), p.secretKey())
                .region(p.region())
                .build();
    }

    @Override
    public PresignedUpload presignPut(String bucket, String objectKey, String contentType, Duration ttl) {
        String url = call(() -> signer.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(bucket(bucket))
                .object(objectKey)
                .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                .build()));
        return new PresignedUpload(url, Map.of("Content-Type", contentType), clock.instant().plus(ttl));
    }

    @Override
    public String presignGet(String bucket, String objectKey, Duration ttl) {
        return call(() -> signer.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket(bucket))
                .object(objectKey)
                .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                .build()));
    }

    @Override
    public Optional<StoredObject> stat(String bucket, String objectKey) {
        try {
            StatObjectResponse r = client.statObject(StatObjectArgs.builder().bucket(bucket(bucket)).object(objectKey).build());
            return Optional.of(new StoredObject(r.contentType(), r.size()));
        } catch (ErrorResponseException e) {
            if (MISSING.contains(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw unavailable(e);
        } catch (Exception e) {
            throw unavailable(e);
        }
    }

    @Override
    public void put(String bucket, String objectKey, byte[] data, String contentType) {
        call(() -> client.putObject(PutObjectArgs.builder()
                .bucket(bucket(bucket))
                .object(objectKey)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType)
                .build()));
    }

    @Override
    public void move(String bucket, String sourceKey, String targetKey) {
        call(() -> client.copyObject(CopyObjectArgs.builder()
                .bucket(bucket(bucket))
                .object(targetKey)
                .source(CopySource.builder().bucket(bucket(bucket)).object(sourceKey).build())
                .build()));
        delete(bucket, sourceKey);
    }

    @Override
    public void delete(String bucket, String objectKey) {
        call(() -> {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket(bucket)).object(objectKey).build());
            return null;
        });
    }

    /**
     * URL công khai cố định. Silo (dev): {@code <public-url>/<bucket>/<key>}. AWS S3: {@code https://<bucket>.<host>/<key>}
     * (virtual-host — kiểu AWS khuyến nghị, cùng kiểu SDK dùng khi ký link).
     */
    @Override
    public String publicUrl(String bucket, String objectKey) {
        String physical = bucket(bucket);
        if (virtualHostStyle) {
            URI base = URI.create(publicBase);
            return base.getScheme() + "://" + physical + "." + base.getAuthority() + "/" + objectKey;
        }
        return publicBase + "/" + physical + "/" + objectKey;
    }

    private String bucket(String logicalBucket) {
        return properties.physicalBucket(logicalBucket);
    }

    static boolean isAwsHost(String url) {
        String host = URI.create(url).getHost();
        return host != null && (host.equals(AWS_HOST_SUFFIX) || host.endsWith("." + AWS_HOST_SUFFIX));
    }

    @FunctionalInterface
    private interface StorageCall<T> {
        T run() throws Exception;
    }

    private static <T> T call(StorageCall<T> action) {
        try {
            return action.run();
        } catch (Exception e) {
            throw unavailable(e);
        }
    }

    private static BusinessException unavailable(Exception e) {
        log.warn("Kho tệp S3 lỗi: {}", e.toString());
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Kho tệp tạm thời không phản hồi, vui lòng thử lại.");
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
