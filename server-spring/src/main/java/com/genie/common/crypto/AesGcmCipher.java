package com.genie.common.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mã hóa / giải mã AES-256-GCM ở tầng ứng dụng cho dữ liệu đặc biệt nhạy cảm — ảnh giấy tờ KYC (LUU_TRU 5.6–5.7, NFR-07;
 * plan Mở shop & KYC D5, K02.01; ADR 0011 mục 4).
 * <ul>
 *   <li>Định dạng bản mã: {@code IV (12 byte ngẫu nhiên) ‖ ciphertext ‖ tag (16 byte)};</li>
 *   <li>{@code aad} (dữ liệu xác thực kèm, vd {@code "15:ID_FRONT"}) gắn bản mã với đúng chủ / đúng loại giấy tờ — chép
 *       tệp của shop này sang shop khác sẽ không giải mã được;</li>
 *   <li>khóa 256 bit đọc từ {@code genie.security.kyc-aes-key} (biến {@code KYC_AES_KEY}, Base64). Sai độ dài → backend dừng
 *       ngay khi khởi động. Production sinh bằng {@code openssl rand -base64 32}; mất khóa = mất toàn bộ hồ sơ đã mã hóa.</li>
 * </ul>
 * Sai khóa, sai AAD hoặc bản mã bị sửa → {@link CryptoException}.
 */
@Component
public class AesGcmCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmCipher(@Value("${genie.security.kyc-aes-key}") String base64Key) {
        this.key = new SecretKeySpec(decodeKey(base64Key), "AES");
    }

    private static byte[] decodeKey(String base64Key) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key == null ? "" : base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("KYC_AES_KEY không phải chuỗi Base64 hợp lệ — sinh bằng: openssl rand -base64 32");
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException("KYC_AES_KEY phải là khóa 32 byte (AES-256) mã hóa Base64, đang có "
                    + raw.length + " byte — sinh bằng: openssl rand -base64 32");
        }
        return raw;
    }

    /** Mã hóa {@code plaintext}; hai lần gọi với cùng dữ liệu cho bản mã khác nhau (IV ngẫu nhiên). */
    public byte[] encrypt(byte[] plaintext, String aad) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] body = cipher.doFinal(plaintext);
            return ByteBuffer.allocate(IV_BYTES + body.length).put(iv).put(body).array();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Không mã hóa được dữ liệu", e);
        }
    }

    /** Giải mã bản mã do {@link #encrypt} tạo với đúng {@code aad}. */
    public byte[] decrypt(byte[] blob, String aad) {
        if (blob == null || blob.length < IV_BYTES + TAG_BITS / 8) {
            throw new CryptoException("Bản mã không hợp lệ", null);
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES));
            cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(blob, IV_BYTES, blob.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Không giải mã được: sai khóa, sai dữ liệu kèm hoặc bản mã đã bị sửa", e);
        }
    }

    /** Lỗi mã hóa / giải mã — không lộ chi tiết khóa hay dữ liệu. */
    public static class CryptoException extends RuntimeException {
        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
