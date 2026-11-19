package com.genie.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.support.IntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Đọc / ghi nội dung tệp trên Silo thật — cần cho mã hóa hồ sơ KYC (plan Mở shop & KYC K02.03). */
class ObjectStorageIT extends IntegrationTestBase {

    @Autowired
    ObjectStorage storage;

    @Test
    void putGetStatAndDeleteInPrivateBucket() {
        String key = "test/" + UUID.randomUUID() + ".bin";
        byte[] data = {9, 8, 7, 6, 5};
        storage.put("kyc-dossiers", key, data, "application/octet-stream");
        assertThat(storage.get("kyc-dossiers", key)).hasValue(data);
        assertThat(storage.stat("kyc-dossiers", key)).hasValueSatisfying(o -> assertThat(o.size()).isEqualTo(5));
        storage.delete("kyc-dossiers", key);
        assertThat(storage.get("kyc-dossiers", key)).isEmpty();
        assertThat(storage.presignGet("kyc-dossiers", "x/y.jpg", java.time.Duration.ofMinutes(5)))
                .contains("X-Amz-Expires=300");
    }
}
