package com.genie.vendor;

import com.genie.common.domain.UserRole;
import com.genie.common.security.JwtService;
import com.genie.common.storage.ObjectStorage;
import com.genie.support.IntegrationTestBase;
import com.genie.support.TestDataBuilder;
import java.util.Random;
import java.util.function.BooleanSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    /** Chờ điều kiện (listener @Async) tối đa 10 giây. */
    protected static void await(BooleanSupplier condition) throws InterruptedException {
        long until = System.currentTimeMillis() + 10_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > until) {
                throw new AssertionError("Hết thời gian chờ điều kiện");
            }
            Thread.sleep(50);
        }
    }

    protected String kycStatus(long vendorProfileId) {
        return jdbc.queryForObject("SELECT kyc_status FROM vendor_profiles WHERE id = ?", String.class, vendorProfileId);
    }

    /**
     * Shop mới đã nộp KYC (CCCD {@code identity}, đại diện "Đặng Quốc Huy") và đang chờ AI ({@code AI_PROCESSING}) —
     * trả {vendorUserId, vendorProfileId}.
     */
    protected long[] submitted(String identity, boolean consent) throws Exception {
        long[] s = newShop();
        String body = "{\"business_type\":\"INDIVIDUAL\",\"identity_number\":\"" + identity + "\","
                + "\"representative\":\"Đặng Quốc Huy\",\"document_object_keys\":{\"ID_FRONT\":\"" + tmpImage(s[0])
                + "\",\"ID_BACK\":\"" + tmpImage(s[0]) + "\",\"SELFIE\":\"" + tmpImage(s[0]) + "\"},"
                + "\"biometric_consent\":" + consent + "}";
        int status = mvc.perform(post("/api/v1/seller/kyc/submissions").header(HttpHeaders.AUTHORIZATION, vendorToken(s[0]))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
        if (status != 202) {
            throw new AssertionError("Nộp KYC trả " + status);
        }
        await(() -> "AI_PROCESSING".equals(kycStatus(s[1])) || "MANUAL_REVIEW".equals(kycStatus(s[1])));
        return s;
    }

    /** Thân callback AI Worker (hợp đồng ADR 0011 mục 5). */
    protected static String callbackBody(String extractedId, String extractedName, String... signals) {
        StringBuilder sig = new StringBuilder();
        for (String c : signals) {
            sig.append(sig.isEmpty() ? "" : ",").append("{\"code\":\"").append(c).append("\"}");
        }
        return "{\"checks\":{\"qr_code\":true,\"mrz\":true,\"mrz_qr_match\":true,\"cccd_number_valid\":true,"
                + "\"expiry_rule_valid\":true,\"face_match\":true,\"face_duplicate\":null,\"national_emblem\":true,"
                + "\"fingerprint\":null},\"face_match_score\":0.91,\"liveness_score\":0.88,\"signals\":[" + sig + "],"
                + "\"extracted\":{\"identity_number\":\"" + extractedId + "\",\"full_name\":\"" + extractedName
                + "\",\"date_of_birth\":\"12041990\",\"gender\":\"Nam\"},\"errors\":[],"
                + "\"model\":{\"pipeline_version\":\"test\"}}";
    }
}
