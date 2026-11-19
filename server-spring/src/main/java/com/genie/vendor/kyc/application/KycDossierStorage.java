package com.genie.vendor.kyc.application;

import com.genie.common.crypto.AesGcmCipher;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.storage.ObjectStorage;
import com.genie.common.storage.StorageProperties;
import com.genie.common.storage.UploadPurpose;
import com.genie.common.storage.UploadService;
import com.genie.vendor.kyc.domain.KycDocumentType;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Kho hồ sơ KYC đã mã hóa (plan Mở shop & KYC D5, K04.03–K04.04; ADR 0011 mục 4; LUU_TRU 5.6–5.7):
 * <ul>
 *   <li>{@link #seal}: đọc tệp {@code tmp/} của chính người bán, mã hóa AES-256-GCM (AAD = {@code vendorProfileId:type}),
 *       ghi {@code kyc-dossiers/vendor/{vendorProfileId}/{slug}-{uuid}.enc}, xóa bản tạm. Bản rõ không bao giờ nằm ở khóa
 *       chính thức;</li>
 *   <li>{@link #openTemporaryView}: giải mã ra {@code kyc-dossiers/tmp/view/{uuid}.{ext}} + Presigned GET 5 phút — chỉ cho
 *       nhân viên duyệt / AI Worker. Khóa bản xem ghi vào Redis {@code kyc:views} để {@link KycViewCleanupJob} xóa sau
 *       10 phút (lifecycle {@code tmp/} 1 ngày của bucket là lớp dự phòng).</li>
 * </ul>
 */
@Component
public class KycDossierStorage {

    private static final Logger log = LoggerFactory.getLogger(KycDossierStorage.class);
    static final String BUCKET = UploadPurpose.KYC_DOCUMENT.bucket();
    static final String VIEW_PREFIX = "tmp/view/";
    static final String VIEWS_KEY = "kyc:views";
    static final Duration VIEW_RETENTION = Duration.ofMinutes(10);

    private final UploadService uploads;
    private final ObjectStorage storage;
    private final AesGcmCipher cipher;
    private final StringRedisTemplate redis;
    private final StorageProperties storageProperties;
    private final Clock clock;

    public KycDossierStorage(UploadService uploads, ObjectStorage storage, AesGcmCipher cipher, StringRedisTemplate redis,
                             StorageProperties storageProperties, Clock clock) {
        this.uploads = uploads;
        this.storage = storage;
        this.cipher = cipher;
        this.redis = redis;
        this.storageProperties = storageProperties;
        this.clock = clock;
    }

    /** Giấy tờ đã niêm phong: khóa {@code .enc} + thông tin hiển thị (tên tệp, dung lượng gốc, loại nội dung). */
    public record SealedDocument(KycDocumentType type, String objectKey, String fileName, long sizeBytes,
                                 String contentType) {
    }

    public SealedDocument seal(long userId, long vendorProfileId, KycDocumentType type, String tmpKey, String fileName) {
        UploadService.TmpFile tmp = uploads.readOwnTmp(userId, UploadPurpose.KYC_DOCUMENT, tmpKey);
        if (type == KycDocumentType.SELFIE && !tmp.contentType().startsWith("image/")) {
            uploads.deleteQuietly(UploadPurpose.KYC_DOCUMENT, tmpKey);
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Ảnh chân dung phải là ảnh JPEG / PNG.",
                    java.util.Map.of("field", "document_object_keys.SELFIE"));
        }
        String key = "vendor/" + vendorProfileId + "/" + type.slug() + "-" + UUID.randomUUID() + ".enc";
        storage.put(BUCKET, key, cipher.encrypt(tmp.data(), type.aad(vendorProfileId)), "application/octet-stream");
        uploads.deleteQuietly(UploadPurpose.KYC_DOCUMENT, tmpKey);
        String name = fileName == null || fileName.isBlank()
                ? type.slug() + "." + UploadPurpose.extensionOf(tmp.contentType())
                : fileName.strip().replaceAll("[\\\\/\\p{Cntrl}]", "_");
        return new SealedDocument(type, key, name.length() > 120 ? name.substring(0, 120) : name, tmp.data().length,
                tmp.contentType());
    }

    /** Giải mã bản gốc (dùng cho bản xem tạm; test kiểm toàn vẹn). */
    public byte[] decrypt(long vendorProfileId, KycDocumentType type, String encKey) {
        byte[] blob = storage.get(BUCKET, encKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy tệp giấy tờ."));
        return cipher.decrypt(blob, type.aad(vendorProfileId));
    }

    /** Bản giải mã tạm + Presigned GET 5 phút ({@code genie.storage.presigned-get-ttl}). */
    public String openTemporaryView(long vendorProfileId, KycDocumentType type, String encKey, String contentType) {
        byte[] plain = decrypt(vendorProfileId, type, encKey);
        String type0 = contentType == null ? "application/octet-stream" : contentType;
        String viewKey = VIEW_PREFIX + UUID.randomUUID() + "." + UploadPurpose.extensionOf(type0);
        storage.put(BUCKET, viewKey, plain, type0);
        redis.opsForZSet().add(VIEWS_KEY, viewKey, clock.millis());
        return storage.presignGet(BUCKET, viewKey, storageProperties.presignedGetTtl());
    }

    /** Xóa bản xem tạm đã quá 10 phút; trả số bản đã xóa. */
    public int purgeExpiredViews() {
        double before = clock.millis() - VIEW_RETENTION.toMillis();
        Set<String> expired = redis.opsForZSet().rangeByScore(VIEWS_KEY, 0, before);
        if (expired == null || expired.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (String key : expired) {
            deleteQuietly(key);
            redis.opsForZSet().remove(VIEWS_KEY, key);
            n++;
        }
        return n;
    }

    public void deleteQuietly(String objectKey) {
        if (objectKey == null) {
            return;
        }
        try {
            storage.delete(BUCKET, objectKey);
        } catch (RuntimeException e) {
            log.warn("Không xóa được tệp KYC {}: {}", objectKey, e.getMessage());
        }
    }
}
