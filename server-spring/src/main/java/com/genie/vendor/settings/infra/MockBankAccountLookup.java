package com.genie.vendor.settings.infra;

import com.genie.common.domain.NameNormalizer;
import com.genie.vendor.settings.application.BankAccountLookup;
import org.springframework.stereotype.Component;

/**
 * Giả lập tra tên chủ tài khoản (plan Mở shop & KYC D13): trả tên người dùng nhập đã bỏ dấu + viết hoa, trừ số tài khoản kết
 * thúc {@code 000} → trả tên khác (để thử ca không khớp). Có độ trễ giả 300 ms như gọi đối tác thật.
 */
@Component
public class MockBankAccountLookup implements BankAccountLookup {

    static final String MISMATCH_HOLDER = "NGUYEN VAN KHAC";
    static final long DELAY_MS = 300;

    @Override
    public String holderName(String bankCode, String accountNumber, String declaredHolder) {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (accountNumber != null && accountNumber.endsWith("000")) {
            return MISMATCH_HOLDER;
        }
        return NameNormalizer.normalize(declaredHolder);
    }
}
