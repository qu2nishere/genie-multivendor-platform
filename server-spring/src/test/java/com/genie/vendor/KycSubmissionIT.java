package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.vendor.kyc.application.KycDossierStorage;
import com.genie.vendor.kyc.domain.KycDocumentType;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Nộp hồ sơ KYC — UC-VND-02 (API_SPEC 10.12; plan Mở shop & KYC K04.10). */
class KycSubmissionIT extends KycTestSupport {

    private static final String KYC = "/api/v1/seller/kyc";

    @Autowired
    KycDossierStorage dossiers;

    private ResultActions submit(String token, String json) throws Exception {
        return mvc.perform(post(KYC + "/submissions").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String form(String type, String identity, String tax, Map<String, String> docs, boolean consent) {
        StringBuilder d = new StringBuilder();
        docs.forEach((k, v) -> d.append(d.isEmpty() ? "" : ",").append('"').append(k).append("\":\"").append(v).append('"'));
        return "{\"shop_name\":null,\"business_type\":\"" + type + "\",\"identity_number\":\"" + identity + "\","
                + "\"tax_code\":" + (tax == null ? "null" : "\"" + tax + "\"") + ",\"representative\":\"Đặng Quốc Huy\","
                + "\"document_object_keys\":{" + d + "},\"document_file_names\":{\"ID_FRONT\":\"cccd-truoc.jpg\"},"
                + "\"biometric_consent\":" + consent + "}";
    }

    @Test
    void submitEncryptsDocumentsAndMovesToPending() throws Exception {
        long[] s = newShop();
        String t = vendorToken(s[0]);
        Object[] front = uploadTmp(s[0], "image/jpeg", 40_000);
        String back = tmpImage(s[0]);
        String selfie = tmpImage(s[0]);
        submit(t, form("INDIVIDUAL", "079096001234", null,
                Map.of("ID_FRONT", (String) front[0], "ID_BACK", back, "SELFIE", selfie), true))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));

        var row = jdbc.queryForMap("SELECT * FROM vendor_profiles WHERE id = ?", s[1]);
        String frontKey = (String) row.get("id_front_url");
        assertThat(frontKey).startsWith("vendor/" + s[1] + "/front-").endsWith(".enc");
        assertThat((String) row.get("selfie_url")).startsWith("vendor/" + s[1] + "/selfie-");
        assertThat(row.get("kyc_biometric_consent")).isEqualTo(true);
        assertThat(row.get("identity_code")).isEqualTo("079096001234");
        // bản tạm đã xóa, bản lưu là bản mã (không phải ảnh gốc) nhưng giải mã ra đúng tệp gốc
        assertThat(storage.stat("kyc-dossiers", (String) front[0])).isEmpty();
        byte[] stored = storage.get("kyc-dossiers", frontKey).orElseThrow();
        assertThat(stored).isNotEqualTo(front[1]);
        assertThat(dossiers.decrypt(s[1], KycDocumentType.ID_FRONT, frontKey)).isEqualTo(front[1]);
        // nhật ký có thời điểm đồng ý sinh trắc
        String details = jdbc.queryForObject("SELECT details FROM system_logs WHERE action_type = 'KYC_SUBMITTED' "
                + "AND reference_id = ?", String.class, String.valueOf(s[1]));
        assertThat(details).contains("\"biometric_consent\":true").contains("consent_at");
        // nộp lại khi đang xử lý
        submit(t, form("INDIVIDUAL", "079096001234", null, Map.of(), true))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-VND-4095"));
    }

    @Test
    void missingDocumentsAndBusinessFields() throws Exception {
        long[] s = newShop();
        String t = vendorToken(s[0]);
        submit(t, form("INDIVIDUAL", "079096001234", null, Map.of("ID_FRONT", tmpImage(s[0]), "ID_BACK", tmpImage(s[0])), true))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-VND-4223"))
                .andExpect(jsonPath("$.details.missing[0]").value("SELFIE"));
        submit(t, form("HOUSEHOLD", "079096001234", null, Map.of(), true))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details.field").value("tax_code"));
        submit(t, form("HOUSEHOLD", "079096001234", "0312345678",
                Map.of("ID_FRONT", tmpImage(s[0]), "ID_BACK", tmpImage(s[0]), "SELFIE", tmpImage(s[0])), true))
                .andExpect(jsonPath("$.code").value("GNE-VND-4223"))
                .andExpect(jsonPath("$.details.missing[0]").value("BUSINESS_LICENSE"));
        submit(t, form("INDIVIDUAL", "07909600", null, Map.of(), true))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.details.field").value("identity_number"));
        assertThat(jdbc.queryForObject("SELECT kyc_status FROM vendor_profiles WHERE id = ?", String.class, s[1]))
                .isEqualTo("NOT_SUBMITTED");
    }

    @Test
    void otherPeoplesObjectKeysAreRejected() throws Exception {
        long[] mine = newShop();
        long[] other = newShop();
        String foreign = tmpImage(other[0]);
        submit(vendorToken(mine[0]), form("INDIVIDUAL", "079096001234", null,
                Map.of("ID_FRONT", foreign, "ID_BACK", tmpImage(mine[0]), "SELFIE", tmpImage(mine[0])), true))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.field").value("object_key"));
        assertThat(storage.stat("kyc-dossiers", foreign)).isPresent();     // tệp của người khác không bị đụng tới
        // tệp tạm quá nhỏ (ảnh < 20 KB) bị từ chối khi niêm phong
        String tiny = (String) uploadTmp(mine[0], "image/png", 5_000)[0];
        submit(vendorToken(mine[0]), form("INDIVIDUAL", "079096001234", null,
                Map.of("ID_FRONT", tiny, "ID_BACK", tmpImage(mine[0]), "SELFIE", tmpImage(mine[0])), true))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void draftIsReturnedWithoutUrlsAndResubmitKeepsOldDocuments() throws Exception {
        long[] s = newShop();
        String t = vendorToken(s[0]);
        mvc.perform(put(KYC + "/draft").header(HttpHeaders.AUTHORIZATION, t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"business_type\":\"INDIVIDUAL\",\"identity_number\":\"0790960\","
                                + "\"document_object_keys\":{\"ID_FRONT\":\"" + tmpImage(s[0]) + "\"},"
                                + "\"document_file_names\":{\"ID_FRONT\":\"cccd-truoc.jpg\"},\"biometric_consent\":true}"))
                .andExpect(status().isOk());
        String body = mvc.perform(get(KYC).header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.data.status").value("NOT_SUBMITTED"))
                .andExpect(jsonPath("$.data.identity_number").value("0790960"))
                .andExpect(jsonPath("$.data.business_type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.data.documents.ID_FRONT.file_name").value("cccd-truoc.jpg"))
                .andExpect(jsonPath("$.data.documents.ID_FRONT.size_bytes").value(30_000))
                .andExpect(jsonPath("$.data.documents.ID_BACK").isEmpty())
                .andExpect(jsonPath("$.data.biometric_consent").value(true))
                .andExpect(jsonPath("$.data.manual_review_requested").value(false))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).doesNotContain("http").doesNotContain(".enc");

        // nộp → bị từ chối → nộp lại không gửi tệp mới: giữ giấy tờ cũ
        submit(t, form("INDIVIDUAL", "079096001234", null, Map.of("ID_BACK", tmpImage(s[0]), "SELFIE", tmpImage(s[0])), false))
                .andExpect(status().isAccepted());
        String frontKey = jdbc.queryForObject("SELECT id_front_url FROM vendor_profiles WHERE id = ?", String.class, s[1]);
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'REJECTED', kyc_rejection_reason = 'Ảnh mờ' WHERE id = ?", s[1]);
        mvc.perform(get(KYC).header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(jsonPath("$.data.rejection_reason").value("Ảnh mờ"));
        submit(t, form("INDIVIDUAL", "079096001234", null, Map.of(), false)).andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT id_front_url FROM vendor_profiles WHERE id = ?", String.class, s[1]))
                .isEqualTo(frontKey);
        assertThat(storage.stat("kyc-dossiers", frontKey)).isPresent();
    }

    @Test
    void manualReviewRequestOnlyOnceAfterRejection() throws Exception {
        long[] s = newShop();
        String t = vendorToken(s[0]);
        mvc.perform(post(KYC + "/manual-review-requests").header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-VND-4095"));
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'REJECTED' WHERE id = ?", s[1]);
        mvc.perform(post(KYC + "/manual-review-requests").header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT kyc_status FROM vendor_profiles WHERE id = ?", String.class, s[1]))
                .isEqualTo("MANUAL_REVIEW");
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'REJECTED' WHERE id = ?", s[1]);
        mvc.perform(post(KYC + "/manual-review-requests").header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GNE-VND-4096"));
        mvc.perform(get(KYC).header(HttpHeaders.AUTHORIZATION, t))
                .andExpect(jsonPath("$.data.manual_review_requested").value(true));
    }
}
