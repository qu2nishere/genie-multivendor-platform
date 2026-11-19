package com.genie.vendor.settings.application;

/**
 * Cổng tra tên chủ tài khoản ngân hàng (UC-VND-07 bước 4; implement 8.3; plan Mở shop & KYC D13, K08.02). Bản thật đi qua
 * đối tác chi hộ / NAPAS (ngoài phạm vi đồ án); hiện dùng {@code MockBankAccountLookup}.
 */
public interface BankAccountLookup {

    /** Tên chủ tài khoản ngân hàng trả về (viết hoa, không dấu). */
    String holderName(String bankCode, String accountNumber, String declaredHolder);
}
