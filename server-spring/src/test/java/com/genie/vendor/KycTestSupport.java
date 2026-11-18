package com.genie.vendor;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.common.storage.ObjectStorage;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import java.util.Random;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;

/** Tiện ích chung cho test KYC người bán (plan Mở shop & KYC K04–K06). */
abstract class KycTestSupport extends IntegrationTestBase {

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected ObjectStorage storage;

    /** Shop mới chưa nộp KYC — trả {vendorUserId, vendorProfileId}. */
    protected long[] newShop() {
        TestDataBuilder data = new TestDataBuilder(jdbc);
        long vendor = data.vendor();
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'NOT_SUBMITTED', business_type = NULL WHERE user_id = ?", vendor);
        return new long[] {vendor, data.shopIdOf(vendor)};
    }

    protected String vendorToken(long vendorUserId) {
        return "Bearer " + jwtService.issue(vendorUserId, UserRole.VENDOR, 1L).token();
    }

    /** Ảnh giả ≥ 20 KB đặt thẳng vào {@code tmp/user/{userId}/…} như trình duyệt vừa PUT lên — trả {khóa, nội dung}. */
    protected Object[] uploadTmp(long userId, String contentType, int size) {
        byte[] data = new byte[size];
        new Random(size + userId).nextBytes(data);
        String ext = contentType.equals("application/pdf") ? "pdf" : contentType.equals("image/png") ? "png" : "jpg";
        String key = "tmp/user/" + userId + "/" + UUID.randomUUID() + "." + ext;
        storage.put("kyc-dossiers", key, data, contentType);
        return new Object[] {key, data};
    }

    protected String tmpImage(long userId) {
        return (String) uploadTmp(userId, "image/jpeg", 30_000)[0];
    }
}
