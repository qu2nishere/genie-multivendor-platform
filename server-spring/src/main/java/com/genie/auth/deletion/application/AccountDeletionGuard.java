package com.genie.auth.deletion.application;

import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.otp.application.StepUpPrecondition;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Gộp lý do chặn từ mọi {@link AccountDeletionBlocker} → {@code 409 GNE-USR-4091} kèm {@code details}.
 * Chạy 2 lần: trước khi gửi mã OTP {@code ACCOUNT_DELETION} (màn hình báo ngay, không tốn mã — API_SPEC 3.5) và
 * lúc xác nhận xóa (có thể phát sinh đơn mới trong lúc nhập mã).
 */
@Component
public class AccountDeletionGuard implements StepUpPrecondition {

    private final List<AccountDeletionBlocker> blockers;

    public AccountDeletionGuard(List<AccountDeletionBlocker> blockers) {
        this.blockers = blockers;
    }

    @Override
    public boolean supports(OtpPurpose purpose) {
        return purpose == OtpPurpose.ACCOUNT_DELETION;
    }

    @Override
    public void check(long userId, OtpPurpose purpose) {
        ensureDeletable(userId);
    }

    public void ensureDeletable(long userId) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (AccountDeletionBlocker blocker : blockers) {
            Map<String, Object> reason = blocker.blockingDetails(userId);
            if (reason != null) {
                details.putAll(reason);
            }
        }
        if (!details.isEmpty()) {
            throw new BusinessException(ErrorCode.ACTIVE_ORDERS_BLOCK_DELETION, details);
        }
    }
}
