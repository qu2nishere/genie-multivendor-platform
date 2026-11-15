package com.genie.auth.otp.application;

import com.genie.auth.profile.application.AccountSummaryService;
import com.genie.common.error.BusinessException;
import com.genie.common.error.ErrorCode;
import com.genie.common.mail.EmailSender;
import com.genie.common.mail.MailTemplate;
import com.genie.common.security.Hashing;
import com.genie.common.security.RateLimiter;
import com.genie.common.security.SecurityProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

/**
 * OTP 6 số dùng chung cho đăng ký, đăng nhập bằng mã, quên mật khẩu, mở khóa và xác thực bước 2 (API_SPEC 0.7, 2.2–2.4;
 * UC-AUTH-01/02/04/06/10; plan AUTH A05).
 * <ul>
 *   <li>Mã sống 5 phút, gửi lại sau 60 giây, tối đa 5 lần / giờ / email ({@code 429 GNE-RAT-4290});</li>
 *   <li>sai → {@code 422 GNE-AUTH-4222} kèm {@code remaining_attempts}; sai lần 5 → challenge bị hủy,
 *       {@link OtpExhaustedException} ({@code 422 GNE-AUTH-4221}); hết hạn / không tồn tại → {@code 410 GNE-AUTH-4101};</li>
 *   <li>chỉ lưu bản băm SHA-256(id : mã : pepper); mã đúng chỉ dùng được một lần (xóa nguyên tử).</li>
 * </ul>
 * Các tham số là hằng số theo tài liệu (không đọc từ application.yml).
 */
@Service
public class OtpChallengeService {

    private static final Logger log = LoggerFactory.getLogger(OtpChallengeService.class);

    public static final Duration TTL = Duration.ofMinutes(5);
    public static final Duration RESEND_AFTER = Duration.ofSeconds(60);
    public static final int MAX_PER_HOUR = 5;
    public static final int MAX_ATTEMPTS = 5;
    private static final int CODE_DIGITS = 6;

    private final OtpStore store;
    private final RateLimiter rateLimiter;
    private final EmailSender emailSender;
    private final Clock clock;
    private final String pepper;
    private final AccountSummaryService accounts;

    /** Khóa trong {@code data} của challenge: tên người nhận khi chưa có tài khoản (đăng ký). */
    public static final String DATA_NAME = "name";

    public OtpChallengeService(OtpStore store, RateLimiter rateLimiter, EmailSender emailSender, Clock clock,
                               SecurityProperties props, AccountSummaryService accounts) {
        this.accounts = accounts;
        this.store = store;
        this.rateLimiter = rateLimiter;
        this.emailSender = emailSender;
        this.clock = clock;
        this.pepper = props.otpPepper() == null ? "" : props.otpPepper();
    }

    /** Gửi mã thật tới {@code email}. {@code data} — dữ liệu kèm luồng (ví dụ email mới). */
    public IssuedOtp issue(OtpPurpose purpose, String email, Long userId, Map<String, String> data) {
        checkRate(email);
        String code = Hashing.randomDigits(CODE_DIGITS);
        OtpChallenge challenge = create(purpose, email, userId, data, code);
        send(challenge, code);
        return view(challenge);
    }

    /**
     * Tạo challenge "giả" có cùng hình dạng nhưng không gửi thư và không bao giờ xác minh được — dùng khi email không
     * tồn tại / không đủ điều kiện để không lộ thông tin tài khoản (UC-AUTH-04 "User Enumeration Defense").
     */
    public IssuedOtp issueSilently(OtpPurpose purpose, String email) {
        checkRate(email);
        OtpChallenge challenge = create(purpose, email, null, Map.of(), null);
        return view(challenge);
    }

    /** Gửi lại mã mới cho challenge còn sống (sau 60 giây; hạn đặt lại 5 phút). */
    public IssuedOtp resend(String challengeId) {
        OtpChallenge old = store.find(challengeId).orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED));
        checkRate(old.email());
        boolean real = !old.codeHash().isEmpty();
        String code = Hashing.randomDigits(CODE_DIGITS);
        OtpChallenge renewed = new OtpChallenge(old.id(), old.purpose(), old.email(), old.userId(),
                real ? hash(old.id(), code) : "", 0, old.resendCount() + 1, clock.instant().plus(TTL), old.data());
        store.save(renewed, TTL);
        store.setActive(renewed.purpose(), renewed.email(), renewed.id(), TTL);
        if (real) {
            send(renewed, code);
        }
        return view(renewed);
    }

    /** Challenge còn sống (không lộ mã). */
    public Optional<OtpChallenge> find(String challengeId) {
        return store.find(challengeId);
    }

    /** Mục đích của challenge còn sống — để controller chọn luồng xác minh. */
    public Optional<OtpPurpose> peekPurpose(String challengeId) {
        return store.find(challengeId).map(OtpChallenge::purpose);
    }

    /** Xác minh mã cho luồng khách vãng lai. */
    public OtpChallenge verify(String challengeId, String code, OtpPurpose expected) {
        return verifyInternal(challengeId, code, expected, null);
    }

    /** Xác minh mã của chính người đang đăng nhập (step-up, đổi email). */
    public OtpChallenge verifyForUser(String challengeId, String code, OtpPurpose expected, long userId) {
        return verifyInternal(challengeId, code, expected, userId);
    }

    private OtpChallenge verifyInternal(String challengeId, String code, OtpPurpose expected, Long userId) {
        OtpChallenge challenge = store.find(challengeId)
                .filter(c -> c.purpose() == expected)
                .filter(c -> userId == null || Objects.equals(c.userId(), userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED));
        boolean wellFormed = code != null && code.length() == CODE_DIGITS && code.chars().allMatch(Character::isDigit);
        if (wellFormed && !challenge.codeHash().isEmpty()
                && Hashing.constantTimeEquals(challenge.codeHash(), hash(challengeId, code))) {
            if (!store.delete(challengeId)) {
                throw new BusinessException(ErrorCode.OTP_EXPIRED);   // request song song đã dùng mã này
            }
            return challenge;
        }
        int attempts = store.incrementAttempts(challengeId);
        if (attempts >= MAX_ATTEMPTS) {
            store.delete(challengeId);
            throw new OtpExhaustedException(challenge);
        }
        throw new BusinessException(ErrorCode.OTP_INVALID, Map.of("remaining_attempts", MAX_ATTEMPTS - attempts));
    }

    /** Hủy challenge (ví dụ hủy đăng ký). */
    public void discard(String challengeId) {
        store.delete(challengeId);
    }

    // ===== nội bộ =====

    private OtpChallenge create(OtpPurpose purpose, String email, Long userId, Map<String, String> data, String code) {
        store.activeId(purpose, email).ifPresent(store::delete);
        String id = "otp_" + Hashing.randomToken(12);
        OtpChallenge challenge = new OtpChallenge(id, purpose, email, userId, code == null ? "" : hash(id, code), 0, 0,
                clock.instant().plus(TTL), data == null ? Map.of() : new HashMap<>(data));
        store.save(challenge, TTL);
        store.setActive(purpose, email, id, TTL);
        return challenge;
    }

    private void checkRate(String email) {
        rateLimiter.check("rl:otp:" + email, 1, RESEND_AFTER);
        rateLimiter.check("rl:otp:hour:" + email, MAX_PER_HOUR, Duration.ofHours(1));
    }

    private void send(OtpChallenge challenge, String code) {
        var locale = LocaleContextHolder.getLocale();
        boolean english = locale != null && "en".equals(locale.getLanguage());
        try {
            emailSender.send(challenge.email(), MailTemplate.OTP,
                    Map.of("otp", code, "expiresMinutes", TTL.toMinutes(), "purpose", challenge.purpose().name(),
                            "action", challenge.purpose().action(english), "name", recipientName(challenge)),
                    locale);
        } catch (MailException e) {
            log.error("Gửi OTP {} tới {} thất bại: {}", challenge.purpose(), mask(challenge.email()), e.getMessage());
            store.delete(challenge.id());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "Chưa gửi được email, vui lòng thử lại.");
        }
    }

    /**
     * Tên chào trong email: tên khai báo khi đăng ký (lưu trong challenge) → tên hồ sơ của tài khoản → phần trước @.
     */
    private String recipientName(OtpChallenge c) {
        String fromData = c.data() == null ? null : c.data().get(DATA_NAME);
        if (fromData != null && !fromData.isBlank()) {
            return fromData.trim();
        }
        if (c.userId() != null) {
            try {
                String fullName = accounts.summaryOf(c.userId()).fullName();
                if (fullName != null && !fullName.isBlank()) {
                    return fullName;
                }
            } catch (RuntimeException e) {
                log.debug("Không lấy được tên người nhận OTP: {}", e.getMessage());
            }
        }
        int at = c.email().indexOf('@');
        return at > 0 ? c.email().substring(0, at) : c.email();
    }

    private String hash(String id, String code) {
        return Hashing.sha256Hex(id + ":" + code + ":" + pepper);
    }

    private IssuedOtp view(OtpChallenge c) {
        long expiresIn = Math.max(0, Duration.between(clock.instant(), c.expiresAt()).toSeconds());
        return new IssuedOtp(c.id(), c.purpose(), mask(c.email()), expiresIn, RESEND_AFTER.toSeconds());
    }

    /** {@code mai.tran@gmail.com} → {@code ma****@gmail.com}. */
    public static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "****";
        }
        String local = email.substring(0, at);
        String keep = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return keep + "****" + email.substring(at);
    }
}
