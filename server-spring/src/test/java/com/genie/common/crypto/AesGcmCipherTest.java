package com.genie.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** AES-256-GCM cho hồ sơ KYC (plan Mở shop & KYC K02.02). */
class AesGcmCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    private final AesGcmCipher cipher = new AesGcmCipher(KEY);

    @Test
    void roundTripsAndUsesRandomIv() {
        byte[] data = "ảnh CCCD mặt trước".getBytes(StandardCharsets.UTF_8);
        byte[] a = cipher.encrypt(data, "15:ID_FRONT");
        byte[] b = cipher.encrypt(data, "15:ID_FRONT");
        assertThat(a).isNotEqualTo(b);                            // IV ngẫu nhiên
        assertThat(a.length).isEqualTo(12 + data.length + 16);    // IV ‖ ciphertext ‖ tag
        assertThat(cipher.decrypt(a, "15:ID_FRONT")).isEqualTo(data);
        assertThat(cipher.decrypt(b, "15:ID_FRONT")).isEqualTo(data);
    }

    @Test
    void tamperedDataWrongAadOrWrongKeyFail() {
        byte[] blob = cipher.encrypt(new byte[] {1, 2, 3, 4, 5}, "15:SELFIE");
        byte[] tampered = blob.clone();
        tampered[14] ^= 0x01;
        assertThatThrownBy(() -> cipher.decrypt(tampered, "15:SELFIE")).isInstanceOf(AesGcmCipher.CryptoException.class);
        assertThatThrownBy(() -> cipher.decrypt(blob, "16:SELFIE")).isInstanceOf(AesGcmCipher.CryptoException.class);
        AesGcmCipher other = new AesGcmCipher(Base64.getEncoder().encodeToString(new byte[32]));
        assertThatThrownBy(() -> other.decrypt(blob, "15:SELFIE")).isInstanceOf(AesGcmCipher.CryptoException.class);
        assertThatThrownBy(() -> cipher.decrypt(new byte[5], "x")).isInstanceOf(AesGcmCipher.CryptoException.class);
    }

    @Test
    void rejectsKeysOfWrongLength() {
        assertThatThrownBy(() -> new AesGcmCipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 byte");
        assertThatThrownBy(() -> new AesGcmCipher("không-phải-base64!"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Base64");
        assertThatThrownBy(() -> new AesGcmCipher(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handlesTenMegabyteFiles() {
        byte[] data = new byte[10 * 1024 * 1024];
        new Random(42).nextBytes(data);
        byte[] blob = cipher.encrypt(data, "15:BUSINESS_LICENSE");
        assertThat(cipher.decrypt(blob, "15:BUSINESS_LICENSE")).isEqualTo(data);
    }
}
