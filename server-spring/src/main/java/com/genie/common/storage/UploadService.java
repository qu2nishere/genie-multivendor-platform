package com.genie.common.storage;

import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Tải tệp qua link có chữ ký (API_SPEC 4.1, plan AUTH A11.01):
 * <ol>
 *   <li>{@link #issue}: kiểm loại / dung lượng khai báo → link PUT tới {@code tmp/user/{userId}/{uuid}.{ext}};</li>
 *   <li>trình duyệt PUT thẳng lên Silo;</li>
 *   <li>nghiệp vụ dùng tệp (vd. đổi ảnh đại diện) gọi {@link #promote}: kiểm tệp có thật, thuộc đúng người, đúng loại,
 *       đúng dung lượng → chuyển sang {@code user/{userId}/{uuid}.{ext}}.</li>
 * </ol>
 * Tệp bỏ dở trong {@code tmp/} được Silo tự xóa sau 1 ngày (lifecycle — deploy/silo/init.sh).
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    static final String TMP_PREFIX = "tmp/";
    private static final Pattern TMP_KEY = Pattern.compile("^tmp/user/(\\d+)/[0-9a-f-]{36}\\.[a-z0-9]{2,5}$");

    private final ObjectStorage storage;
    private final StorageProperties properties;

    public UploadService(ObjectStorage storage, StorageProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    /** Link tải lên đã ký. */
    public record IssuedUpload(String uploadUrl, String method, Map<String, String> headers, String objectKey,
                               Instant expiresAt) {
    }

    /** Tệp đã chuyển khỏi {@code tmp/}: khóa chính thức + URL công khai (null với bucket riêng tư). */
    public record StoredFile(String objectKey, String publicUrl) {
    }

    public IssuedUpload issue(long userId, UploadPurpose purpose, String contentType, long contentLength) {
        String type = normalize(contentType);
        if (!purpose.accepts(type)) {
            throw invalid("content_type", "Định dạng tệp không được hỗ trợ.",
                    Map.of("allowed_types", purpose.contentTypes()));
        }
        if (contentLength <= 0 || contentLength > purpose.maxBytes()) {
            throw invalid("content_length", "Tệp vượt quá dung lượng cho phép.",
                    Map.of("max_bytes", purpose.maxBytes()));
        }
        if (!purpose.acceptsSize(type, contentLength)) {
            throw invalid("content_length", "Ảnh quá nhỏ, vui lòng chụp lại rõ nét hơn.",
                    Map.of("min_bytes", purpose.minImageBytes()));
        }
        String key = TMP_PREFIX + "user/" + userId + "/" + UUID.randomUUID() + "." + UploadPurpose.extensionOf(type);
        Duration ttl = purpose.sensitive() ? properties.presignedPutKycTtl() : properties.presignedPutTtl();
        ObjectStorage.PresignedUpload signed = storage.presignPut(purpose.bucket(), key, type, ttl);
        return new IssuedUpload(signed.url(), "PUT", signed.headers(), key, signed.expiresAt());
    }

    /**
     * Nhận tệp người dùng vừa tải lên: chỉ chấp nhận khóa {@code tmp/user/{userId}/…} của CHÍNH người đó (không cho dùng
     * tệp của người khác), tệp phải tồn tại, đúng loại và dung lượng (link PUT không giới hạn được nội dung).
     */
    public StoredFile promote(long userId, UploadPurpose purpose, String tmpKey) {
        var m = tmpKey == null ? null : TMP_KEY.matcher(tmpKey);
        if (m == null || !m.matches() || Long.parseLong(m.group(1)) != userId) {
            throw invalid("object_key", "Tệp không hợp lệ.", Map.of());
        }
        ObjectStorage.StoredObject stored = storage.stat(purpose.bucket(), tmpKey)
                .orElseThrow(() -> invalid("object_key", "Tệp chưa được tải lên hoặc đã hết hạn, vui lòng tải lại.",
                        Map.of()));
        if (!purpose.accepts(normalize(stored.contentType()))
                || !purpose.acceptsSize(normalize(stored.contentType()), stored.size())) {
            deleteQuietly(purpose, tmpKey);
            throw invalid("object_key", "Tệp không đúng định dạng hoặc vượt quá dung lượng cho phép.",
                    Map.of("allowed_types", purpose.contentTypes(), "max_bytes", purpose.maxBytes()));
        }
        String finalKey = tmpKey.substring(TMP_PREFIX.length());
        storage.move(purpose.bucket(), tmpKey, finalKey);
        return new StoredFile(finalKey, purpose.publicRead() ? storage.publicUrl(purpose.bucket(), finalKey) : null);
    }

    /** Nội dung một tệp tạm đã tải lên, đã kiểm chủ sở hữu / loại / dung lượng. */
    public record TmpFile(String objectKey, String contentType, byte[] data) {
    }

    /**
     * Đọc tệp {@code tmp/user/{userId}/…} của CHÍNH người gọi để nghiệp vụ tự xử lý rồi ghi ở nơi khác (vd. mã hóa giấy tờ
     * KYC — plan Mở shop & KYC K04.03). Kiểm như {@link #promote}; sai loại / dung lượng thì xóa tệp tạm. Không xóa tệp khi
     * thành công — nghiệp vụ gọi {@link #deleteQuietly} sau khi đã ghi xong bản chính thức.
     */
    public TmpFile readOwnTmp(long userId, UploadPurpose purpose, String tmpKey) {
        var m = tmpKey == null ? null : TMP_KEY.matcher(tmpKey);
        if (m == null || !m.matches() || Long.parseLong(m.group(1)) != userId) {
            throw invalid("object_key", "Tệp không hợp lệ.", Map.of());
        }
        ObjectStorage.StoredObject stored = storage.stat(purpose.bucket(), tmpKey)
                .orElseThrow(() -> invalid("object_key", "Tệp chưa được tải lên hoặc đã hết hạn, vui lòng tải lại.",
                        Map.of()));
        String type = normalize(stored.contentType());
        if (!purpose.accepts(type) || !purpose.acceptsSize(type, stored.size())) {
            deleteQuietly(purpose, tmpKey);
            throw invalid("object_key", "Tệp không đúng định dạng hoặc dung lượng cho phép.",
                    Map.of("allowed_types", purpose.contentTypes(), "max_bytes", purpose.maxBytes(),
                            "min_bytes", purpose.minImageBytes()));
        }
        byte[] data = storage.get(purpose.bucket(), tmpKey)
                .orElseThrow(() -> invalid("object_key", "Tệp chưa được tải lên hoặc đã hết hạn, vui lòng tải lại.",
                        Map.of()));
        return new TmpFile(tmpKey, type, data);
    }

    /**
     * Như {@link #promote} nhưng đặt tệp ở khóa do nghiệp vụ chọn: {@code targetPrefix + <uuid>.<ext>} — vd. ảnh banner
     * {@code banner/{id}/desktop-} (plan banner D4). Vẫn chỉ nhận tệp {@code tmp/user/{userId}/…} của chính người đó.
     */
    public StoredFile promoteTo(long userId, UploadPurpose purpose, String tmpKey, String targetPrefix) {
        StoredFile staged = promote(userId, purpose, tmpKey);
        String fileName = staged.objectKey().substring(staged.objectKey().lastIndexOf('/') + 1);
        String finalKey = targetPrefix + fileName;
        storage.move(purpose.bucket(), staged.objectKey(), finalKey);
        return new StoredFile(finalKey, purpose.publicRead() ? storage.publicUrl(purpose.bucket(), finalKey) : null);
    }

    /** URL công khai của một khóa trong bucket của {@code purpose} (bucket công khai). */
    public String publicUrl(UploadPurpose purpose, String objectKey) {
        return storage.publicUrl(purpose.bucket(), objectKey);
    }

    /**
     * Lưu tệp do backend tự lấy về (không qua link ký) — vd. ảnh đại diện Google khi đăng nhập lần đầu. Vẫn kiểm loại /
     * dung lượng theo {@code purpose}; khóa {@code user/{userId}/{uuid}.{ext}} như tệp người dùng tải lên.
     */
    public StoredFile storeImported(long userId, UploadPurpose purpose, byte[] data, String contentType) {
        String type = normalize(contentType);
        if (data == null || data.length == 0 || !purpose.accepts(type) || data.length > purpose.maxBytes()) {
            throw invalid("file", "Tệp không đúng định dạng hoặc vượt quá dung lượng cho phép.",
                    Map.of("allowed_types", purpose.contentTypes(), "max_bytes", purpose.maxBytes()));
        }
        String key = "user/" + userId + "/" + UUID.randomUUID() + "." + UploadPurpose.extensionOf(type);
        storage.put(purpose.bucket(), key, data, type);
        return new StoredFile(key, purpose.publicRead() ? storage.publicUrl(purpose.bucket(), key) : null);
    }

    /** Khóa của một URL công khai do chính kho này cấp (để xóa ảnh cũ); rỗng nếu là URL ngoài (vd. ảnh Google). */
    public Optional<String> keyOfPublicUrl(UploadPurpose purpose, String url) {
        if (url == null) {
            return Optional.empty();
        }
        String prefix = storage.publicUrl(purpose.bucket(), "");
        return url.startsWith(prefix) && url.length() > prefix.length()
                ? Optional.of(url.substring(prefix.length()))
                : Optional.empty();
    }

    /** Xóa không ném lỗi — dọn dẹp sau khi giao dịch đã xong không được làm hỏng phản hồi. */
    public void deleteQuietly(UploadPurpose purpose, String objectKey) {
        try {
            storage.delete(purpose.bucket(), objectKey);
        } catch (RuntimeException e) {
            log.warn("Không xóa được tệp {}/{}: {}", purpose.bucket(), objectKey, e.getMessage());
        }
    }

    private static String normalize(String contentType) {
        if (contentType == null) {
            return null;
        }
        int semicolon = contentType.indexOf(';');
        return (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).trim().toLowerCase(Locale.ROOT);
    }

    private static BusinessException invalid(String field, String message, Map<String, Object> extra) {
        var details = new java.util.LinkedHashMap<String, Object>();
        details.put("field", field);
        details.putAll(extra);
        return new BusinessException(ErrorCode.VALIDATION_FAILED, message, details);
    }
}
