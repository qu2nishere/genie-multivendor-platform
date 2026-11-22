// Nhân viên kích hoạt lời mời: xin OTP → xác minh OTP + đặt mật khẩu → mở phiên (plan B1 R02.05, mục 3.3; API 2.11)
package com.genie.auth.staff.application;

import com.genie.auth.login.application.AuthResult;
import com.genie.auth.login.application.LoginService;
import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpChallengeService;
import com.genie.auth.otp.application.OtpExhaustedException;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.profile.application.AccountSummaryService;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * UC-ADM-01 bước 5 (module {@code auth}): mã lời mời sai / hết hạn / tài khoản không còn chờ kích hoạt đều trả
 * {@code 410 GNE-AUTH-4103} (không phân biệt để không lộ thông tin). OTP gửi tới đúng email được mời.
 */
@Service
public class StaffActivationService {

    static final String METHOD = "STAFF_ACTIVATION";

    private final StaffInvitationStore invitations;
    private final OtpChallengeService otps;
    private final UserRepository users;
    private final AccountSummaryService summaries;
    private final PasswordEncoder passwordEncoder;
    private final LoginService logins;
    private final Clock clock;

    public StaffActivationService(StaffInvitationStore invitations, OtpChallengeService otps, UserRepository users,
                                  AccountSummaryService summaries, PasswordEncoder passwordEncoder,
                                  LoginService logins, Clock clock) {
        this.invitations = invitations;
        this.otps = otps;
        this.users = users;
        this.summaries = summaries;
        this.passwordEncoder = passwordEncoder;
        this.logins = logins;
        this.clock = clock;
    }

    /** Bước 1 — gửi OTP {@code STAFF_ACTIVATION} tới email được mời. */
    @Transactional(readOnly = true)
    public IssuedOtp requestOtp(String invitationToken) {
        long userId = invitations.resolve(invitationToken).orElseThrow(StaffActivationService::expired);
        User user = users.findById(userId).filter(User::isPendingStaffActivation)
                .orElseThrow(StaffActivationService::expired);
        String name = summaries.summaryOf(user).fullName();
        return otps.issue(OtpPurpose.STAFF_ACTIVATION, user.getEmail(), userId,
                name == null ? Map.of() : Map.of(OtpChallengeService.DATA_NAME, name));
    }

    /** Bước 2 — xác minh OTP, đặt mật khẩu, {@code UNVERIFIED → ACTIVE}, mở phiên như đăng nhập. */
    @Transactional
    public AuthResult activate(String invitationToken, String challengeId, String otp, String password,
                               ClientInfo client) {
        long userId = invitations.resolve(invitationToken).orElseThrow(StaffActivationService::expired);
        try {
            otps.verifyForUser(challengeId, otp, OtpPurpose.STAFF_ACTIVATION, userId);
        } catch (OtpExhaustedException e) {
            throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS, Map.of("remaining_attempts", 0));
        }
        User user = users.findByIdForUpdate(userId).filter(User::isPendingStaffActivation)
                .orElseThrow(StaffActivationService::expired);
        user.activateStaff(passwordEncoder.encode(password), OffsetDateTime.now(clock));
        afterCommit(() -> invitations.cancel(userId));   // mã chỉ dùng một lần
        return logins.completeLogin(user, client, METHOD);
    }

    private static BusinessException expired() {
        return new BusinessException(ErrorCode.INVITATION_EXPIRED);
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
