package com.genie.vendor.kyc.application;

import java.util.Map;

/**
 * Cổng gửi hồ sơ cho AI Worker thẩm định (UC-VND-03; plan Mở shop & KYC K05.04–K05.05). Cài đặt:
 * {@code HttpKycVerificationClient} ({@code genie.ai-worker.mode = http}) và {@code StubKycVerificationClient}
 * ({@code stub} — giả lập trong backend, ADR 0011 mục 6). Lỗi gửi → ném ngoại lệ, hồ sơ chuyển duyệt tay.
 */
public interface KycVerificationClient {

    /**
     * @param kycId            {@code vendor_profiles.id}
     * @param documents        {@code id_front, id_back, selfie} → URL bản xem tạm 5 phút
     * @param identityNumber   số CCCD kê khai
     * @param fullName         họ tên kê khai (người đại diện)
     * @param biometricConsent đồng ý so khớp khuôn mặt
     * @param callbackUrl      {@code …/api/v1/internal/kyc/{id}/result}
     */
    record Request(long kycId, Map<String, String> documents, String identityNumber, String fullName,
                   boolean biometricConsent, String callbackUrl) {
    }

    void requestVerification(Request request);
}
