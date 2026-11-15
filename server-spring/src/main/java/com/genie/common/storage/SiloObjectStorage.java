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
 * {@link ObjectStorage} trên Silo — kho tệp chuẩn S3, fork của MinIO (gọi qua SDK S3 {@code io.minio} 8.5, ADR 0009). Dùng HAI client:
 * <ul>
 *   <li>{@code client} — gọi Silo thật qua {@code genie.storage.endpoint} (stat, copy, xóa);</li>
 *   <li>{@code signer} — chỉ để KÝ link theo {@code genie.storage.public-url} (địa chỉ trình duyệt thấy). Ký link là
 *       phép tính cục bộ, không gọi mạng (đã đặt sẵn region), nên host công khai không cần truy cập được từ backend.</li>
 * </ul>
 */
@Component
public class SiloObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(SiloObjectStorage.class);
    private static final Set<String> MISSING = Set.of("NoSuchKey", "NoSuchObject", "NoSuchBucket");

    private final MinioClient client;
    private final MinioClient signer;
    private final String publicBase;
    private final Clock clock;

    public SiloObjectStorage(StorageProperties properties, Clock clock) {
        this.client = build(properties.endpoint(), properties);
        this.signer = build(properties.publicUrl(), properties);
        this.publicBase = trimSlash(properties.publicUrl());
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
                .bucket(bucket)
                .object(objectKey)
                .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                .build()));
        return new PresignedUpload(url, Map.of("Content-Type", contentType), clock.instant().plus(ttl));
    }

    @Override
    public String presignGet(String bucket, String objectKey, Duration ttl) {
        return call(() -> signer.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectKey)
                .expiry((int) ttl.toSeconds(), TimeUnit.SECONDS)
                .build()));
    }

    @Override
    public Optional<StoredObject> stat(String bucket, String objectKey) {
        try {
            StatObjectResponse r = client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
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
                .bucket(bucket)
                .object(objectKey)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType)
                .build()));
    }

    @Override
    public void move(String bucket, String sourceKey, String targetKey) {
        call(() -> client.copyObject(CopyObjectArgs.builder()
                .bucket(bucket)
                .object(targetKey)
                .source(CopySource.builder().bucket(bucket).object(sourceKey).build())
                .build()));
        delete(bucket, sourceKey);
    }

    @Override
    public void delete(String bucket, String objectKey) {
        call(() -> {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return null;
        });
    }

    @Override
    public String publicUrl(String bucket, String objectKey) {
        return publicBase + "/" + bucket + "/" + objectKey;
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
        log.warn("Silo lỗi: {}", e.toString());
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Kho tệp tạm thời không phản hồi, vui lòng thử lại.");
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
