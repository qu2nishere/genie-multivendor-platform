package com.genie.auth.oauth.application;

import com.genie.auth.otp.application.IssuedOtp;
import com.genie.auth.otp.application.OtpChallenge;
import com.genie.auth.otp.application.OtpChallengeService;
import com.genie.auth.otp.application.OtpExhaustedException;
import com.genie.auth.otp.application.OtpPurpose;
import com.genie.auth.profile.domain.User;
import com.genie.auth.profile.domain.UserRepository;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.security.ClientInfo;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hoàn tất đăng ký bằng nhà cung cấp KHÔNG trả email — Zalo (UC-AUTH-03, plan AUTH A14.10, D23).
 * <ol>
 *   <li>Đăng nhập Zalo lần đầu → {@code SIGNUP_REQUIRED} + {@code signup_token} (Redis 15 phút, xem
 *       {@link OAuthLoginService#complete}).</li>
 *   <li>{@link #requestOtp} ({@code POST /auth/oauth/zalo/signup}): người dùng nhập email → email đã thuộc tài khoản Genie
 *       → {@code 409 GNE-AUTH-4091} kèm {@code next_step = LINK_FROM_ACCOUNT} (KHÔNG tự gộp — Zalo không chứng minh được
 *       quyền sở hữu email; chủ tài khoản đăng nhập rồi liên kết ở Tài khoản › Liên kết đăng nhập); còn trống → gửi OTP
 *       {@code OAUTH_SIGNUP} tới email đó.</li>
 *   <li>{@link #verify} (qua {@code POST /auth/otp-challenges/{id}/verify}, màn OTP dùng chung): mã đúng → tạo người mua
 *       (email đã xác minh, không mật khẩu, gắn sẵn Zalo) → đăng nhập ({@code 201 $AuthTokens}).</li>
 * </ol>
 * Không ghi PostgreSQL trước khi OTP đúng; không thêm bảng / cột — OTP và phiên tạm đều ở Redis.
 */
@Service
public class OAuthSignupService {

    /** Khóa trong {@code data} của challenge OTP: mã phiên đăng ký tạm. */
    static final String DATA_SIGNUP_TOKEN = "signup_token";

    private final PendingOAuthSignupStore signups;
    private final OtpChallengeService otps;
    private final UserRepository users;
    private final OAuthLoginService oauth;
    private final Clock clock;

    public OAuthSignupService(PendingOAuthSignupStore signups, OtpChallengeService otps, UserRepository users,
                              OAuthLoginService oauth, Clock clock) {
        this.signups = signups;
        this.otps = otps;
        this.users = users;
        this.oauth = oauth;
        this.clock = clock;
    }

    /** Nhập email cho phiên đăng ký Zalo → gửi OTP ({@code 202 $OtpChallenge}). */
    @Transactional(readOnly = true)
    public IssuedOtp requestOtp(OAuthProvider provider, String signupToken, String rawEmail) {
        PendingOAuthSignup pending = pending(provider, signupToken);
        String email = User.normalizeEmail(rawEmail);
        ensureEmailFree(email, provider);
        ensureIdentityFree(pending);
        Map<String, String> data = new HashMap<>();
        data.put(DATA_SIGNUP_TOKEN, signupToken);
        if (pending.name() != null) {
            data.put(OtpChallengeService.DATA_NAME, pending.name());
        }
        return otps.issue(OtpPurpose.OAUTH_SIGNUP, email, null, data);
    }

    /** OTP đúng → tạo tài khoản gắn Zalo và đăng nhập. */
    @Transactional
    public OAuthLoginResult verify(String challengeId, String code, ClientInfo client) {
        OtpChallenge challenge;
        try {
            challenge = otps.verify(challengeId, code, OtpPurpose.OAUTH_SIGNUP);
        } catch (OtpExhaustedException e) {
            // sai 5 lần: hủy luôn phiên đăng ký — phải đăng nhập Zalo lại từ đầu
            signups.delete(e.getChallenge().data().get(DATA_SIGNUP_TOKEN));
            throw e;
        }
        String token = challenge.data().get(DATA_SIGNUP_TOKEN);
        PendingOAuthSignup pending = signups.find(token).orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED,
                "Phiên đăng ký đã hết hạn, vui lòng đăng nhập Zalo lại."));
        String email = challenge.email();
        ensureEmailFree(email, pending.provider());   // có thể vừa có người đăng ký email này trong lúc chờ OTP
        ensureIdentityFree(pending);
        IdentityClaims claims = new IdentityClaims(pending.subject(), null, false, pending.name(), pending.picture());
        User user = oauth.createAccount(pending.provider(), claims, email, OffsetDateTime.now(clock), client);
        signups.delete(token);
        return oauth.done(pending.provider(), OAuthLoginResult.Outcome.CREATED, user, client, pending.next());
    }

    private PendingOAuthSignup pending(OAuthProvider provider, String token) {
        return signups.find(token).filter(p -> p.provider() == provider)
                .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_INVALID,
                        "Phiên đăng ký đã hết hạn, vui lòng đăng nhập " + provider.label() + " lại."));
    }

    private void ensureEmailFree(String email, OAuthProvider provider) {
        if (users.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED,
                    "Email này đã có tài khoản Genie. Hãy đăng nhập rồi liên kết " + provider.label()
                            + " ở Tài khoản › Liên kết đăng nhập.",
                    Map.of("next_step", "LINK_FROM_ACCOUNT", "provider", provider.name()));
        }
    }

    /** Zalo này vừa được gắn vào tài khoản khác (liên kết thủ công song song) → {@code 409 4092}. */
    private void ensureIdentityFree(PendingOAuthSignup pending) {
        if (users.findByOauthProviderAndOauthProviderId(pending.provider().id(), pending.subject()).isPresent()) {
            throw new BusinessException(ErrorCode.IDENTITY_ALREADY_LINKED);
        }
    }
}
