package com.genie.auth.otp.application;

import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xin / gửi lại mã OTP bước 2 cho người đang đăng nhập (API_SPEC 3.5, plan AUTH A05.05).
 * Mã gửi tới email hiện tại của tài khoản; trước khi gửi chạy các {@link StepUpPrecondition} của mục đích đó.
 */
@Service
public class StepUpOtpService {

    private final OtpChallengeService otps;
    private final UserRepository users;
    private final List<StepUpPrecondition> preconditions;

    public StepUpOtpService(OtpChallengeService otps, UserRepository users, List<StepUpPrecondition> preconditions) {
        this.otps = otps;
        this.users = users;
        this.preconditions = preconditions;
    }

    @Transactional(readOnly = true)
    public IssuedOtp request(long userId, OtpPurpose purpose) {
        if (purpose == null || !purpose.isStepUp()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Mục đích OTP không hợp lệ.");
        }
        User user = users.findById(userId).filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        for (StepUpPrecondition p : preconditions) {
            if (p.supports(purpose)) {
                p.check(userId, purpose);
            }
        }
        return otps.issue(purpose, user.getEmail(), userId, Map.of());
    }

    public IssuedOtp resend(long userId, String challengeId) {
        OtpChallenge challenge = otps.find(challengeId)
                .filter(c -> Objects.equals(c.userId(), userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED));
        return otps.resend(challenge.id());
    }
}
