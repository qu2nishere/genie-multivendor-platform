package com.genie.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.genie.common.mail.MailTemplate;
import com.genie.support.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Tài khoản nhận giải ngân — UC-VND-07 (API_SPEC 10.11; plan Mở shop & KYC K08.08). */
class PayoutAccountIT extends KycTestSupport {

    private static final String URL = "/api/v1/seller/payout-account";

    private long vendor;
    private String token;
    private String email;

    private void verifiedShop() {
        vendor = new TestDataBuilder(jdbc).vendor();
        jdbc.update("UPDATE vendor_profiles SET representative = 'Đặng Quốc Huy' WHERE user_id = ?", vendor);
        token = vendorToken(vendor);
        email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, vendor);
    }

    private static String body(String account, String holder) {
        return "{\"bank_code\":\"VCB\",\"account_number\":\"" + account + "\",\"account_holder\":\"" + holder + "\"}";
    }

    /** Xin mã bước 2 rồi trả {challengeId, code}. Bỏ giới hạn 60 giây giữa hai lần gửi để test gửi nhiều mã. */
    private String[] otp(String purpose) throws Exception {
        redis.delete("rl:otp:" + email);
        String res = mvc.perform(post("/api/v1/me/otp-challenges").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"purpose\":\"" + purpose + "\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        return new String[] {com.jayway.jsonpath.JsonPath.read(res, "$.data.challenge_id"), mails.lastOtp(email)};
    }

    private ResultActions save(String json, String[] otp) throws Exception {
        var req = put(URL).header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON).content(json);
        if (otp != null) {
            req = req.header("X-OTP-Challenge-Id", otp[0]).header("X-OTP-Code", otp[1]);
        }
        return mvc.perform(req);
    }

    @Test
    void verificationMatchesKycName() throws Exception {
        verifiedShop();
        mvc.perform(post(URL + "/verification").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("0071000006712", "Đặng Quốc Huy")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matched").value(true))
                .andExpect(jsonPath("$.data.normalized_holder").value("DANG QUOC HUY"));
        mvc.perform(post(URL + "/verification").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("0071000006000", "Đặng Quốc Huy")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("GNE-WAL-4222"))
                .andExpect(jsonPath("$.details.kyc_name").value("DANG QUOC HUY"));
        mvc.perform(get("/api/v1/public/banks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("VCB"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=86400, public"));
    }

    @Test
    void saveNeedsStepUpOtpAndReverifiesOnServer() throws Exception {
        verifiedShop();
        save(body("0071000006712", "Đặng Quốc Huy"), null)
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GNE-AUTH-4222"));
        // mã của mục đích khác không dùng được
        String[] wrongPurpose = otp("PAYOUT_ACCOUNT_DELETE");
        save(body("0071000006712", "Đặng Quốc Huy"), wrongPurpose).andExpect(status().isGone());

        save(body("0071000006712", "Đặng Quốc Huy"), otp("PAYOUT_ACCOUNT_UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bank_name").value("Vietcombank"))
                .andExpect(jsonPath("$.data.account_number_masked").value("•••• •••• 6712"))
                .andExpect(jsonPath("$.data.account_holder").value("DANG QUOC HUY"))
                .andExpect(jsonPath("$.data.is_verified").value(true));
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.data.account_number_masked").value("•••• •••• 6712"));
        var row = jdbc.queryForMap("SELECT * FROM vendor_profiles WHERE user_id = ?", vendor);
        assertThat(row.get("payout_account_verified_at")).isNotNull();
        String details = jdbc.queryForObject("SELECT details FROM system_logs WHERE action_type = 'PAYOUT_ACCOUNT_CHANGED'",
                String.class);
        assertThat(details).contains("6712").doesNotContain("0071000006712");
        await(() -> mails.last(email, MailTemplate.PAYOUT_ACCOUNT_CHANGED).isPresent());

        // đổi sang tài khoản không khớp tên → không lưu, tài khoản cũ giữ nguyên
        save(body("1234567000", "Đặng Quốc Huy"), otp("PAYOUT_ACCOUNT_UPDATE"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("GNE-WAL-4222"));
        assertThat(jdbc.queryForObject("SELECT payout_bank_account_number FROM vendor_profiles WHERE user_id = ?",
                String.class, vendor)).isEqualTo("0071000006712");

        String[] del = otp("PAYOUT_ACCOUNT_DELETE");
        mvc.perform(delete(URL).header(HttpHeaders.AUTHORIZATION, token)
                        .header("X-OTP-Challenge-Id", del[0]).header("X-OTP-Code", del[1]))
                .andExpect(status().isNoContent());
        var cleared = jdbc.queryForMap("SELECT payout_bank_name, payout_account_verified_at FROM vendor_profiles WHERE user_id = ?",
                vendor);
        assertThat(cleared.get("payout_bank_name")).isNull();
        assertThat(cleared.get("payout_account_verified_at")).isNull();
    }

    @Test
    void unverifiedShopIsForbidden() throws Exception {
        verifiedShop();
        jdbc.update("UPDATE vendor_profiles SET kyc_status = 'PENDING' WHERE user_id = ?", vendor);
        mvc.perform(post(URL + "/verification").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("0071000006712", "Đặng Quốc Huy")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("GNE-VND-4031"));
    }
}
