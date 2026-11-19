package com.genie.vendor.kyc.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Kết quả AI Worker gửi về (hợp đồng ADR 0011 mục 5; API_SPEC 12): từng phép kiểm + mã tín hiệu + dữ liệu trích xuất.
 * AI Worker KHÔNG kết luận — backend tính điểm và chốt ({@link KycDecisionService}).
 */
public record KycAiResult(Checks checks, BigDecimal faceMatchScore, BigDecimal livenessScore, List<String> signals,
                          Extracted extracted, List<String> errors, Map<String, String> model) {

    /** Mỗi giá trị {@code true / false / null} (null = không kiểm được / không áp dụng). */
    public record Checks(Boolean qrCode, Boolean mrz, Boolean mrzQrMatch, Boolean cccdNumberValid, Boolean expiryRuleValid,
                         Boolean faceMatch, Boolean faceDuplicate, Boolean nationalEmblem, Boolean fingerprint) {

        public static Checks empty() {
            return new Checks(null, null, null, null, null, null, null, null, null);
        }
    }

    /** Dữ liệu đọc từ QR mặt trước / MRZ mặt sau (ngày dạng ddMMyyyy như QR). */
    public record Extracted(String identityNumber, String fullName, String dateOfBirth, String gender, String address,
                            String issueDate, String expiryDate, String legacyIdentityNumber) {
    }
}
